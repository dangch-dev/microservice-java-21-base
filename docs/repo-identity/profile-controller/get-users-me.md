# GET /api/id/users/me


## Summary
- Get the current user profile.


## Description
1. Resolve userId from access token.
2. Load user and map to profile response.

## Auth & Permissions
- USER


## Request
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
    "id": string,
    "email": string,
    "fullName": string,
    "phoneNumber": string | null,
    "avatarUrl": string | null,
    "address": string | null,
    "roles": [string],
    "status": string (ACTIVE|BLOCKED)
  }
}
```

### Errors
- (404 Not Found) - errorCode: NOT_FOUND when user not found.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing or invalid.
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