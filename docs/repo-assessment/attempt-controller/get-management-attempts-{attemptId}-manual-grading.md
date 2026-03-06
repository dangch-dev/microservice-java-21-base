# GET /api/assessment/management/attempts/{attemptId}/manual-grading


## Summary
- Open manual grading entry point for admin (attempt result payload + grading lock info).


## Description
1. Validate attempt exists (deleted = false).
2. Reject if attempt is IN_PROGRESS or gradingStatus is AUTO_GRADING (not gradable).
3. Acquire/refresh manual grading lock by `attemptId + adminId + sessionId`.
4. Load exam version and resolve question order.
5. Load question versions and user answers.
6. Return attempt totals and per-question result payload (answered only) + lock info.

## Auth & Permissions
- ADMIN


## Request
### Path Params
- attemptId: string (required)

### Headers
- Authorization: string (Bearer token)
- X-Session-Id: string (required, manual grading session id)


## Required
| field | location | required |
| --- | --- | --- |
| attemptId | path | x |
| Authorization | header | x |
| X-Session-Id | header | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "attemptId": string,
    "examId": string,
    "examVersionId": string,
    "status": string,
    "gradingStatus": string | null,
    "name": string,
    "description": string | null,
    "durationMinutes": integer | null,
    "startTime": string (ISO-8601),
    "endTime": string (ISO-8601) | null,
    "score": number | null,
    "maxScore": number | null,
    "percent": number | null,
    "lock": {
      "ownerId": string,
      "ownerFullName": string | null,
      "ownerAvatarUrl": string | null,
      "ownerEmail": string | null,
      "ownerRoleName": string | null,
      "sessionId": string,
      "ttlSeconds": number
    },
    "items": [
      {
        "order": integer,
        "examVersionQuestionId": string,
        "questionId": string,
        "questionVersionId": string,
        "type": string,
        "questionContent": object,
        "gradingRules": object,
        "answerJson": object | null,
        "earnedPoints": number | null,
        "answerGradingStatus": string | null
      }
    ],
    "groups": [
      {
        "groupId": string,
        "groupVersionId": string,
        "promptContent": object,
        "questionIds": [string]
      }
    ]
  }
}
```

### Errors
- (404 Not Found) - errorCode: 227 when attempt not found.
- (422 Unprocessable Entity) - errorCode: 420 when attempt is still IN_PROGRESS or AUTO_GRADING.
- (409 Conflict) - errorCode: 423 when attempt is locked by another admin (returns lock info).
- (409 Conflict) - errorCode: 424 when same admin opens another session/tab (returns lock info).
- (400 Bad Request) - errorCode: 221 when `X-Session-Id` is missing/invalid.
- (401 Unauthorized) - errorCode: 241 when access token is missing/invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: 230 when user is not ADMIN.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- Returns `questionContent`, `gradingRules`, and `answerJson` for admin grading.
