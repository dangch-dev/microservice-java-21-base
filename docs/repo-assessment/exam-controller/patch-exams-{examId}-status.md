# PATCH /api/assessment/exams/{examId}/status

## Summary
- Update exam enable flag (is_enabled).

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
  "enabled": boolean
}
```

## Required
| field | location | required |
| --- | --- | --- |
| examId | path | x |
| enabled | body | x |
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
- (400 Bad Request) - errorCode: 221 when request body is invalid or `enabled` is missing.
- (400 Bad Request) - errorCode: 204 when status is already `true` or `false`.
- (404 Not Found) - errorCode: 227 when exam not found.
- (422 Unprocessable Entity) - errorCode: 420 when enabling without a published version.
- (422 Unprocessable Entity) - errorCode: 420 when published version is not PUBLISHED.
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
2. Validate `enabled` not null.
3. If `enabled=true`, require `published_exam_version_id` and status = PUBLISHED.
4. Update `is_enabled`.







