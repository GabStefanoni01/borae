package com.borae.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

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
        return restaurantService.create(request, currentUser(authentication));
    }

    @GetMapping("/{restaurantId}")
    @Operation(
            summary = "Consultar perfil do restaurante",
            description = "Retorna o perfil para usuários OWNER ou MANAGER vinculados ao restaurante."
    )
    public RestaurantProfileResponse getProfile(
            @PathVariable UUID restaurantId,
            Authentication authentication
    ) {
        return restaurantService.getProfile(restaurantId, currentUser(authentication));
    }

    @PatchMapping("/{restaurantId}")
    @Operation(summary = "Atualizar perfil do restaurante", description = "OWNER e MANAGER podem editar o perfil.")
    public RestaurantProfileResponse updateProfile(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody UpdateRestaurantProfileRequest request,
            Authentication authentication
    ) {
        return restaurantService.updateProfile(restaurantId, request, currentUser(authentication));
    }

    @GetMapping("/{restaurantId}/business-hours")
    @Operation(
            summary = "Consultar horários de funcionamento",
            description = "Retorna os dias e intervalos configurados. Dias ausentes são considerados fechados."
    )
    public List<BusinessHoursDayResponse> getBusinessHours(
            @PathVariable UUID restaurantId,
            Authentication authentication
    ) {
        return restaurantService.getBusinessHours(restaurantId, currentUser(authentication));
    }

    @PutMapping("/{restaurantId}/business-hours")
    @Operation(
            summary = "Substituir horários de funcionamento",
            description = "Substitui a grade semanal completa. Dias omitidos ficam fechados; um dia pode ter vários intervalos."
    )
    public List<BusinessHoursDayResponse> replaceBusinessHours(
            @PathVariable UUID restaurantId,
            @Valid @RequestBody ReplaceBusinessHoursRequest request,
            Authentication authentication
    ) {
        return restaurantService.replaceBusinessHours(restaurantId, request, currentUser(authentication));
    }

    private UserAccount currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Sessão inválida ou usuário inativo."
                ));
    }
}
