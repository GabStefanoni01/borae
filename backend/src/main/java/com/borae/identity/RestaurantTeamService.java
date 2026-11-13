package com.borae.identity;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RestaurantTeamService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMembershipRepository membershipRepository;
    private final RestaurantInvitationRepository invitationRepository;
    private final UserAccountRepository userRepository;

    public RestaurantTeamService(
            RestaurantRepository restaurantRepository,
            RestaurantMembershipRepository membershipRepository,
            RestaurantInvitationRepository invitationRepository,
            UserAccountRepository userRepository
    ) {
        this.restaurantRepository = restaurantRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RestaurantMemberResponse> listMembers(UUID restaurantId, UserAccount actor) {
        requireRole(restaurantId, actor, RestaurantRole.OWNER, RestaurantRole.MANAGER);
        return membershipRepository.findActiveMembersByRestaurantId(restaurantId)
                .stream()
                .map(RestaurantMemberResponse::from)
                .toList();
    }

    @Transactional
    public RestaurantInvitationResponse invite(
            UUID restaurantId,
            CreateRestaurantInvitationRequest request,
            UserAccount actor
    ) {
        requireRole(restaurantId, actor, RestaurantRole.OWNER);
        if (request.role() == RestaurantRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O papel OWNER não pode ser concedido por convite.");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .filter(Restaurant::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante não encontrado."));

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email)
                .filter(UserAccount::isActive)
                .filter(user -> membershipRepository.existsByUser_IdAndRestaurant_IdAndActiveTrue(user.getId(), restaurantId))
                .isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário já faz parte da equipe.");
        }
        if (invitationRepository.existsByRestaurant_IdAndEmailIgnoreCaseAndAcceptedAtIsNullAndExpiresAtAfter(
                restaurantId, email, Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um convite pendente para este e-mail.");
        }

        String token = generateToken();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        invitationRepository.save(new RestaurantInvitation(
                restaurant, email, request.role(), hashToken(token), expiresAt, actor
        ));

        return new RestaurantInvitationResponse(null, email, request.role(), expiresAt, token);
    }

    @Transactional
    public RestaurantMemberResponse acceptInvitation(AcceptRestaurantInvitationRequest request, UserAccount actor) {
        String tokenHash = hashToken(request.token());
        RestaurantInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite inválido ou inexistente."));

        Instant now = Instant.now();
        if (!invitation.isPendingAt(now)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Este convite expirou ou já foi utilizado.");
        }
        if (!invitation.getEmail().equalsIgnoreCase(actor.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este convite foi enviado para outro e-mail.");
        }

        UUID restaurantId = invitation.getRestaurant().getId();
        if (membershipRepository.existsByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já faz parte deste restaurante.");
        }

        RestaurantMembership membership = membershipRepository.save(
                new RestaurantMembership(actor, invitation.getRestaurant(), invitation.getRole())
        );
        invitation.markAccepted(now);
        return RestaurantMemberResponse.from(membership);
    }

    @Transactional
    public RestaurantMemberResponse changeRole(
            UUID restaurantId,
            UUID membershipId,
            ChangeRestaurantMemberRoleRequest request,
            UserAccount actor
    ) {
        requireRole(restaurantId, actor, RestaurantRole.OWNER);
        if (request.role() == RestaurantRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é permitido atribuir OWNER por esta operação.");
        }

        RestaurantMembership membership = findMembership(restaurantId, membershipId);
        if (membership.getRole() == RestaurantRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O papel do proprietário não pode ser alterado.");
        }
        membership.changeRole(request.role());
        return RestaurantMemberResponse.from(membership);
    }

    @Transactional
    public void removeMember(UUID restaurantId, UUID membershipId, UserAccount actor) {
        requireRole(restaurantId, actor, RestaurantRole.OWNER);
        RestaurantMembership membership = findMembership(restaurantId, membershipId);
        if (membership.getRole() == RestaurantRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O proprietário não pode ser removido da própria equipe.");
        }
        membership.deactivate();
    }

    private RestaurantMembership findMembership(UUID restaurantId, UUID membershipId) {
        return membershipRepository.findByIdAndRestaurant_IdAndActiveTrue(membershipId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado."));
    }

    private void requireRole(UUID restaurantId, UserAccount actor, RestaurantRole... allowedRoles) {
        RestaurantMembership access = membershipRepository
                .findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId)
                .filter(membership -> membership.getRestaurant().isActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a este restaurante."));

        for (RestaurantRole role : allowedRoles) {
            if (access.getRole() == role) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel não permite esta operação.");
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível.", exception);
        }
    }
}
