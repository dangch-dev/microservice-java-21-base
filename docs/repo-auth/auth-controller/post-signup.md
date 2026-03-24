# POST /api/auth/signup


## Summary
- Register a new user and set HttpOnly access/refresh cookies.


## Description
1. Validate request payload.
2. If email already exists:
   - When existing user is guest-only (ROLE_GUEST only), upgrade to ROLE_MEMBER and issue tokens.
   - Otherwise return conflict error.
3. Create user with ROLE_MEMBER and issue tokens (cookies).

## Auth & Permissions
- PUBLIC


## Request
### Body
```
{
  "email": string,
  "password": string,
  "fullName": string,
  "phoneNumber": string | null,
  "avatarUrl": string | null,
  "address": string | null
}
```


## Required
| field | location | required |
| --- | --- | --- |
| email | body | x |
| fullName | body | x |
| password | body | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": null
}
```

### Headers
- Set-Cookie: access_token=...; HttpOnly; Path=/; SameSite=...
- Set-Cookie: refresh_token=...; HttpOnly; Path=/; SameSite=...

### Errors
- (400 Bad Request) - errorCode: 243 when required fields are missing (email, password, ullName).
- (400 Bad Request) - errorCode: BAD_REQUEST when email format or field length is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (409 Conflict) - errorCode: 255 when email already exists for a non-guest user.
- (400 Bad Request) - errorCode: 221 when default role is not found.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- Email verification is issued separately.
