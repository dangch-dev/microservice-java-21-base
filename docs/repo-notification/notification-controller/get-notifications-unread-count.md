# GET /api/notification/notifications/unread-count

## Summary
- Get unread notification count for the current user.

## Auth & Permissions
- USER

## Request
### Headers
- Authorization: string (Bearer token)

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": integer
}
```

### Errors
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing or invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: 233 when email is not verified.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```

## Logic (Internal)
1. Count unread notifications by userId.

