# POST /api/auth/login

## Summary
- Authenticate with email and password and return access and refresh tokens.

## Auth & Permissions
- PUBLIC

## Request
### Body
```
{
  "email": string,
  "password": string
}
```

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "accessToken": string,
    "refreshToken": string,
    "acssessExpireIn": integer,
    "refreshExpireIn": integer
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 243 when required fields are missing (email, password).
- (400 Bad Request) - errorCode: BAD_REQUEST when email format is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (401 Unauthorized) - errorCode: 238 when email does not exist.
- (401 Unauthorized) - errorCode: 239 when password is incorrect.
- (403 Forbidden) - errorCode: FORBIDDEN when user is blocked.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```

## Logic (Internal)
1. Validate request payload.
2. Look up user by email and verify password.
3. Issue access and refresh tokens.

## Notes
- Token expiry values are in seconds.

