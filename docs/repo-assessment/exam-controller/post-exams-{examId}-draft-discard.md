# POST /exams/{examId}/draft/discard

## Summary
- Discard the current draft version of an exam (idempotent).

## Auth & Permissions
- ADMIN

## Request
### Path Params
- examId: string (required)

### Headers
- Authorization: string (Bearer token)

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
- (404 Not Found) - errorCode: 227 when exam not found.
- (422 Unprocessable Entity) - errorCode: 420 when draft exam version does not exist or status is not DRAFT.
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
1. Lock and load exam.
2. If no draft pointer ? return OK.
3. Validate draft exists and status is DRAFT.
4. Soft delete draft and clear `draft_exam_version_id`.

## Notes
- Calling multiple times is safe; if draft already null it returns OK.

