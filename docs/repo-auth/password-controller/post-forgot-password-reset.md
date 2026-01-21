# POST /api/auth/forgot-password/reset

## Summary
- Reset password with a valid reset token.

## Auth & Permissions
- PUBLIC

## Request
### Body
```
{
  "token": string,
  "newPassword": string
}
```

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
- (400 Bad Request) - errorCode: 243 when 	oken or 
ewPassword is missing.
- (400 Bad Request) - errorCode: BAD_REQUEST when password length is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
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
1. Validate reset token and user.
2. Update user password and revoke reset token.

