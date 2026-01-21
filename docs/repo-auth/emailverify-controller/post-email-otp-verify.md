# POST /api/auth/email/otp/verify

## Summary
- Verify OTP and issue new access and refresh tokens.

## Auth & Permissions
- USER

## Request
### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "otp": string
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
- (400 Bad Request) - errorCode: 243 when otp is missing.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing or invalid.
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

## Logic (Internal)
1. Resolve user from access token.
2. Validate OTP and mark email verified.
3. Issue new tokens.

## Notes
- Max attempts is 5.

