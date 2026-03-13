# POST /api/auth/guest


## Summary
- Create or reuse a guest account and set HttpOnly access/refresh cookies.


## Description
1. Validate request payload.
2. If email already exists:
   - When existing user is guest-only (ROLE_GUEST only), issue tokens.
   - Otherwise return conflict error.
3. Create user with ROLE_GUEST and issue tokens (cookies).

## Auth & Permissions
- PUBLIC


## Request
### Body
```
{
  "fullName": string,
  "email": string,
  "phoneNumber": string
}
```


## Required
| field | location | required |
| --- | --- | --- |
| fullName | body | x |
| email | body | x |
| phoneNumber | body | x |


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
- (400 Bad Request) - errorCode: 243 when required fields are missing (fullName, email, phoneNumber).
- (400 Bad Request) - errorCode: BAD_REQUEST when email format or field length is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (409 Conflict) - errorCode: 255 when email already exists for a non-guest user.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- Guest accounts are created with ROLE_GUEST.
- Guests do not receive email verification.
