# PUT /api/assessment/management/attempts/{attemptId}/manual-grading


## Summary
- Save manual grading for selected questions (partial save).


## Description
1. Validate attempt exists and is not IN_PROGRESS.
2. Validate manual grading lock (owner + session).
3. Validate payload items.
4. Update earnedPoints + graderComment + gradingStatus for each answer.
5. Return lock info.

## Auth & Permissions
- ADMIN


## Request
### Path Params
- attemptId: string (required)

### Headers
- Authorization: string (Bearer token)
- X-Session-Id: string (required, manual grading session id)

### Body
```
{
  "items": [
    {
      "examVersionQuestionId": "string",
      "earnedPoints": number,
      "graderComment": "string | null"
    }
  ]
}
```


## Required
| field | location | required |
| --- | --- | --- |
| attemptId | path | x |
| Authorization | header | x |
| X-Session-Id | header | x |
| items | body | x |
| items[].examVersionQuestionId | body | x |
| items[].earnedPoints | body | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "ownerId": string,
    "ownerFullName": string | null,
    "ownerAvatarUrl": string | null,
    "ownerEmail": string | null,
    "ownerRoleName": string | null,
    "sessionId": string,
    "ttlSeconds": number
  }
}
```

### Errors
- (404 Not Found) - errorCode: 227 when attempt not found.
- (422 Unprocessable Entity) - errorCode: 420 when attempt is still IN_PROGRESS.
- (409 Conflict) - errorCode: 423 when attempt is locked by another admin (returns lock info).
- (409 Conflict) - errorCode: 424 when same admin opens another session/tab (returns lock info).
- (409 Conflict) - errorCode: 425 when lock is lost (returns lock info).
- (400 Bad Request) - errorCode: 221 when payload invalid or missing header.
- (401 Unauthorized) - errorCode: 241 when access token is missing/invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: 230 when user is not ADMIN.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null | { "ownerId": string, "ownerFullName": string | null, "ownerAvatarUrl": string | null, "ownerEmail": string | null, "ownerRoleName": string | null, "sessionId": string, "ttlSeconds": number }
}
```


## Notes
- Partial save is supported; only items in the request are updated.
- Grading status is set to FINALIZED after save.
