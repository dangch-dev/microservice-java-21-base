# POST /api/assessment/session/{code}/start

## Summary
- Validate session by code and (optionally) access code for the current user.

## Auth & Permissions
- AUTHENTICATED (MEMBER/GUEST/ADMIN/MANAGER)

## Request
### Path Params
- code: string (required)

### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "accessCode": "string | null"
}
```

## Required
| field | location | required |
| --- | --- | --- |
| code | path | x |
| Authorization | header | x |

## Response
### Success
```
{
  "success": true,
  "errorCode": null,
  "errorMessage": null,
  "data": {
    "attemptId": "string",
    "mode": "NEW|RESUME",
    "examId": "string",
    "examVersionId": "string",
    "status": "IN_PROGRESS|TIMEOUT|SUBMITTED",
    "name": "string",
    "description": "string | null",
    "startTime": "2026-03-12T00:00:00Z",
    "durationMinutes": 60,
    "timeRemainingSeconds": 3600
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 221 when accessCode is missing/invalid or session not active (not started / ended).
- (404 Not Found) - errorCode: 227 when code/assignment/session not found.
- (422 Unprocessable Entity) - errorCode: 420 when session code already used or user has another active attempt.
- (422 Unprocessable Entity) - errorCode: 427 when exam is disabled.
- (422 Unprocessable Entity) - errorCode: 428 when published version does not exist.
- (422 Unprocessable Entity) - errorCode: 431 when published version status is not PUBLISHED.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user has no authority.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```
