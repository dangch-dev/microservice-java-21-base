package pl.co.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pl.co.assessment.dto.UserLookupApiResponse;
import pl.co.assessment.dto.UserLookupRequest;
import pl.co.assessment.dto.UserLookupResponse;
import pl.co.assessment.service.IdentityLookupService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.http.InternalApiClient;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IdentityLookupServiceImpl implements IdentityLookupService {

    private final InternalApiClient internalApiClient;

    @Value("${internal.service.identity-service}")
    private String identityService;

    private final String lookupPath = "/internal/users/lookup";

    @Override
    public Map<String, UserLookupResponse> lookupByIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> ids = userIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        UserLookupRequest request = new UserLookupRequest(ids);
        ResponseEntity<UserLookupApiResponse> response = internalApiClient.send(
                identityService,
                lookupPath,
                HttpMethod.POST,
                MediaType.APPLICATION_JSON,
                null,
                null,
                request,
                UserLookupApiResponse.class,
                false
        );
        UserLookupApiResponse body = response.getBody();
        if (body == null || !body.isSuccess() || body.getData() == null) {
            throw new ApiException(ErrorCode.E305, "Identity lookup failed");
        }
        Map<String, UserLookupResponse> map = new LinkedHashMap<>();
        for (UserLookupResponse item : body.getData()) {
            if (item == null || item.getUserId() == null || item.getUserId().isBlank()) {
                continue;
            }
            map.putIfAbsent(item.getUserId(), item);
        }
        return map;
    }
}
