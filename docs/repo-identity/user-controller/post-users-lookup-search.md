# POST /api/id/users/lookup


## Summary
- Search users by fullName/email with pagination.


## Description
1. Normalize searchValue (trim).
2. If searchValue is present, query by fullName/email (contains, case-insensitive).
3. If searchValue is null/blank, return all users with paging.
3. Return paged list with the same columns as lookup.

## Auth & Permissions
- AUTHENTICATED


## Request
### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "searchValue": string | null,
  "page": integer | null (default 0),
  "size": integer | null (default 20)
}
```


## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |
| searchValue | body |  |


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
        "userId": string,
        "fullName": string,
        "avatarUrl": string | null,
        "email": string,
        "roleName": string | null
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
- (400 Bad Request) - errorCode: 243 when `page/size` is invalid.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing/invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
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
- When `searchValue` is null/blank, returns all users with paging.
