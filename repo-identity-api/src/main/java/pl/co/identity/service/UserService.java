package pl.co.identity.service;

import pl.co.identity.dto.ProfileResponse;
import pl.co.identity.dto.UpdateProfileRequest;

public interface UserService {
    ProfileResponse getProfile(String userId);
    ProfileResponse updateProfile(String userId, UpdateProfileRequest request);
}
