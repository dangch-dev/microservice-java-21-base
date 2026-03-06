# POST /api/auth/google/disconnect


## Summary
- Remove the stored Google connection for the current user.


## Description
1. Read current userId from JWT (access_token cookie or Authorization header).
2. Delete stored Google token by userId.

## Auth & Permissions
- AUTHENTICATED (requires valid system access token)


## Request
### Body
None


## Response
### Success
```
{
  "success": true,
  "errorCode": null,
  "errorMessage": null,
  "data": null
}
```

### Errors
- (401 Unauthorized) - missing/invalid system access token.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```
