package com.borae.catalog;

import com.borae.identity.RestaurantMembership;
import com.borae.identity.RestaurantMembershipRepository;
import com.borae.identity.RestaurantRole;
import com.borae.identity.UserAccount;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.UUID;

@Service
public class Product3DGenerationJobService {

    private static final EnumSet<Product3DGenerationJobStatus> ACTIVE_STATUSES =
            EnumSet.of(Product3DGenerationJobStatus.QUEUED, Product3DGenerationJobStatus.PROCESSING);

    private final RestaurantMembershipRepository membershipRepository;
    private final MenuRepository menuRepository;
    private final MenuProductRepository productRepository;
    private final Product3DGenerationJobRepository jobRepository;

    public Product3DGenerationJobService(
            RestaurantMembershipRepository membershipRepository,
            MenuRepository menuRepository,
            MenuProductRepository productRepository,
            Product3DGenerationJobRepository jobRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.menuRepository = menuRepository;
        this.productRepository = productRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Product3DGenerationJobResponse create(
            UUID restaurantId, UUID menuId, UUID productId, UserAccount actor
    ) {
        requireManagerAccess(restaurantId, actor);
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
                    "Produto arquivado não pode receber uma geração 3D.");
        }
        if (product.getImage() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Adicione uma foto ao produto antes de solicitar a geração 3D.");
        }
        if (!restaurantId.equals(product.getImage().getRestaurant().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A imagem do produto não pertence a este restaurante.");
        }
        if (jobRepository.existsByProduct_IdAndStatusIn(productId, ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma geração 3D em andamento para este produto.");
        }

        Product3DGenerationJob job = new Product3DGenerationJob(
                product, product.getImage().getId(), actor
        );
        try {
            return Product3DGenerationJobResponse.from(jobRepository.saveAndFlush(job));
        } catch (DataIntegrityViolationException exception) {
            // The partial unique index is the final guard against concurrent duplicate requests.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma geração 3D em andamento para este produto.");
        }
    }

    @Transactional(readOnly = true)
    public Product3DGenerationJobResponse get(
            UUID restaurantId, UUID menuId, UUID productId, UUID jobId, UserAccount actor
    ) {
        requireManagerAccess(restaurantId, actor);
        ensureProductBelongsToMenu(restaurantId, menuId, productId);
        Product3DGenerationJob job = jobRepository.findByIdAndProduct_Id(jobId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Geração 3D não encontrada."));
        return Product3DGenerationJobResponse.from(job);
    }

    @Transactional
    public Product3DGenerationJobResponse cancel(
            UUID restaurantId, UUID menuId, UUID productId, UUID jobId, UserAccount actor
    ) {
        requireManagerAccess(restaurantId, actor);
        ensureProductBelongsToMenu(restaurantId, menuId, productId);
        Product3DGenerationJob job = jobRepository.findByIdAndProduct_Id(jobId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Geração 3D não encontrada."));
        try {
            job.cancel();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente uma geração ainda na fila pode ser cancelada.");
        }
        return Product3DGenerationJobResponse.from(job);
    }

    private void ensureProductBelongsToMenu(UUID restaurantId, UUID menuId, UUID productId) {
        menuRepository.findByIdAndRestaurant_Id(menuId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cardápio não encontrado."));
        productRepository.findByIdAndCategory_Menu_Id(productId, menuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Produto não encontrado neste cardápio."));
    }

    private void requireManagerAccess(UUID restaurantId, UserAccount actor) {
        RestaurantMembership membership = membershipRepository
                .findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId)
                .filter(item -> item.getRestaurant().isActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você não tem acesso a este restaurante."));
        if (membership.getRole() != RestaurantRole.OWNER && membership.getRole() != RestaurantRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seu papel não permite administrar gerações 3D.");
        }
    }
}
