# GET /api/assessment/management/exams


## Summary
- List exams with optional filters and pagination.


## Description
1. Normalize filters (blank -> null) and apply paging defaults.
2. Query repository for page result and map to response.

## Auth & Permissions
- ADMIN


## Request
### Query Params
- searchValue: string | null (optional)
- categoryId: string | null (optional)
- page: integer (optional, default 0)
- size: integer (optional, default 20, minimum 1)

### Headers
- Authorization: string (Bearer token)


## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "items": [
      {
        "examId": string,
        "examVersionId": string,
        "categoryName": string,
        "name": string,
        "description": string | null,
        "status": string,
        "durationMinutes": integer | null,
        "shuffleQuestions": boolean,
        "shuffleOptions": boolean,
        "enabled": boolean
      }
    ],
    "totalElements": integer,
    "totalPages": integer,
    "page": integer,
    "size": integer
  }
}
```

### Errors
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
- `page` is zero-based.



