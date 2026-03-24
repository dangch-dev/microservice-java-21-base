# GET /api/assessment/management/attempts/{attemptId}/result


## Summary
- View attempt result for admin (includes content, grading rules, and answers).


## Description
1. Load attempt by id (deleted = false).
2. Reject if attempt is IN_PROGRESS.
3. Load exam version and resolve question order.
4. Load question versions and user answers.
5. Return attempt totals and per-question result payload.

## Auth & Permissions
- ADMIN


## Request
### Path Params
- attemptId: string (required)

### Headers
- Authorization: string (Bearer token)


## Required
| field | location | required |
| --- | --- | --- |
| attemptId | path | x |
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
    "examId": string,
    "examVersionId": string,
    "createdBy": string,
    "creatorFullName": string | null,
    "creatorAvatarUrl": string | null,
    "creatorEmail": string | null,
    "creatorRoleName": string | null,
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
- (422 Unprocessable Entity) - errorCode: 420 when attempt is still IN_PROGRESS.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
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
- Returns `questionContent`, `gradingRules`, and `answerJson` for admin review.
