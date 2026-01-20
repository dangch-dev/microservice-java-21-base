# GET /tickets/{ticketId}

## Summary
- Get ticket details by id for the current user.

## Auth & Permissions
- USER

## Request
### Path Params
- ticketId: string (required)

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
- (404 Not Found) - errorCode: 227 when ticket not found.
- (403 Forbidden) - errorCode: 230 when user has no authority to view the ticket.
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
1. Load ticket by id.
2. Validate ownership and return ticket details.
