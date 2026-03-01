# POST /api/auth/signout


## Summary
- Sign out and clear auth cookies.


## Description
1. Resolve refresh token from cookie (or request body for non-browser clients).
2. Revoke the provided refresh token if it exists.
3. Clear auth cookies.

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
- Set-Cookie: access_token=; Max-Age=0
- Set-Cookie: refresh_token=; Max-Age=0

### Errors
- (400 Bad Request) - errorCode: 243 when refresh token is missing (no cookie and no body).
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- Signout is idempotent for unknown tokens.
