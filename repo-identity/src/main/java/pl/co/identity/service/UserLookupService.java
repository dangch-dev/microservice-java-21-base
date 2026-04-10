package pl.co.identity.service;

import pl.co.identity.dto.UserLookupResponse;

import java.util.List;

import pl.co.identity.dto.UserLookupPageResponse;

public interface UserLookupService {
    List<UserLookupResponse> lookupByIds(List<String> userIds);
    UserLookupPageResponse search(String searchValue, List<String> roleNames, Integer page, Integer size);
}
