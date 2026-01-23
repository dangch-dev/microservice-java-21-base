# GET /api/id/admin/users

## Summary
- List users with optional filters and pagination.

## Auth & Permissions
- ADMIN

## Request
### Query Params
- emailContains: string | null (optional)
- role: string | null (optional)
- status: string | null (ACTIVE|BLOCKED)
- page: integer (optional, default 0)
- size: integer (optional, default 20)

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
        "email": string,
        "fullName": string,
        "phoneNumber": string | null,
        "avatarUrl": string | null,
        "address": string | null,
        "roles": [string],
        "status": string (ACTIVE|BLOCKED),
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

## Logic (Internal)
1. Build filter specification.
2. Query page and map to response.

## Notes
- page is zero based.







