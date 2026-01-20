# POST /notifications/{id}/seen

## Summary
- Mark a notification as seen.

## Auth & Permissions
- USER

## Request
### Path Params
- id: string (required)

### Headers
- Authorization: string (Bearer token)

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "id": string,
    "action": string,
    "title": string,
    "message": string,
    "resourceId": string | null,
    "payload": object | null,
    "read": boolean,
    "seen": boolean,
    "createdAt": string,
    "readAt": string | null,
    "seenAt": string | null,
    "dedupeKey": string | null
  }
}
```

### Errors
- (500 Internal Server Error) - errorCode: INTERNAL_ERROR when notification not found.
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
1. Find notification by id and userId.
2. Mark it as seen.
