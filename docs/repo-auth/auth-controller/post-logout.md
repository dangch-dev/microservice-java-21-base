# POST /api/auth/logout

## Summary
- Revoke a refresh token.

## Auth & Permissions
- PUBLIC

## Request
### Body
```
{
  "refreshToken": string
}
```

## Required
| field | location | required |
| --- | --- | --- |
| refreshToken | body | x |

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
- (400 Bad Request) - errorCode: 243 when 
efreshToken is missing.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
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
2. Revoke the provided refresh token if it exists.

## Notes
- Logout is idempotent for unknown tokens.



