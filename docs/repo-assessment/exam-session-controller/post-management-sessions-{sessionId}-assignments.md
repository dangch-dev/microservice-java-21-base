# POST /api/assessment/management/sessions/{sessionId}/assignments

## Summary
- Add assignments for USER/CLASS sessions.

## Auth & Permissions
- ADMIN

## Request
### Headers
- Authorization: string (Bearer token)

### Body (example)
```
{
  "userIds": ["userId1", "userId2"]
}
```

## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |
| sessionId | path | x |
| userIds | body | x |

## Response
### Success
```
{
  "success": true,
  "errorCode": null,
  "errorMessage": null,
  "data": [
    {
      "id": "string",
      "sessionId": "string",
      "userId": "string",
      "code": "string",
      "accessCode": "string | null",
      "attemptId": null
    }
  ]
}
```

### Errors
- (400 Bad Request) - errorCode: 221 when `userIds` is missing/invalid.
- (400 Bad Request) - errorCode: 220 when duplicate userId in session.
- (400 Bad Request) - errorCode: 221 when session is GUEST.
- (404 Not Found) - errorCode: 227 when session not found.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.
