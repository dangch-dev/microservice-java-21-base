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

## Notes
- Uses `form.info.title` and `form.info.description` for exam metadata.
- Unsupported item types are skipped and added to `warnings`.
