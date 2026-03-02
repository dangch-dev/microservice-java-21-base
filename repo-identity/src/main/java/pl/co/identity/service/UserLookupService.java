package pl.co.identity.service;

import pl.co.identity.dto.UserLookupResponse;

import java.util.List;

public interface UserLookupService {
    List<UserLookupResponse> lookupByIds(List<String> userIds);
}
