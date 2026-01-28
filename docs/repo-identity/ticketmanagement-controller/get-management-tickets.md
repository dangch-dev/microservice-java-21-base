# GET /api/id/management/tickets


## Summary
- List tickets for management view.


## Description
1. Filter tickets by status.
2. Return paged results.

## Auth & Permissions
- ADMIN


## Request
### Query Params
- page: integer (optional, default 0)
- size: integer (optional, default 20)
- status: string | null (OPEN|IN_PROGRESS|COMPLETED|CANCELLED)

### Headers
- Authorization: string (Bearer token)


## Required
| field | location | required |
| --- | --- | --- |
| status | query | x |
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
        "id": string,
        "title": string,
        "description": string,
        "status": string (OPEN|IN_PROGRESS|COMPLETED|CANCELLED),
        "createdBy": string | null,
        "creatorName": string | null,
        "creatorAvatarUrl": string | null,
        "assignedTo": string | null,
        "files": [
          {
            "fileId": string,
            "filename": string,
            "mimeType": string,
            "sizeBytes": integer
          }
        ],
        "createdAt": string,
        "updatedAt": string
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
- (400 Bad Request) - errorCode: 204 when status is invalid.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing or invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.
- (403 Forbidden) - errorCode: 233 when email is not verified.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- page is zero based.



