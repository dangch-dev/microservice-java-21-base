# PUT /api/assessment/attempts/{attemptId}/answers


## Summary
- Save attempt answers by delta (only changed questions).


## Description
1. Load attempt (deleted = false) and verify owner.
2. Reject if attempt is SUBMITTED or TIMEOUT.
3. Filter invalid examVersionQuestionId and validate answerJson by type + gradingRules.
4. Upsert answers; answerJson = null clears the answer.

## Auth & Permissions
- USER


## Request
### Path Params
- attemptId: string (required)

### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "answers": [
    {
      "examVersionQuestionId": string,
      "answerJson": {
        "schema_version": integer | null,
        "type": string | null,
        "payload": {
          "selected_option_ids": [string] | null,
          "text": string | null,
          "pairs": [
            { "left_id": string, "right_id": string }
          ] | null,
          "blanks": [
            {
              "blank_id": string,
              "kind": string | null,
              "value": string | null,
              "selected_option_ids": [string] | null
            }
          ] | null,
          "files": [
            { "file_id": string, "name": string | null, "mime": string | null, "size": integer | null }
          ] | null
        }
      }
    }
  ]
}
```


## Required
| field | location | required |
| --- | --- | --- |
| attemptId | path | x |
| answers | body |  |
| answers[].examVersionQuestionId | body | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": null
}
```

### Errors
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (404 Not Found) - errorCode: 227 when attempt does not exist.
- (403 Forbidden) - errorCode: 230 when attempt does not belong to current user.
- (409 Conflict) - errorCode: 420 when attempt is submitted or timeout.
- (422 Unprocessable Entity) - errorCode: 221 when answerJson is invalid for question type or grading rules.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- Invalid examVersionQuestionId is ignored.
- answerJson = null clears the stored answer.
- Answers are blocked when attempt status is TIMEOUT.
- `schema_version` and `type` are optional; the server uses questionVersion type to validate.
