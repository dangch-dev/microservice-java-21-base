# GET /api/assessment/management/attempts


## Summary
- List attempts for admin management with filters.


## Description
1. Apply optional filters (status, gradingStatus, examId, userId, startTime range).
2. Join exam version to return name/description/duration.
3. Return paged list.

## Auth & Permissions
- ADMIN


## Request
### Query Params
- status: string | null (IN_PROGRESS|TIMEOUT|SUBMITTED)
- gradingStatus: string | null (AUTO_GRADING|MANUAL_GRADING|GRADED)
- examId: string | null
- userId: string | null (createdBy)
- from: string | null (ISO-8601, startTime >= from)
- to: string | null (ISO-8601, startTime <= to)
- page: integer (optional, default 0)
- size: integer (optional, default 20)

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
        "attemptId": string,
        "examId": string,
        "examVersionId": string,
        "createdBy": string,
        "name": string,
        "description": string | null,
        "durationMinutes": integer | null,
        "status": string,
        "gradingStatus": string | null,
        "startTime": string (ISO-8601),
        "endTime": string (ISO-8601) | null,
        "score": number | null,
        "maxScore": number | null,
        "percent": number | null
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
- (403 Forbidden) - errorCode: 230 when user is not ADMIN.
- (400 Bad Request) - errorCode: 221 when `from/to` is invalid ISO-8601.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- `createdBy` is returned for management filtering/auditing.
