package com.borae.catalog;

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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class Product3DModelService {

    private static final Logger log = LoggerFactory.getLogger(Product3DModelService.class);
    private static final long MAX_MODEL_BYTES = 10L * 1024 * 1024;
    private static final int MAX_DIMENSION_MM = 10_000;
    private static final String GLB_CONTENT_TYPE = "model/gltf-binary";
    private static final Set<String> ALLOWED_DECLARED_TYPES = Set.of(
            "model/gltf-binary",
            "model/gltf+binary",
            "application/octet-stream"
    );

    private final RestaurantMembershipRepository membershipRepository;
    private final MenuRepository menuRepository;
    private final MenuProductRepository productRepository;
    private final Product3DModelRepository modelRepository;
    private final Product3DModelAuditRepository auditRepository;
    private final RestaurantObjectStorage storage;
    private final GlbFileValidator glbValidator;

    public Product3DModelService(RestaurantMembershipRepository membershipRepository,
                                 MenuRepository menuRepository,
                                 MenuProductRepository productRepository,
                                 Product3DModelRepository modelRepository,
                                 Product3DModelAuditRepository auditRepository,
                                 RestaurantObjectStorage storage,
                                 GlbFileValidator glbValidator) {
        this.membershipRepository = membershipRepository;
        this.menuRepository = menuRepository;
        this.productRepository = productRepository;
        this.modelRepository = modelRepository;
        this.auditRepository = auditRepository;
        this.storage = storage;
        this.glbValidator = glbValidator;
    }

    @Transactional(readOnly = true)
    public Product3DModelResponse get(UUID restaurantId, UUID menuId, UUID productId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        findEditableProduct(restaurantId, menuId, productId);
        Product3DModel model = modelRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Este produto ainda não possui modelo 3D."));
        return Product3DModelResponse.from(model);
    }

    @Transactional
    public Product3DModelResponse upload(UUID restaurantId, UUID menuId, UUID productId,
                                        MultipartFile file, int widthMm, int heightMm, int depthMm,
                                        UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        MenuProduct product = findEditableProduct(restaurantId, menuId, productId);
        validateDimensions(widthMm, heightMm, depthMm);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um arquivo GLB.");
        }
        if (file.getSize() > MAX_MODEL_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "O modelo 3D pode ter no máximo 10 MB nesta primeira versão.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null
                || originalName.indexOf('\0') >= 0
                || originalName.contains("..")
                || !originalName.toLowerCase(Locale.ROOT).endsWith(".glb")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie um arquivo com extensão .glb válida.");
        }
        String declaredType = file.getContentType();
        if (declaredType != null && !declaredType.isBlank()) {
            String normalizedType = declaredType.split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (!ALLOWED_DECLARED_TYPES.contains(normalizedType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "MIME type inválido para modelo 3D. Utilize model/gltf-binary ou application/octet-stream.");
            }
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado.");
        }
        if (bytes.length == 0 || bytes.length > MAX_MODEL_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "O modelo 3D pode ter no máximo 10 MB nesta primeira versão.");
        }
        glbValidator.validate(bytes);

        String folder = "/restaurants/" + restaurantId + "/products/" + productId + "/models-3d";
        String fileName = UUID.randomUUID() + ".glb";
        RestaurantObjectStorage.StoredObject uploaded =
                storage.upload(folder, fileName, bytes, GLB_CONTENT_TYPE);

        try {
            Product3DModel model = modelRepository.findByProduct_Id(productId).orElse(null);
            String previousFileId = null;
            String action;
            if (model == null) {
                model = new Product3DModel(product, uploaded.fileId(), uploaded.publicUrl(),
                        GLB_CONTENT_TYPE, bytes.length, widthMm, heightMm, depthMm, actor.getId());
                modelRepository.saveAndFlush(model);
                action = "UPLOADED";
            } else {
                previousFileId = model.replace(uploaded.fileId(), uploaded.publicUrl(), GLB_CONTENT_TYPE,
                        bytes.length, widthMm, heightMm, depthMm, actor.getId());
                modelRepository.flush();
                action = "REPLACED";
            }
            auditRepository.save(new Product3DModelAudit(productId, actor.getId(), action, uploaded.fileId()));
            deleteUploadedAssetIfTransactionRollsBack(uploaded.fileId());
            if (previousFileId != null) {
                deleteFromStorageAfterCommit(previousFileId);
            }
            return Product3DModelResponse.from(model);
        } catch (RuntimeException exception) {
            try {
                storage.delete(uploaded.fileId());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional
    public Product3DModelResponse updatePublication(UUID restaurantId, UUID menuId, UUID productId,
                                                    boolean published, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        findEditableProduct(restaurantId, menuId, productId);
        Product3DModel model = modelRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Este produto ainda não possui modelo 3D."));

        if (published && model.getStatus() != Product3DModelStatus.PUBLISHED) {
            model.publish(actor.getId());
            auditRepository.save(new Product3DModelAudit(productId, actor.getId(),
                    "PUBLISHED", model.getProviderFileId()));
        } else if (!published && model.getStatus() == Product3DModelStatus.PUBLISHED) {
            model.unpublish(actor.getId());
            auditRepository.save(new Product3DModelAudit(productId, actor.getId(),
                    "UNPUBLISHED", model.getProviderFileId()));
        }
        return Product3DModelResponse.from(model);
    }

    @Transactional
    public void delete(UUID restaurantId, UUID menuId, UUID productId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        findEditableProduct(restaurantId, menuId, productId);
        Product3DModel model = modelRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Este produto não possui modelo 3D."));
        String fileId = model.getProviderFileId();
        modelRepository.delete(model);
        auditRepository.save(new Product3DModelAudit(productId, actor.getId(), "DELETED", fileId));
        deleteFromStorageAfterCommit(fileId);
    }

    private MenuProduct findEditableProduct(UUID restaurantId, UUID menuId, UUID productId) {
        Menu menu = menuRepository.findByIdAndRestaurant_Id(menuId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cardápio não encontrado."));
        if (menu.getStatus() == MenuStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cardápio arquivado não pode ser alterado.");
        }
        MenuProduct product = productRepository.findByIdAndCategory_Menu_Id(productId, menuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Produto não encontrado neste cardápio."));
        if (product.getLifecycleStatus() == ProductLifecycleStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Produto arquivado não pode receber alterações no modelo 3D.");
        }
        return product;
    }

    private void validateDimensions(int widthMm, int heightMm, int depthMm) {
        if (widthMm <= 0 || heightMm <= 0 || depthMm <= 0
                || widthMm > MAX_DIMENSION_MM || heightMm > MAX_DIMENSION_MM || depthMm > MAX_DIMENSION_MM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe largura, altura e profundidade em milímetros (entre 1 e 10000 mm).");
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
                    "Seu papel não permite administrar modelos 3D.");
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
                    try {
                        storage.delete(fileId);
                    } catch (RuntimeException exception) {
                        log.warn("Não foi possível remover do ImageKit o novo asset 3D após rollback. fileId={}",
                                fileId, exception);
                    }
                }
            }
        });
    }

    private void deleteFromStorageAfterCommit(String fileId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storage.delete(fileId);
                } catch (RuntimeException exception) {
                    log.warn("Não foi possível remover o asset 3D antigo do ImageKit. fileId={}", fileId, exception);
                }
            }
        });
    }
}
