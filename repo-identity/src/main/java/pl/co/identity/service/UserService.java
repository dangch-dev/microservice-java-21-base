package pl.co.identity.service;

import pl.co.identity.dto.ProfileResponse;
import pl.co.identity.dto.UpdateProfileResponse;
import pl.co.identity.dto.UpdateProfileRequest;

public interface UserService {
    ProfileResponse getProfile(String userId);
    UpdateProfileResponse updateProfile(String userId, UpdateProfileRequest request);
}
