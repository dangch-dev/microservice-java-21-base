package pl.co.assessment.service;

import pl.co.assessment.dto.UserLookupResponse;

import java.util.Collection;
import java.util.Map;

public interface IdentityLookupService {
    Map<String, UserLookupResponse> lookupByIds(Collection<String> userIds);
}
