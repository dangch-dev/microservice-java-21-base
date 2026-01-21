# GET /api/id/tickets/{ticketId}/comments

## Summary
- List comments for a ticket.

## Auth & Permissions
- USER

## Request
### Path Params
- ticketId: string (required)

### Headers
- Authorization: string (Bearer token)

## Required
| field | location | required |
| --- | --- | --- |
| ticketId | path | x |
| Authorization | header | x |

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": [
    {
      "id": string,
      "ticketId": string,
      "createdBy": string | null,
      "creatorName": string | null,
      "creatorAvatarUrl": string | null,
      "content": string,
      "files": [
        {
          "fileId": string,
          "filename": string,
          "mimeType": string,
          "sizeBytes": integer
        }
      ],
      "createdAt": string
    }
  ]
}
```

### Errors
- (404 Not Found) - errorCode: 227 when ticket not found.
- (403 Forbidden) - errorCode: 230 when user has no authority to view comments.
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
1. Load ticket and validate access.
2. Return comments sorted by created time.






