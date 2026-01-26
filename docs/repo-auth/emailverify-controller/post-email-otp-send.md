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
  "data": null
}
```

### Errors
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing or invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (401 Unauthorized) - errorCode: 238 when user not found.
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


