# GET /api/assessment/attempts/{attemptId}


## Summary
- Get attempt details for the current user (questions + saved answers).


## Description
1. Load attempt (deleted = false) and verify owner.
2. Load exam version and compute timeRemainingSeconds; if expired, the attempt may be marked TIMEOUT.
3. Resolve question order (shuffle -> attempt order, else default order).
4. Apply option order if shuffleOptions enabled.
5. Load saved answers for resume.

## Auth & Permissions
- USER, ADMIN (owner only)


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
    "status": string,
    "name": string,
    "description": string | null,
    "durationMinutes": integer | null,
    "startTime": string (ISO-8601) | null,
    "timeRemainingSeconds": integer | null,
    "questions": [
      {
        "order": integer,
        "examVersionQuestionId": string,
        "questionVersionId": string,
        "type": string,
        "questionContent": {
          "schema_version": integer,
          "prompt": {
            "content": string,
            "files": [
              {
                "fileId": string,
                "filename": string,
                "mimeType": string,
                "sizeBytes": integer
              }
            ]
          },
          "explanation": {
            "content": string | null,
            "files": [
              {
                "fileId": string,
                "filename": string,
                "mimeType": string,
                "sizeBytes": integer
              }
            ]
          },
          "options": [
            {
              "id": string,
              "content": string,
              "files": [
                {
                  "fileId": string,
                  "filename": string,
                  "mimeType": string,
                  "sizeBytes": integer
                }
              ]
            }
          ],
          "matching": {
            "left_items": [
              {
                "id": string,
                "content": string,
                "files": [
                  {
                    "fileId": string,
                    "filename": string,
                    "mimeType": string,
                    "sizeBytes": integer
                  }
                ]
              }
            ],
            "right_items": [
              {
                "id": string,
                "content": string,
                "files": [
                  {
                    "fileId": string,
                    "filename": string,
                    "mimeType": string,
                    "sizeBytes": integer
                  }
                ]
              }
            ]
          },
          "blanks": {
            "input_kind": string (text|select),
            "word_bank": [
              {
                "id": string,
                "content": string
              }
            ]
          },
          "file_upload": {
            "allowed_mime_types": [string],
            "max_files": integer
          }
        }
      }
    ] | null,
    "answers": [
      {
        "examVersionQuestionId": string,
        "answerJson": object
      }
    ] | null
  }
}
```

### Errors
- (404 Not Found) - errorCode: 227 when attempt not found.
- (403 Forbidden) - errorCode: 230 when attempt does not belong to current user.
- (422 Unprocessable Entity) - errorCode: 420 when exam version does not exist.
- (401 Unauthorized) - errorCode: 241 when access token is missing/invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- `gradingRules` are not returned on this endpoint.
- When stored option order is missing, options keep original order.
- `timeRemainingSeconds` is null when `durationMinutes` or `startTime` is null.
- `questions` and `answers` are returned only when attempt status = IN_PROGRESS.
