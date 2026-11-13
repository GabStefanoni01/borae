package com.borae.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Dados do usuário autenticado e seus acessos")
public class UserController {

    private final UserAccountRepository userRepository;
    private final RestaurantMembershipRepository membershipRepository;

    public UserController(
            UserAccountRepository userRepository,
            RestaurantMembershipRepository membershipRepository
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar usuário autenticado", description = "Retorna os dados da conta associada ao JWT enviado.")
    public UserProfileResponse me(Authentication authentication) {
        return UserProfileResponse.from(findActiveUser(authentication));
    }

    @GetMapping("/me/restaurants")
    @Operation(
            summary = "Listar restaurantes vinculados ao usuário",
            description = "Retorna somente restaurantes ativos nos quais o usuário autenticado possui vínculo ativo, incluindo seu papel."
    )
    public List<UserRestaurantAccessResponse> myRestaurants(Authentication authentication) {
        UserAccount user = findActiveUser(authentication);
        return membershipRepository.findActiveMembershipsByUserId(user.getId())
                .stream()
                .map(UserRestaurantAccessResponse::from)
                .toList();
    }

    private UserAccount findActiveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Sessão inválida ou usuário inativo."
                ));
    }
}
