# POST /api/auth/email/otp/verify


## Summary
- Verify OTP and set new access/refresh cookies.


## Description
1. Resolve user from access token.
2. Validate OTP and mark email verified.
3. Issue new tokens (cookies).

## Auth & Permissions
- USER


## Request
### Headers
- Authorization: string (Bearer token) (optional for non-browser)
  or
- Cookie: access_token=... (browser)

### Body
```
{
  "otp": string
}
```


## Required
| field | location | required |
| --- | --- | --- |
| otp | body | x |
| Authorization | header |  |


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
- (400 Bad Request) - errorCode: 243 when otp is missing.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (401 Unauthorized) - errorCode: 238 when user not found.
- (401 Unauthorized) - errorCode: 602 when OTP is invalid or attempts exceeded.
- (403 Forbidden) - errorCode: 242 when OTP is expired.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- Max attempts is 5.

