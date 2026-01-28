# PUT /api/id/users/me


## Summary
- Update current user profile.


## Description
1. Resolve userId from access token.
2. Update profile fields and return updated profile.

## Auth & Permissions
- USER


## Request
### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "fullName": string,
  "phoneNumber": string | null,
  "avatarUrl": string | null,
  "address": string | null
}
```


## Required
| field | location | required |
| --- | --- | --- |
| fullName | body | x |
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
- (400 Bad Request) - errorCode: 243 when ullName is missing.
- (400 Bad Request) - errorCode: BAD_REQUEST when field length is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
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