# POST /api/assessment/attempts/exam/{examId}/start


## Summary
- Start a new attempt or resume an active attempt for the current user.


## Description
1. Validate exam exists, enabled, and has a published version (status = PUBLISHED).
2. If an active attempt exists (IN_PROGRESS | TIMEOUT), compute remaining time; if expired, mark TIMEOUT and return RESUME with remaining = 0.
3. Otherwise create a new attempt and persist question/option order if shuffle enabled.

## Auth & Permissions
- MEMBER, GUEST, ADMIN


## Request
### Path Params
- examId: string (required)

### Headers
- Authorization: string (Bearer token)


## Required
| field | location | required |
| --- | --- | --- |
| examId | path | x |
| Authorization | header | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "attemptId": string,
    "mode": string (NEW|RESUME),
    "examId": string,
    "examVersionId": string,
    "status": string,
    "name": string,
    "description": string | null,
    "startTime": string (ISO-8601),
    "durationMinutes": integer | null,
    "timeRemainingSeconds": integer | null
  }
}
```

### Errors
- (404 Not Found) - errorCode: 227 when exam not found.
- (422 Unprocessable Entity) - errorCode: 427 when exam is disabled.
- (422 Unprocessable Entity) - errorCode: 428 when published version does not exist.
- (422 Unprocessable Entity) - errorCode: 431 when published version status is not PUBLISHED.
- (422 Unprocessable Entity) - errorCode: 426 when guest attempt limit exceeded.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: 230 when user has no authority.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- When `shuffleQuestions=true`, the server stores question order per attempt.
- When `shuffleOptions=true`, the server stores option order for SINGLE/MULTIPLE choice only.
- `timeRemainingSeconds` is null when `durationMinutes` or `startTime` is null.
