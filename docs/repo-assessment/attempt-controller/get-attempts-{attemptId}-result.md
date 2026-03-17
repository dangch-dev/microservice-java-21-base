# GET /api/assessment/attempts/{attemptId}/result


## Summary
- Get attempt result (scores + content + grading rules + answers).


## Description
1. Load attempt (deleted = false) and verify owner.
2. Reject if attempt is IN_PROGRESS.
3. Load exam version and resolve question order (shuffle -> attempt order, else default).
4. Load question versions and user answers.
5. Return attempt totals and per-question result payload.

## Auth & Permissions
- Public for guest attempts.
- For non-guest attempts: only owner can view.
- If authenticated, you can view your own attempts and any guest attempts.


## Request
### Path Params
- attemptId: string (required)

## Required
| field | location | required |
| --- | --- | --- |
| attemptId | path | x |


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
        },
        "gradingRules": {
          "schema_version": integer,
          "max_points": number,
          "choice": {
            "correct_option_ids": [string]
          },
          "short_text": {
            "accepted": [string],
            "match_method": string (exact|contains)
          },
          "matching": {
            "pairs": [
              {
                "left_id": string,
                "right_id": string
              }
            ],
            "scheme": string (per_pair|all_or_nothing)
          },
          "fill_blanks": {
            "input_kind": string (text|select),
            "blanks": [
              {
                "blank_id": string,
                "accepted": [string],
                "match_method": string (exact|contains),
                "correct_option_ids": [string]
              }
            ],
            "scheme": string (per_pair|all_or_nothing)
          },
          "manual": {
            "auto_mode": boolean,
            "rubric": [
              {
                "id": string,
                "label": string,
                "max_points": number,
                "description": string | null
              }
            ]
          }
        },
        "answerJson": object | null,
        "earnedPoints": number | null,
        "answerGradingStatus": string | null
      }
    ],
    "groups": [
      {
        "groupId": string,
        "groupVersionId": string,
        "promptContent": {
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
          }
        },
        "questionIds": [string]
      }
    ]
  }
}
```

### Errors
- (404 Not Found) - errorCode: 227 when attempt not found.
- (403 Forbidden) - errorCode: 230 when attempt does not belong to current user.
- (422 Unprocessable Entity) - errorCode: 420 when attempt is still IN_PROGRESS.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- This endpoint always returns `questionContent`, `gradingRules`, and `answerJson`.
- If no auth is provided, only attempts owned by GUEST users can be accessed.
