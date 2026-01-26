# POST /api/assessment/attempts/exam/{examId}/start


## Summary
- Start a new attempt or resume an active attempt for the current user.


## Description
1. Validate exam exists, enabled, and has a published version (status = PUBLISHED).
2. If an active attempt exists (IN_PROGRESS | TIMEOUT), return RESUME.
3. Otherwise create a new attempt and persist question/option order if shuffle enabled.

## Auth & Permissions
- USER, ADMIN


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
    "name": string,
    "description": string | null,
    "startTime": string (ISO-8601),
    "durationMinutes": integer | null,
    "timeRemainingSeconds": integer
  }
}
```

### Errors
- (404 Not Found) - errorCode: 227 when exam not found.
- (422 Unprocessable Entity) - errorCode: 420 when exam is disabled.
- (422 Unprocessable Entity) - errorCode: 420 when published version missing or not PUBLISHED.
- (401 Unauthorized) - errorCode: 241 when access token is missing/invalid.
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
