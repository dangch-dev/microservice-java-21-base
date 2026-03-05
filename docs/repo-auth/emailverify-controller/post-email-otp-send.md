# POST /api/auth/email/otp/send


## Summary
- Send a verification OTP to the authenticated user email.


## Description
1. Resolve user from access token.
2. Create OTP and send email.

## Auth & Permissions
- USER


## Request
### Headers
- Authorization: string (Bearer token) (optional for non-browser)
  or
- Cookie: access_token=... (browser)


## Required
| field | location | required |
| --- | --- | --- |
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

### Errors
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing or invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (401 Unauthorized) - errorCode: 238 when user not found.
- (409 Conflict) - errorCode: 220 when email is already verified.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- OTP TTL is 10 minutes.
