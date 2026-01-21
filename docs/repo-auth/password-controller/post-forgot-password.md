# POST /api/auth/forgot-password

## Summary
- Request a password reset token by email.

## Auth & Permissions
- PUBLIC

## Request
### Body
```
{
  "email": string
}
```

## Required
| field | location | required |
| --- | --- | --- |
| email | body | x |

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": string
}
```

### Errors
- (400 Bad Request) - errorCode: 243 when email is missing.
- (400 Bad Request) - errorCode: BAD_REQUEST when email format is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
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
1. Validate email and create reset token.
2. Send reset email.

## Notes
- Token TTL is 15 minutes.



