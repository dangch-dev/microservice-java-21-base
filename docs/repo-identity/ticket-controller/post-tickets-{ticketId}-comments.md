# POST /api/id/tickets/{ticketId}/comments


## Summary
- Add a comment to a ticket.


## Description
1. Load ticket and validate comment access.
2. Save comment and publish file and notification events.

## Auth & Permissions
- USER


## Request
### Path Params
- ticketId: string (required)

### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "content": string,
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


## Required
| field | location | required |
| --- | --- | --- |
| content | body | x |
| files | body | x |
| ticketId | path | x |
| Authorization | header | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
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
}
```

### Errors
- (400 Bad Request) - errorCode: 243 when content is missing.
- (400 Bad Request) - errorCode: BAD_REQUEST when content length is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (404 Not Found) - errorCode: 227 when ticket not found.
- (403 Forbidden) - errorCode: 230 when user has no authority to comment.
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