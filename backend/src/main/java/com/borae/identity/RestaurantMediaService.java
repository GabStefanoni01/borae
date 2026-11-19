package com.borae.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class RestaurantMediaService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantMediaService.class);
    private static final long LOGO_MAX_BYTES = 5L * 1024 * 1024;
    private static final long PHOTO_MAX_BYTES = 10L * 1024 * 1024;
    private static final long MAX_PHOTOS = 100L;

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMembershipRepository membershipRepository;
    private final RestaurantMediaRepository mediaRepository;
    private final RestaurantObjectStorage storage;
    private final RestaurantImageProcessor imageProcessor;

    public RestaurantMediaService(RestaurantRepository restaurantRepository,
                                  RestaurantMembershipRepository membershipRepository,
                                  RestaurantMediaRepository mediaRepository,
                                  RestaurantObjectStorage storage,
                                  RestaurantImageProcessor imageProcessor) {
        this.restaurantRepository = restaurantRepository;
        this.membershipRepository = membershipRepository;
        this.mediaRepository = mediaRepository;
        this.storage = storage;
        this.imageProcessor = imageProcessor;
    }

    @Transactional(readOnly = true)
    public List<RestaurantMediaResponse> list(UUID restaurantId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        return mediaRepository.findAllByRestaurant_IdAndMediaTypeInOrderByCreatedAtDesc(
                        restaurantId,
                        List.of(RestaurantMediaType.LOGO, RestaurantMediaType.PHOTO)
                )
                .stream().map(RestaurantMediaResponse::from).toList();
    }

    @Transactional
    public RestaurantMediaResponse upload(UUID restaurantId, RestaurantMediaType type,
                                          MultipartFile file, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        if (type == null || file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione o tipo e o arquivo de imagem.");
        }
        if (type == RestaurantMediaType.PRODUCT_IMAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Imagens de produtos devem ser enviadas pelo endpoint específico do produto.");
        }

        long maxBytes = type == RestaurantMediaType.LOGO ? LOGO_MAX_BYTES : PHOTO_MAX_BYTES;
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    type == RestaurantMediaType.LOGO ? "A logo pode ter no máximo 5 MB." : "Cada foto pode ter no máximo 10 MB.");
        }
        if (type == RestaurantMediaType.PHOTO &&
                mediaRepository.countByRestaurant_IdAndMediaType(restaurantId, RestaurantMediaType.PHOTO) >= MAX_PHOTOS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O restaurante já atingiu o limite de 100 fotos.");
        }

        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado.");
        }
        if (original.length > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    type == RestaurantMediaType.LOGO ? "A logo pode ter no máximo 5 MB." : "Cada foto pode ter no máximo 10 MB.");
        }
        RestaurantImageProcessor.ProcessedImage processed =
                imageProcessor.process(original, file.getOriginalFilename(), file.getContentType());

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .filter(Restaurant::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante não encontrado."));

        RestaurantMedia previousLogo = type == RestaurantMediaType.LOGO
                ? mediaRepository.findFirstByRestaurant_IdAndMediaType(restaurantId, RestaurantMediaType.LOGO).orElse(null)
                : null;

        String folder = "/restaurants/" + restaurantId + "/" + type.name().toLowerCase();
        String fileName = UUID.randomUUID() + "." + processed.extension();
        RestaurantObjectStorage.StoredObject uploaded =
                storage.upload(folder, fileName, processed.bytes(), processed.contentType());

        RestaurantMedia saved;
        try {
            if (previousLogo != null) {
                mediaRepository.delete(previousLogo);
                mediaRepository.flush();
            }
            saved = mediaRepository.save(new RestaurantMedia(restaurant, type, uploaded.fileId(), uploaded.publicUrl(),
                    processed.contentType(), processed.bytes().length, processed.width(), processed.height()));
            deleteUploadedAssetIfTransactionRollsBack(uploaded.fileId());
            if (previousLogo != null) {
                deleteFromStorageAfterCommit(previousLogo.getProviderFileId());
            }
        } catch (RuntimeException exception) {
            try {
                storage.delete(uploaded.fileId());
            } catch (RuntimeException cleanupException) {
                log.warn("REMOTE_STORAGE_ORPHAN_CLEANUP_REQUIRED: falha ao remover mídia recém-enviada após erro de persistência. fileId={}",
                        uploaded.fileId(), cleanupException);
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
        return RestaurantMediaResponse.from(saved);
    }

    @Transactional
    public void delete(UUID restaurantId, UUID mediaId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        RestaurantMedia media = mediaRepository.findByIdAndRestaurant_Id(mediaId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada."));
        if (media.getMediaType() == RestaurantMediaType.PRODUCT_IMAGE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Imagens de produtos devem ser removidas pelo endpoint específico do produto.");
        }
        mediaRepository.delete(media);
        deleteFromStorageAfterCommit(media.getProviderFileId());
    }

    private void deleteUploadedAssetIfTransactionRollsBack(String fileId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    deleteRemoteWithRetry(fileId, "rollback");
                }
            }
        });
    }

    private void deleteFromStorageAfterCommit(String fileId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteRemoteWithRetry(fileId, "afterCommit");
                }
            });
        } else {
            deleteRemoteWithRetry(fileId, "direct");
        }
    }

    private void deleteRemoteWithRetry(String fileId, String phase) {
        try {
            storage.delete(fileId);
        } catch (RuntimeException firstException) {
            try {
                storage.delete(fileId);
            } catch (RuntimeException secondException) {
                log.warn("REMOTE_STORAGE_ORPHAN_CLEANUP_REQUIRED: não foi possível remover mídia remota do ImageKit (fase={}). fileId={}",
                        phase, fileId, secondException);
            }
        }
    }

    private void requireManagerAccess(UUID restaurantId, UserAccount actor) {
        RestaurantMembership membership = membershipRepository
                .findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId)
                .filter(item -> item.getRestaurant().isActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você não tem acesso a este restaurante."));
        if (membership.getRole() != RestaurantRole.OWNER && membership.getRole() != RestaurantRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seu papel não permite administrar as imagens do restaurante.");
        }
    }
}
