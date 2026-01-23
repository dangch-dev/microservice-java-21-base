# POST /api/assessment/exams/{examId}/draft/publish

## Summary
- Publish the current draft version and archive the previous published version.

## Auth & Permissions
- ADMIN

## Request
### Path Params
- examId: string (required)

### Headers
- Authorization: string (Bearer token)

### Body
- Same as `/api/assessment/exams/{examId}/draft/save` (ExamDraftSaveRequest).
- If provided, must include at least `metadata` or `questionChanges`.

## Required
| field | location | required |
| --- | --- | --- |
| examId | path | x |
| Authorization | header | x |

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
- (400 Bad Request) - errorCode: 221 when draft does not exist or status is not DRAFT.
- (400 Bad Request) - errorCode: 221 when draft has no active questions.
- (400 Bad Request) - errorCode: 221 when request body is provided but missing metadata and questionChanges.
- (404 Not Found) - errorCode: 227 when exam not found.
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
1. Lock and load exam row.
2. Validate draft pointer and draft status.
3. Ensure draft has at least 1 active question.
4. Archive old published version (if exists).
5. Promote draft to PUBLISHED and update exam pointers.

## Notes
- Draft version becomes the published version; no new exam version is created.
- Questions and question versions are not modified.






