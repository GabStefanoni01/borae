package com.borae.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Equipe do restaurante", description = "Convites, membros e permissões por restaurante")
public class RestaurantTeamController {

    private final RestaurantTeamService teamService;
    private final UserAccountRepository userRepository;

    public RestaurantTeamController(RestaurantTeamService teamService, UserAccountRepository userRepository) {
        this.teamService = teamService;
        this.userRepository = userRepository;
    }

    @GetMapping("/restaurants/{restaurantId}/members")
    @Operation(summary = "Listar equipe", description = "OWNER e MANAGER podem consultar os membros ativos.")
    public List<RestaurantMemberResponse> listMembers(
            @PathVariable UUID restaurantId,
            Authentication authentication
    ) {
        return teamService.listMembers(restaurantId, currentUser(authentication));
    }

    @PostMapping("/restaurants/{restaurantId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Convidar membro",
            description = "Somente OWNER pode convidar MANAGER ou STAFF. O token deve ser compartilhado com o e-mail convidado."
    )
    public RestaurantInvitationResponse invite(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody CreateRestaurantInvitationRequest request,
            Authentication authentication
    ) {
        return teamService.invite(restaurantId, request, currentUser(authentication));
    }

    @PostMapping("/restaurant-invitations/accept")
    @Operation(summary = "Aceitar convite", description = "O usuário autenticado precisa utilizar o mesmo e-mail do convite.")
    public RestaurantMemberResponse accept(
            @Valid @RequestBody AcceptRestaurantInvitationRequest request,
            Authentication authentication
    ) {
        return teamService.acceptInvitation(request, currentUser(authentication));
    }

    @PatchMapping("/restaurants/{restaurantId}/members/{membershipId}")
    @Operation(summary = "Alterar papel", description = "Somente OWNER pode alterar MANAGER e STAFF. OWNER não pode ser atribuído.")
    public RestaurantMemberResponse changeRole(
            @PathVariable UUID restaurantId,
            @PathVariable UUID membershipId,
            @Valid @RequestBody ChangeRestaurantMemberRoleRequest request,
            Authentication authentication
    ) {
        return teamService.changeRole(restaurantId, membershipId, request, currentUser(authentication));
    }

    @DeleteMapping("/restaurants/{restaurantId}/members/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover membro", description = "Somente OWNER pode remover membros que não sejam proprietários.")
    public void removeMember(
            @PathVariable UUID restaurantId,
            @PathVariable UUID membershipId,
            Authentication authentication
    ) {
        teamService.removeMember(restaurantId, membershipId, currentUser(authentication));
    }

    private UserAccount currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Sessão inválida ou usuário inativo."
                ));
    }
}
