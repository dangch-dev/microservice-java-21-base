# GET /api/assessment/attempts


## Summary
- Get attempt list for the current user.


## Description
1. Resolve userId from token.
2. Apply optional filters (status, gradingStatus, startTime range) with pagination.
3. Return list with exam version metadata and attempt scores.

## Auth & Permissions
- USER, ADMIN (owner only)


## Request
### Query Params
- status: string | null (IN_PROGRESS|TIMEOUT|SUBMITTED)
- gradingStatus: string | null (AUTO_GRADING|MANUAL_GRADING|GRADED)
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
- (400 Bad Request) - errorCode: 221 when `from/to` is invalid ISO-8601.
- (401 Unauthorized) - errorCode: 241 when access token is missing/invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: 230 when user has no authority.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- The list only includes attempts created by the current user.
