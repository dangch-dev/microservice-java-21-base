# POST /api/auth/refresh


## Summary
- Rotate refresh token and set new cookies.


## Description
1. Validate refresh token signature and claims.
2. Validate refresh token against storage.
3. Verify user status and email verification.
4. Issue new access token and rotate refresh token.

## Auth & Permissions
- PUBLIC


## Request
### Body (optional for non-browser clients)
```
{
  "refreshToken": string
}
```


## Required
| field | location | required |
| --- | --- | --- |
| refreshToken | body |  |


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
- (400 Bad Request) - errorCode: 243 when refresh token is missing (no cookie and no body).
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when refresh token is invalid.
- (401 Unauthorized) - errorCode: 234 when refresh token is expired.
- (401 Unauthorized) - errorCode: 248 when refresh token is revoked or does not match.
- (404 Not Found) - errorCode: 227 when user not found.
- (403 Forbidden) - errorCode: 249 when user is blocked.
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
- Refresh token rotation revokes the previous token.
