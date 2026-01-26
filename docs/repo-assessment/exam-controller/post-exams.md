# POST /api/assessment/exams


## Summary
- Create a new exam and its first draft version.


## Description
1. Create `Exam` with categoryId.
2. Create `ExamVersion` (status = DRAFT, shuffle flags = false).
3. Set `draft_exam_version_id` on Exam and return ids.

## Auth & Permissions
- ADMIN


## Request
### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "categoryId": string (required),
  "name": string (required),
  "description": string | null,
  "durationMinutes": integer | null
}
```


## Required
| field | location | required |
| --- | --- | --- |
| name | body | x |
| Authorization | header | x |
| categoryId | body | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "examId": string,
    "examVersionId": string
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 243 when required fields are missing (`categoryId`, `name`).
- (400 Bad Request) - errorCode: 202 when request body has invalid data type/JSON.
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
- Draft metadata is initialized from request name/description/duration.
- `is_enabled` defaults to `false`.



