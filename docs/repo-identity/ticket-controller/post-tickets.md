# POST /api/id/tickets

## Summary
- Create a new support ticket.

## Auth & Permissions
- USER

## Request
### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "title": string,
  "description": string,
  "files": [
    {
      "fileId": string,
      "filename": string,
      "mimeType": string,
      "sizeBytes": integer
    }
  ]
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
- (400 Bad Request) - errorCode: 243 when 	itle or description is missing.
- (400 Bad Request) - errorCode: BAD_REQUEST when field length is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
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
1. Create ticket with status OPEN and creator userId.
2. Publish attached files.

