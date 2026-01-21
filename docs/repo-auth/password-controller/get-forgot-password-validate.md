# GET /api/auth/forgot-password/validate

## Summary
- Validate reset token and return user info for the reset page.

## Auth & Permissions
- PUBLIC

## Request
### Query Params
- token: string (required)

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "email": string,
    "fullName": string,
    "avatarUrl": string | null
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 243 when 	oken is missing.
- (401 Unauthorized) - errorCode: 241 when reset token is invalid or expired.
- (401 Unauthorized) - errorCode: 238 when user does not exist.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```

## Logic (Internal)
1. Validate reset token and return user info.

