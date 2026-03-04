# GET /api/auth/google/connect/token


## Summary
- Return Google access token for the current user (if connected).
- If the stored Google access token is expired, the service will try to refresh it automatically.


## Description
1. Read current userId from JWT (access_token cookie or Authorization header).
2. Load stored Google token by userId.
3. If token is expired and refresh_token exists → refresh with Google token endpoint and update storage.
4. Return Google account info + access token (if available).

## Auth & Permissions
- AUTHENTICATED (requires valid system access token)


## Request
### Query
None


## Response
### Success
```
{
  "success": true,
  "errorCode": null,
  "errorMessage": null,
  "data": {
    "accessToken": string | null,
    "expiresAt": "YYYY-MM-DDTHH:mm:ssZ" | null,
    "googleName": string | null,
    "googleAvatarUrl": string | null,
    "googleEmail": string | null
  }
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


## Notes
- If refresh fails or no token is stored, `accessToken`/`expiresAt` will be `null`.
