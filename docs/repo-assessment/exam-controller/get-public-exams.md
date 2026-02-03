# GET /api/assessment/exams


## Summary
- List published exams that are enabled (public list).


## Description
1. Filter exams by enabled = true.
2. Use published exam version only.
3. Filter by exam name when `searchValue` is provided.
4. Return paged list.

## Auth & Permissions
- USER, ADMIN


## Request
### Query Params
- searchValue: string | null (optional)
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
- (401 Unauthorized) - errorCode: 241 when access token is missing/invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- Only `enabled = true` and `PUBLISHED` exam versions are returned.
