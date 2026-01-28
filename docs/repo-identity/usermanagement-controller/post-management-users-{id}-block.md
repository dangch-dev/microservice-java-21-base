# POST /api/id/management/users/{id}/block


## Summary
- Block a user.


## Description
1. Update user status to BLOCKED.

## Auth & Permissions
- ADMIN


## Request
### Path Params
- id: string (required)

### Headers
- Authorization: string (Bearer token)


## Required
| field | location | required |
| --- | --- | --- |
| id | path | x |
| Authorization | header | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
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
}
```

### Errors
- (404 Not Found) - errorCode: NOT_FOUND when user not found.
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
