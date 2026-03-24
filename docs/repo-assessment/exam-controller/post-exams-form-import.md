# POST /api/assessment/management/exams/form/import

## Summary
- Import a Google Form payload into a new exam draft.

## Description
1. Read `form.info.title` and `form.info.description` for exam metadata.
2. Create a new exam + draft version.
3. Convert supported Google Form items into questions.
4. Save draft questions in order.

## Auth & Permissions
- ADMIN

## Request
### Headers
- Authorization: string (Bearer token)

### Body (example)
```
{
  "formId": "string",
  "formName": "string",
  "formUrl": "string",
  "form": {
    "formId": "string",
    "info": {
      "title": "string",
      "description": "string"
    },
    "items": []
  }
}
```

## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |
| form | body | x |
| form.info.title | body | x |

## Response
### Success
```
{
  "success": true,
  "errorCode": null,
  "errorMessage": null,
  "data": {
    "examId": "string",
    "draftExamVersionId": "string",
    "importedCount": 0,
    "skippedCount": 0,
    "warnings": []
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 221 when `form` is missing or `form.info.title` is blank.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type/JSON.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
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
- Uses `form.info.title` and `form.info.description` for exam metadata.
- Unsupported item types are skipped and added to `warnings`.
