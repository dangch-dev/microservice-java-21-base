# PATCH /api/id/management/tickets/{ticketId}/status

## Summary
- Update ticket status or assignee.

## Auth & Permissions
- ADMIN

## Request
### Path Params
- ticketId: string (required)

### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "status": string | null (OPEN|IN_PROGRESS|COMPLETED|CANCELLED),
  "assignedTo": string | null
}
```

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "id": string,
    "title": string,
    "description": string,
    "status": string (OPEN|IN_PROGRESS|COMPLETED|CANCELLED),
    "createdBy": string | null,
    "creatorName": string | null,
    "creatorAvatarUrl": string | null,
    "assignedTo": string | null,
    "files": [
      {
        "fileId": string,
        "filename": string,
        "mimeType": string,
        "sizeBytes": integer
      }
    ],
    "createdAt": string,
    "updatedAt": string
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 204 when status is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (404 Not Found) - errorCode: 227 when ticket or assignee not found.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing or invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.
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
1. Load ticket by id.
2. Update status and or assignee.
3. Publish notifications when status or assignee changes.

