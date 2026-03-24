# POST /api/assessment/internal/exam-sessions/verify-guest-code

## Summary
- Verify guest-only session code.

## Auth & Permissions
- INTERNAL (ROLE_INTERNAL)

## Request
### Headers
- X-Internal-Token: string (Bearer token)

### Body
```
{
  "code": "XXXX-YYYY"
}
```

## Required
| field | location | required |
| --- | --- | --- |
| X-Internal-Token | header | x |
| code | body | x |

## Response
### Success
```
{
  "success": true,
  "errorCode": null,
  "errorMessage": null,
  "data": {
    "valid": true,
    "sessionId": "string",
    "assignmentId": "string",
    "examId": "string",
    "startAt": "2026-03-12T00:00:00Z",
    "endAt": "2026-03-12T23:59:59Z",
    "userId": "string | null"
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 221 when code is blank or session is not active (not started / ended).
- (404 Not Found) - errorCode: 227 when code or session not found.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when internal token is missing/invalid.
