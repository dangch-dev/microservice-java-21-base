# POST /api/auth/guest/login-by-code

## Summary
- Issue guest tokens by exam code (for exam entry).

## Description
1. Validate code.
2. Call assessment internal API to verify guest code.
3. Load guest user and issue access/refresh tokens (cookies).

## Auth & Permissions
- PUBLIC

## Request
### Body
```
{
  "code": "XXXX-YYYY"
}
```

## Required
| field | location | required |
| --- | --- | --- |
| code | body | x |

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
- (400 Bad Request) - errorCode: 221 when code is missing/invalid.
- (404 Not Found) - errorCode: 227 when code or user not found.
- (403 Forbidden) - errorCode: 230 when user is not guest-only.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when internal token is missing/invalid.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```

## Notes
- This endpoint is intended for guest-only exam sessions.
