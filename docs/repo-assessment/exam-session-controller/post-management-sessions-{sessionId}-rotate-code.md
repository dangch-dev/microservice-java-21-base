# POST /api/assessment/management/sessions/{sessionId}/rotate-code

## Summary
- Regenerate session code(s).

## Auth & Permissions
- ADMIN

## Request
### Headers
- Authorization: string (Bearer token)

## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |
| sessionId | path | x |

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
- (404 Not Found) - errorCode: 227 when session not found.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.

## Notes
- USER/CLASS: generates a new common `code` and syncs to all assignments.
- GUEST: regenerates a unique `code` for each assignment.
