package com.borae.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/restaurants")
@Tag(name = "Restaurantes", description = "Cadastro e gestão inicial de restaurantes")
public class RestaurantController {

    private final UserAccountRepository userRepository;
    private final RestaurantService restaurantService;

    public RestaurantController(
            UserAccountRepository userRepository,
            RestaurantService restaurantService
    ) {
        this.userRepository = userRepository;
        this.restaurantService = restaurantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastrar restaurante",
            description = "Cria um restaurante e vincula automaticamente o usuário autenticado como proprietário."
    )
    public UserRestaurantAccessResponse create(
            @Valid @RequestBody CreateRestaurantRequest request,
            Authentication authentication
    ) {
        UserAccount owner = userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Sessão inválida ou usuário inativo."
                ));

        return restaurantService.create(request, owner);
    }
}
