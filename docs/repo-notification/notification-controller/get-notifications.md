# GET /api/notification/notifications

## Summary
- List notifications for the current user.

## Auth & Permissions
- USER

## Request
### Query Params
- page: integer (optional, default 0)
- size: integer (optional, default 20)

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
  "data": {
    "items": [
      {
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
    ],
    "totalElements": integer,
    "totalPages": integer,
    "page": integer,
    "size": integer,
    "unreadCount": integer
  }
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
1. Load notifications by userId with paging.
2. Return items and unread count.

## Notes
- page is zero based.






