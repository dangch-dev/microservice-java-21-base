# PUT /api/assessment/management/sessions/{sessionId}

## Summary
- Update session info.

## Auth & Permissions
- ADMIN

## Request
### Headers
- Authorization: string (Bearer token)

### Body (example)
```
{
  "title": "Session A Updated",
  "startAt": "2026-03-12T00:00:00Z",
  "endAt": "2026-03-12T23:59:59Z",
  "accessCode": "NEW123"
}
```

## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |
| sessionId | path | x |
| title | body | x |

## Response
### Success
```
{
  "success": true,
  "errorCode": null,
  "errorMessage": null,
  "data": {
    "id": "string",
    "examId": "string",
    "title": "string",
    "startAt": "2026-03-12T00:00:00Z",
    "endAt": "2026-03-12T23:59:59Z",
    "targetType": "USER|CLASS|GUEST",
    "code": "string | null",
    "accessCode": "string | null",
    "assignmentCount": 0
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 221 when `title` is missing/blank.
- (404 Not Found) - errorCode: 227 when session not found.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.

## Notes
- If `accessCode` changes, all assignments in the session are updated to the new value.
- If `accessCode` is `null` or blank, it will be cleared.
