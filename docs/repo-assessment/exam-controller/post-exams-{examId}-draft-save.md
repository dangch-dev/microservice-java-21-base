# POST /api/assessment/exams/{examId}/draft/save

## Summary
- Save draft metadata and question changes (add/edit/delete/reorder-only).

## Auth & Permissions
- ADMIN

## Request
### Path Params
- examId: string (required)

### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "metadata": {
    "name": string,
    "description": string | null,
    "durationMinutes": integer | null,
    "shuffleQuestions": boolean,
    "shuffleOptions": boolean
  },
  "questionChanges": [
    {
      "questionId": string,
      "questionOrder": integer,
      "deleted": boolean,
      "type": string (SINGLE_CHOICE|MULTIPLE_CHOICE|SHORT_TEXT|MATCHING|FILL_BLANKS|ESSAY|FILE_UPLOAD),
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
```

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
- (400 Bad Request) - errorCode: 243 when required fields are missing (e.g., `questionId`, `metadata.name`, `metadata.shuffleQuestions`, `metadata.shuffleOptions`).
- (400 Bad Request) - errorCode: 202 when request body has invalid data type/JSON.
- (404 Not Found) - errorCode: 227 when exam not found.
- (422 Unprocessable Entity) - errorCode: 420 when draft exam version does not exist or status is not DRAFT.
- (409 Conflict) - errorCode: 220 when duplicate `questionId` or duplicate `questionOrder`.
- (400 Bad Request) - errorCode: 221 when request data is invalid (missing `metadata` and `questionChanges`, invalid `questionOrder`, non-continuous order, invalid `questionId`, missing `type/questionContent/gradingRules`).
- (400 Bad Request) - errorCode: 204 when `questionContent`/`gradingRules` validation fails.
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

## Logic (Internal)
1. Validate input: must have `metadata` or `questionChanges`.
2. Load exam + draft (must be DRAFT).
3. Classify changes: delete, reorder-only, edit, add.
4. Validate orders: unique and continuous from 1..N.
5. Apply delete ? update mappings.
6. Apply reorder-only ? update questionOrder only.
7. Apply edit/add ? create new QuestionVersion and update mappings.
8. Update draft metadata if provided.

## Notes
- If `deleted = true`, only `questionId` is required; content/rules are ignored.
- If `questionId` exists and no `type/content/rules` => reorder-only.
- If `questionId` exists and any of `type/content/rules` is present => must provide all 3.
- If `questionId` does not exist => ADD, must provide all `type/content/rules`.
- `questionContent` and `gradingRules` are required for ADD/EDIT; required subfields depend on `type`.

### Type requirements
| type | questionContent required | gradingRules required |
| --- | --- | --- |
| `SINGLE_CHOICE` | `options[]` (each with `id`, `content`) | `choice.correct_option_ids[]` |
| `MULTIPLE_CHOICE` | `options[]` (each with `id`, `content`) | `choice.correct_option_ids[]` |
| `SHORT_TEXT` | none | `short_text.accepted[]`, `short_text.match_method` |
| `MATCHING` | `matching` | `matching.pairs[]` (each with `left_id`, `right_id`), `matching.scheme` |
| `FILL_BLANKS` | `blanks.input_kind`; if `input_kind=select` => `blanks.word_bank[]` (each with `id`, `content`) | `fill_blanks.blanks[]` (each with `blank_id`); if `input_kind=text` => `accepted[]`, `match_method`; if `input_kind=select` => `correct_option_ids[]`; plus `fill_blanks.scheme` |
| `ESSAY` | none | none |
| `FILE_UPLOAD` | `file_upload.max_files` | none |


