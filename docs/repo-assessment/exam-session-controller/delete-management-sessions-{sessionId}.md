# DELETE /api/assessment/management/sessions/{sessionId}

## Summary
- Soft delete a session and its assignments.

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
  "data": null
}
```

### Errors
- (404 Not Found) - errorCode: 227 when session not found.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.
