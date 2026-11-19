package com.borae.catalog;

import com.borae.identity.RestaurantImageProcessor;
import com.borae.identity.RestaurantMedia;
import com.borae.identity.RestaurantMediaRepository;
import com.borae.identity.RestaurantMediaType;
import com.borae.identity.RestaurantMembership;
import com.borae.identity.RestaurantMembershipRepository;
import com.borae.identity.RestaurantObjectStorage;
import com.borae.identity.RestaurantRole;
import com.borae.identity.UserAccount;
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
import java.util.UUID;

@Service
public class MenuProductImageService {

    private static final Logger log = LoggerFactory.getLogger(MenuProductImageService.class);
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final RestaurantMembershipRepository membershipRepository;
    private final MenuRepository menuRepository;
    private final MenuProductRepository productRepository;
    private final RestaurantMediaRepository mediaRepository;
    private final RestaurantObjectStorage storage;
    private final RestaurantImageProcessor imageProcessor;

    public MenuProductImageService(RestaurantMembershipRepository membershipRepository,
                                   MenuRepository menuRepository,
                                   MenuProductRepository productRepository,
                                   RestaurantMediaRepository mediaRepository,
                                   RestaurantObjectStorage storage,
                                   RestaurantImageProcessor imageProcessor) {
        this.membershipRepository = membershipRepository;
        this.menuRepository = menuRepository;
        this.productRepository = productRepository;
        this.mediaRepository = mediaRepository;
        this.storage = storage;
        this.imageProcessor = imageProcessor;
    }

    @Transactional
    public MenuProductResponse upload(UUID restaurantId, UUID menuId, UUID productId,
                                      MultipartFile file, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        Menu menu = menuRepository.findByIdAndRestaurant_Id(menuId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cardápio não encontrado."));
        if (menu.getStatus() == MenuStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cardápio arquivado não pode ser alterado.");
        }
        MenuProduct product = productRepository.findByIdAndCategory_Menu_Id(productId, menuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado neste cardápio."));
        if (product.getLifecycleStatus() == ProductLifecycleStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Produto arquivado não pode receber imagens.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma imagem.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "A imagem do produto pode ter no máximo 10 MB.");
        }

        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler a imagem enviada.");
        }
        RestaurantImageProcessor.ProcessedImage processed =
                imageProcessor.process(original, file.getOriginalFilename(), file.getContentType());

        String folder = "/restaurants/" + restaurantId + "/products/" + productId;
        String fileName = UUID.randomUUID() + "." + processed.extension();
        RestaurantObjectStorage.StoredObject uploaded =
                storage.upload(folder, fileName, processed.bytes(), processed.contentType());

        RestaurantMedia previous = product.getImage();
        try {
            RestaurantMedia media = mediaRepository.save(new RestaurantMedia(menu.getRestaurant(),
                    RestaurantMediaType.PRODUCT_IMAGE, uploaded.fileId(), uploaded.publicUrl(),
                    processed.contentType(), processed.bytes().length, processed.width(), processed.height()));
            product.setImage(media);
            productRepository.flush();
            deleteUploadedAssetIfTransactionRollsBack(uploaded.fileId());
            if (previous != null) {
                mediaRepository.delete(previous);
                deleteFromStorageAfterCommit(previous.getProviderFileId());
            }
            return MenuProductResponse.from(product);
        } catch (RuntimeException exception) {
            try {
                storage.delete(uploaded.fileId());
            } catch (RuntimeException cleanupException) {
                log.warn("REMOTE_STORAGE_ORPHAN_CLEANUP_REQUIRED: falha ao remover imagem de produto após erro de banco. fileId={}",
                        uploaded.fileId(), cleanupException);
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional
    public void delete(UUID restaurantId, UUID menuId, UUID productId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        Menu menu = menuRepository.findByIdAndRestaurant_Id(menuId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cardápio não encontrado."));
        if (menu.getStatus() == MenuStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cardápio arquivado não pode ser alterado.");
        }
        MenuProduct product = productRepository.findByIdAndCategory_Menu_Id(productId, menuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado neste cardápio."));
        RestaurantMedia image = product.getImage();
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Este produto não possui imagem.");
        }
        product.setImage(null);
        productRepository.flush();
        mediaRepository.delete(image);
        deleteFromStorageAfterCommit(image.getProviderFileId());
    }

    private void requireManagerAccess(UUID restaurantId, UserAccount actor) {
        RestaurantMembership membership = membershipRepository
                .findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId)
                .filter(item -> item.getRestaurant().isActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você não tem acesso a este restaurante."));
        if (membership.getRole() != RestaurantRole.OWNER && membership.getRole() != RestaurantRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seu papel não permite administrar imagens de produtos.");
        }
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
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteRemoteWithRetry(fileId, "afterCommit");
            }
        });
    }

    private void deleteRemoteWithRetry(String fileId, String phase) {
        try {
            storage.delete(fileId);
        } catch (RuntimeException firstException) {
            try {
                storage.delete(fileId);
            } catch (RuntimeException secondException) {
                log.warn("REMOTE_STORAGE_ORPHAN_CLEANUP_REQUIRED: não foi possível remover imagem de produto do ImageKit (fase={}). fileId={}",
                        phase, fileId, secondException);
            }
        }
    }
}
