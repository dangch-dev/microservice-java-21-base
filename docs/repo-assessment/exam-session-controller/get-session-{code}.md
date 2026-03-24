# GET /api/assessment/session/{code}

## Summary
- Get session info by code for the current user (no accessCode validation).

## Auth & Permissions
- AUTHENTICATED (MEMBER/GUEST/ADMIN/MANAGER)

## Request
### Path Params
- code: string (required)

### Headers
- Authorization: string (Bearer token)

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
    "examId": "string",
    "examVersionId": "string",
    "name": "string",
    "description": "string | null",
    "durationMinutes": 60,
    "requiredAccessCode": true,
    "sessionAttemptId": "string | null",
    "sessionAttemptStatus": "IN_PROGRESS|TIMEOUT|SUBMITTED | null",
    "startTime": "2026-03-12T00:00:00Z",
    "endTime": "2026-03-12T23:59:59Z",
    "activeAttemptId": "string | null"
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 221 when code is blank or session is not active (not started / ended).
- (404 Not Found) - errorCode: 227 when code/assignment/session not found.
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

## Notes
- `requiredAccessCode=true` means FE should ask for access code before starting the attempt.
- To validate `accessCode`, use `POST /api/assessment/session/{code}/start`.
