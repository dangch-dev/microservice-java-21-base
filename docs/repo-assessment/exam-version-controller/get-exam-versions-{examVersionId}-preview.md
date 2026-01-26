# GET /api/assessment/exam-versions/{examVersionId}/preview


## Summary
- Preview an exam version (draft, published, or archived) by version id.


## Description
1. Load exam version by id (deleted = false).
2. Load questions by `question_order` and join question versions.
3. Return metadata and full question payload.

## Auth & Permissions
- ADMIN


## Request
### Path Params
- examVersionId: string (required)

### Headers
- Authorization: string (Bearer token)


## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |
| examVersionId | path | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "metadata": {
      "name": string,
      "description": string | null,
      "durationMinutes": integer | null,
      "shuffleQuestions": boolean,
      "shuffleOptions": boolean,
      "status": string,
      "enabled": boolean
    },
    "questions": [
      {
        "questionId": string,
        "questionOrder": integer,
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
        }
      }
    ]
  }
}
```

### Errors
- (404 Not Found) - errorCode: 227 when exam version not found.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing/invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- Preview returns grading rules because this endpoint is admin-only.


