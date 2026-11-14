package com.borae.identity;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class RestaurantMediaService {

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
        return mediaRepository.findAllByRestaurant_IdOrderByCreatedAtDesc(restaurantId)
                .stream().map(RestaurantMediaResponse::from).toList();
    }

    @Transactional
    public RestaurantMediaResponse upload(UUID restaurantId, RestaurantMediaType type,
                                          MultipartFile file, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        if (type == null || file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione o tipo e o arquivo de imagem.");
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
            saved = mediaRepository.save(new RestaurantMedia(restaurant, type, uploaded.fileId(), uploaded.publicUrl(),
                    processed.contentType(), processed.bytes().length, processed.width(), processed.height()));
            if (previousLogo != null) {
                mediaRepository.delete(previousLogo);
                storage.delete(previousLogo.getProviderFileId());
            }
        } catch (RuntimeException exception) {
            storage.delete(uploaded.fileId());
            throw exception;
        }
        return RestaurantMediaResponse.from(saved);
    }

    @Transactional
    public void delete(UUID restaurantId, UUID mediaId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        RestaurantMedia media = mediaRepository.findByIdAndRestaurant_Id(mediaId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada."));
        mediaRepository.delete(media);
        storage.delete(media.getProviderFileId());
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
