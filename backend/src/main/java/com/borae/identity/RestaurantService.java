package com.borae.identity;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMembershipRepository membershipRepository;
    private final RestaurantBusinessHoursRepository businessHoursRepository;

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            RestaurantMembershipRepository membershipRepository,
            RestaurantBusinessHoursRepository businessHoursRepository
    ) {
        this.restaurantRepository = restaurantRepository;
        this.membershipRepository = membershipRepository;
        this.businessHoursRepository = businessHoursRepository;
    }

    @Transactional
    public UserRestaurantAccessResponse create(CreateRestaurantRequest request, UserAccount owner) {
        String name = request.name().trim();
        String description = optionalText(request.description());

        Restaurant restaurant = restaurantRepository.save(
                new Restaurant(name, createSlug(name), description)
        );
        RestaurantMembership membership = membershipRepository.save(
                new RestaurantMembership(owner, restaurant, RestaurantRole.OWNER)
        );

        return UserRestaurantAccessResponse.from(membership);
    }

    @Transactional(readOnly = true)
    public RestaurantProfileResponse getProfile(UUID restaurantId, UserAccount actor) {
        requireProfilePermission(restaurantId, actor);
        Restaurant restaurant = findActiveRestaurant(restaurantId);
        return RestaurantProfileResponse.from(restaurant);
    }

    @Transactional
    public RestaurantProfileResponse updateProfile(
            UUID restaurantId,
            UpdateRestaurantProfileRequest request,
            UserAccount actor
    ) {
        requireProfilePermission(restaurantId, actor);
        Restaurant restaurant = findActiveRestaurant(restaurantId);

        String name = request.name() == null ? restaurant.getName() : request.name().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome do restaurante não pode ficar vazio.");
        }

        String slug = request.slug() == null ? restaurant.getSlug() : normalizeSlug(request.slug());
        if (!slug.equals(restaurant.getSlug())
                && restaurantRepository.existsBySlugAndIdNot(slug, restaurantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este endereço público já está em uso.");
        }

        BigDecimal latitude = request.latitude() == null ? restaurant.getLatitude() : request.latitude();
        BigDecimal longitude = request.longitude() == null ? restaurant.getLongitude() : request.longitude();
        if ((latitude == null) != (longitude == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Latitude e longitude devem ser informadas ou removidas em conjunto."
            );
        }

        restaurant.updateProfile(
                name,
                slug,
                request.description() == null ? restaurant.getDescription() : optionalText(request.description()),
                request.phone() == null ? restaurant.getPhone() : optionalText(request.phone()),
                request.whatsapp() == null ? restaurant.getWhatsapp() : optionalText(request.whatsapp()),
                request.instagram() == null ? restaurant.getInstagram() : optionalText(request.instagram()),
                request.website() == null ? restaurant.getWebsite() : optionalText(request.website()),
                request.address() == null ? restaurant.getAddress() : optionalText(request.address()),
                latitude,
                longitude,
                request.category() == null ? restaurant.getCategory() : optionalText(request.category())
        );
        return RestaurantProfileResponse.from(restaurant);
    }

    @Transactional(readOnly = true)
    public List<BusinessHoursDayResponse> getBusinessHours(UUID restaurantId, UserAccount actor) {
        requireProfilePermission(restaurantId, actor);
        findActiveRestaurant(restaurantId);

        Map<DayOfWeek, List<BusinessHoursIntervalResponse>> schedule = new EnumMap<>(DayOfWeek.class);
        for (RestaurantBusinessHours entry :
                businessHoursRepository.findAllByRestaurant_IdOrderByDayOfWeekAscPositionAsc(restaurantId)) {
            schedule.computeIfAbsent(entry.getDayOfWeek(), ignored -> new ArrayList<>())
                    .add(new BusinessHoursIntervalResponse(entry.getOpeningTime(), entry.getClosingTime()));
        }

        return schedule.entrySet().stream()
                .map(entry -> new BusinessHoursDayResponse(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }

    @Transactional
    public List<BusinessHoursDayResponse> replaceBusinessHours(
            UUID restaurantId,
            ReplaceBusinessHoursRequest request,
            UserAccount actor
    ) {
        requireProfilePermission(restaurantId, actor);
        Restaurant restaurant = findActiveRestaurant(restaurantId);
        validateBusinessHours(request);

        businessHoursRepository.deleteAllByRestaurant_Id(restaurantId);
        businessHoursRepository.flush();

        List<RestaurantBusinessHours> entries = new ArrayList<>();
        for (BusinessHoursDayRequest day : request.days()) {
            List<BusinessHoursIntervalRequest> intervals = day.intervals().stream()
                    .sorted((first, second) -> first.opensAt().compareTo(second.opensAt()))
                    .toList();

            for (int position = 0; position < intervals.size(); position++) {
                BusinessHoursIntervalRequest interval = intervals.get(position);
                entries.add(new RestaurantBusinessHours(
                        restaurant,
                        day.dayOfWeek(),
                        position,
                        interval.opensAt(),
                        interval.closesAt()
                ));
            }
        }

        businessHoursRepository.saveAll(entries);
        return getBusinessHoursAfterReplacement(request);
    }

    private List<BusinessHoursDayResponse> getBusinessHoursAfterReplacement(ReplaceBusinessHoursRequest request) {
        return request.days().stream()
                .sorted((first, second) -> first.dayOfWeek().compareTo(second.dayOfWeek()))
                .map(day -> new BusinessHoursDayResponse(
                        day.dayOfWeek(),
                        day.intervals().stream()
                                .sorted((first, second) -> first.opensAt().compareTo(second.opensAt()))
                                .map(interval -> new BusinessHoursIntervalResponse(interval.opensAt(), interval.closesAt()))
                                .toList()
                ))
                .toList();
    }

    private void validateBusinessHours(ReplaceBusinessHoursRequest request) {
        if (request == null || request.days() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a grade semanal de funcionamento.");
        }

        Set<DayOfWeek> configuredDays = new HashSet<>();
        for (BusinessHoursDayRequest day : request.days()) {
            if (day == null || day.dayOfWeek() == null || day.intervals() == null || day.intervals().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada dia informado precisa ter ao menos um intervalo.");
            }
            if (!configuredDays.add(day.dayOfWeek())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não repita o mesmo dia na grade semanal.");
            }

            for (BusinessHoursIntervalRequest interval : day.intervals()) {
                if (interval == null || interval.opensAt() == null || interval.closesAt() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os horários de abertura e fechamento.");
                }
            }

            List<BusinessHoursIntervalRequest> intervals = day.intervals().stream()
                    .sorted((first, second) -> first.opensAt().compareTo(second.opensAt()))
                    .toList();
            LocalTime previousClosingTime = null;

            for (BusinessHoursIntervalRequest interval : intervals) {
                if (!interval.closesAt().isAfter(interval.opensAt())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "O horário de fechamento deve ser posterior ao horário de abertura."
                    );
                }
                if (previousClosingTime != null && interval.opensAt().isBefore(previousClosingTime)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Os intervalos do dia não podem se sobrepor.");
                }
                previousClosingTime = interval.closesAt();
            }
        }
    }

    private String optionalText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private Restaurant findActiveRestaurant(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .filter(Restaurant::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante não encontrado."));
    }

    private void requireProfilePermission(UUID restaurantId, UserAccount actor) {
        RestaurantMembership membership = membershipRepository
                .findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId)
                .filter(item -> item.getRestaurant().isActive())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Você não tem acesso a este restaurante."
                ));

        if (membership.getRole() != RestaurantRole.OWNER && membership.getRole() != RestaurantRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel não permite administrar o perfil.");
        }
    }

    private String normalizeSlug(String value) {
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");

        if (normalized.isBlank() || normalized.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O endereço público informado é inválido.");
        }
        return normalized;
    }

    private String createSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (normalized.isBlank()) normalized = "restaurante";
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int maxBaseLength = 120 - suffix.length() - 1;
        String base = normalized.substring(0, Math.min(normalized.length(), maxBaseLength));
        return base + "-" + suffix;
    }
}
