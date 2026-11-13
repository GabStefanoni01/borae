package com.borae.identity;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String fullName,
        String email
) {
    public static UserProfileResponse from(UserAccount user) {
        return new UserProfileResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
