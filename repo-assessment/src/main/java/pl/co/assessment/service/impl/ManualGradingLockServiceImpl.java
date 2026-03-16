package pl.co.assessment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import pl.co.assessment.dto.AttemptLockResponse;
import pl.co.assessment.service.ManualGradingLockService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ManualGradingLockServiceImpl implements ManualGradingLockService {

    private static final String LOCK_KEY_PREFIX = "mg:attempt:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(60);
    private static final long DEFAULT_TTL_MS = LOCK_TTL.toMillis();

    private static final DefaultRedisScript<List> ACQUIRE_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<List> RENEW_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<List> VALIDATE_SCRIPT = new DefaultRedisScript<>();

    static {
        ACQUIRE_SCRIPT.setResultType(List.class);
        ACQUIRE_SCRIPT.setScriptText("""
                local key = KEYS[1]
                local ownerId = ARGV[1]
                local sessionId = ARGV[2]
                local payload = ARGV[3]
                local ttl = tonumber(ARGV[4])
                local current = redis.call('GET', key)
                if not current then
                  redis.call('SET', key, payload, 'PX', ttl)
                  return {1, ttl, payload}
                end
                local ok, obj = pcall(cjson.decode, current)
                if ok then
                  if obj['ownerId'] == ownerId and obj['sessionId'] == sessionId then
                    redis.call('SET', key, payload, 'PX', ttl)
                    return {2, ttl, payload}
                  end
                  if obj['ownerId'] == ownerId and obj['sessionId'] ~= sessionId then
                    local ttlLeft = redis.call('PTTL', key)
                    return {3, ttlLeft, current}
                  end
                  local ttlLeft = redis.call('PTTL', key)
                  return {4, ttlLeft, current}
                end
                local ttlLeft = redis.call('PTTL', key)
                return {4, ttlLeft, current}
                """);
        RENEW_SCRIPT.setResultType(List.class);
        RENEW_SCRIPT.setScriptText("""
                local key = KEYS[1]
                local ownerId = ARGV[1]
                local sessionId = ARGV[2]
                local payload = ARGV[3]
                local ttl = tonumber(ARGV[4])
                local current = redis.call('GET', key)
                if not current then
                  return {0, -1, ''}
                end
                local ok, obj = pcall(cjson.decode, current)
                if ok then
                  if obj['ownerId'] == ownerId and obj['sessionId'] == sessionId then
                    redis.call('SET', key, payload, 'PX', ttl)
                    return {1, ttl, payload}
                  end
                  if obj['ownerId'] == ownerId and obj['sessionId'] ~= sessionId then
                    local ttlLeft = redis.call('PTTL', key)
                    return {3, ttlLeft, current}
                  end
                  local ttlLeft = redis.call('PTTL', key)
                  return {4, ttlLeft, current}
                end
                local ttlLeft = redis.call('PTTL', key)
                return {4, ttlLeft, current}
                """);
        VALIDATE_SCRIPT.setResultType(List.class);
        VALIDATE_SCRIPT.setScriptText("""
                local key = KEYS[1]
                local ownerId = ARGV[1]
                local sessionId = ARGV[2]
                local current = redis.call('GET', key)
                if not current then
                  return {0, -1, ''}
                end
                local ttlLeft = redis.call('PTTL', key)
                local ok, obj = pcall(cjson.decode, current)
                if ok and obj['ownerId'] == ownerId and obj['sessionId'] == sessionId then
                  return {1, ttlLeft, current}
                end
                if ok and obj['ownerId'] == ownerId and obj['sessionId'] ~= sessionId then
                  return {3, ttlLeft, current}
                end
                return {4, ttlLeft, current}
                """);
    }

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public AttemptLockResponse acquire(String attemptId, String ownerId, String sessionId) {
        try {
            // Compose lock key and JSON payload for Redis.
            String key = LOCK_KEY_PREFIX + attemptId;
            ManualGradingLockPayload payload = new ManualGradingLockPayload(ownerId, sessionId, Instant.now().toEpochMilli());
            String payloadJson = objectMapper.writeValueAsString(payload);
            // Atomically acquire or renew lock via Lua script.
            List<Object> result = redisTemplate.execute(
                    ACQUIRE_SCRIPT,
                    List.of(key),
                    ownerId,
                    sessionId,
                    payloadJson,
                    String.valueOf(DEFAULT_TTL_MS)
            );
            if (result == null || result.isEmpty()) {
                throw new ApiException(ErrorCode.E423, ErrorCode.E423.message("Lock unavailable"));
            }
            // Parse Lua result into status + TTL + current lock payload.
            long rawCode = toLong(result.get(0));
            long ttlMs = result.size() > 1 ? toLong(result.get(1)) : -1L;
            String value = result.size() > 2 ? Objects.toString(result.get(2), null) : null;
            AcquireCode code = AcquireCode.from(rawCode);
            if (code == AcquireCode.ACQUIRED || code == AcquireCode.RENEWED) {
                // Lock is owned by current admin+session.
                return buildResponse(ownerId, sessionId, ttlMs);
            }
            ManualGradingLockPayload current = null;
            if (value != null && !value.isBlank()) {
                try {
                    current = objectMapper.readValue(value, ManualGradingLockPayload.class);
                } catch (JsonProcessingException ignored) {
                    current = null;
                }
            }
            if (code == AcquireCode.SESSION_CONFLICT) {
                // Same admin, different session/tab.
                AttemptLockResponse lock = buildResponseFromPayload(current, ttlMs);
                if (lock == null && value != null && !value.isBlank()) {
                    lock = buildResponse(value, null, ttlMs);
                }
                String holder = lock == null ? ownerId : lock.getOwnerId();
                throw new ApiException(ErrorCode.E424, ErrorCode.E424.message("Session conflict for " + holder), lock);
            }
            // Locked by another admin.
            AttemptLockResponse lock = buildResponseFromPayload(current, ttlMs);
            if (lock == null && value != null && !value.isBlank()) {
                lock = buildResponse(value, null, ttlMs);
            }
            String holder = lock == null ? null : lock.getOwnerId();
            throw new ApiException(ErrorCode.E423, ErrorCode.E423.message("Locked by " + holder), lock);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message("Lock payload serialization failed"));
        }
    }

    @Override
    public AttemptLockResponse renew(String attemptId, String ownerId, String sessionId) {
        try {
            // Build lock key + payload to extend TTL.
            String key = LOCK_KEY_PREFIX + attemptId;
            ManualGradingLockPayload payload = new ManualGradingLockPayload(ownerId, sessionId, Instant.now().toEpochMilli());
            String payloadJson = objectMapper.writeValueAsString(payload);
            // Only renew if current owner+session matches.
            List<Object> result = redisTemplate.execute(
                    RENEW_SCRIPT,
                    List.of(key),
                    ownerId,
                    sessionId,
                    payloadJson,
                    String.valueOf(DEFAULT_TTL_MS)
            );
            if (result.isEmpty()) {
                throw new ApiException(ErrorCode.E425);
            }
            // Parse Lua result into status + TTL + current lock payload.
            long rawCode = toLong(result.get(0));
            long ttlMs = result.size() > 1 ? toLong(result.get(1)) : -1L;
            String value = result.size() > 2 ? Objects.toString(result.get(2), null) : null;
            AcquireCode code = AcquireCode.from(rawCode);
            if (code == AcquireCode.ACQUIRED) {
                // Renew success for current session.
                return buildResponse(ownerId, sessionId, ttlMs);
            }
            ManualGradingLockPayload current = null;
            if (value != null && !value.isBlank()) {
                try {
                    current = objectMapper.readValue(value, ManualGradingLockPayload.class);
                } catch (JsonProcessingException ignored) {
                    current = null;
                }
            }
            if (code == AcquireCode.SESSION_CONFLICT) {
                AttemptLockResponse lock = buildResponseFromPayload(current, ttlMs);
                if (lock == null && value != null && !value.isBlank()) {
                    lock = buildResponse(value, null, ttlMs);
                }
                String holder = lock == null ? ownerId : lock.getOwnerId();
                throw new ApiException(ErrorCode.E424, ErrorCode.E424.message("Session conflict for " + holder), lock);
            }
            // Lock lost or owned by someone else.
            AttemptLockResponse lock = buildResponseFromPayload(current, ttlMs);
            if (lock == null && value != null && !value.isBlank()) {
                lock = buildResponse(value, null, ttlMs);
            }
            throw new ApiException(ErrorCode.E425, ErrorCode.E425.message("Lock lost"), lock);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message("Lock payload serialization failed"));
        }
    }

    @Override
    public AttemptLockResponse validate(String attemptId, String ownerId, String sessionId) {
        String key = LOCK_KEY_PREFIX + attemptId;
        List<Object> result = redisTemplate.execute(
                VALIDATE_SCRIPT,
                List.of(key),
                ownerId,
                sessionId
        );
        if (result == null || result.isEmpty()) {
            throw new ApiException(ErrorCode.E425, ErrorCode.E425.message("Lock lost"));
        }
        long rawCode = toLong(result.get(0));
        long ttlMs = result.size() > 1 ? toLong(result.get(1)) : -1L;
        String value = result.size() > 2 ? Objects.toString(result.get(2), null) : null;
        AcquireCode code = AcquireCode.from(rawCode);
        if (code == AcquireCode.ACQUIRED) {
            return buildResponse(ownerId, sessionId, ttlMs);
        }
        ManualGradingLockPayload current = null;
        if (value != null && !value.isBlank()) {
            try {
                current = objectMapper.readValue(value, ManualGradingLockPayload.class);
            } catch (JsonProcessingException ignored) {
                current = null;
            }
        }
        if (code == AcquireCode.SESSION_CONFLICT) {
            AttemptLockResponse lock = buildResponseFromPayload(current, ttlMs);
            if (lock == null && value != null && !value.isBlank()) {
                lock = buildResponse(value, null, ttlMs);
            }
            String holder = lock == null ? ownerId : lock.getOwnerId();
            throw new ApiException(ErrorCode.E424, ErrorCode.E424.message("Session conflict for " + holder), lock);
        }
        if (code == AcquireCode.UNKNOWN) {
            throw new ApiException(ErrorCode.E425, ErrorCode.E425.message("Lock lost"), (Object) null);
        }
        AttemptLockResponse lock = buildResponseFromPayload(current, ttlMs);
        if (lock == null && value != null && !value.isBlank()) {
            lock = buildResponse(value, null, ttlMs);
        }
        String holder = lock == null ? null : lock.getOwnerId();
        throw new ApiException(ErrorCode.E423, ErrorCode.E423.message("Locked by " + holder), lock);
    }

    private AttemptLockResponse buildResponse(String ownerId, String sessionId, long ttlMs) {
        // Convert TTL from ms to seconds and normalize.
        long ttlSeconds = ttlMs < 0 ? LOCK_TTL.getSeconds() : Math.max(0L, ttlMs / 1000L);
        return AttemptLockResponse.builder()
                .ownerId(ownerId)
                .sessionId(sessionId)
                .ttlSeconds(ttlSeconds)
                .build();
    }

    private long toLong(Object value) {
        // Defensive numeric parsing for Lua return values.
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return -1L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    private AttemptLockResponse buildResponseFromPayload(ManualGradingLockPayload payload, long ttlMs) {
        if (payload == null) {
            return null;
        }
        return buildResponse(payload.getOwnerId(), payload.getSessionId(), ttlMs);
    }

    private static class ManualGradingLockPayload {
        private String ownerId;
        private String sessionId;
        private long acquiredAt;

        public ManualGradingLockPayload() {
        }

        ManualGradingLockPayload(String ownerId, String sessionId, long acquiredAt) {
            this.ownerId = ownerId;
            this.sessionId = sessionId;
            this.acquiredAt = acquiredAt;
        }

        public String getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(String ownerId) {
            this.ownerId = ownerId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public long getAcquiredAt() {
            return acquiredAt;
        }

        public void setAcquiredAt(long acquiredAt) {
            this.acquiredAt = acquiredAt;
        }
    }

    private enum AcquireCode {
        ACQUIRED(1),
        RENEWED(2),
        SESSION_CONFLICT(3),
        LOCKED(4),
        UNKNOWN(0);

        private final long value;

        AcquireCode(long value) {
            this.value = value;
        }

        static AcquireCode from(long value) {
            for (AcquireCode code : values()) {
                if (code.value == value) {
                    return code;
                }
            }
            return UNKNOWN;
        }
    }

}
