# GET /api/storage/file/{id}


## Summary
- Download a file by id.


## Description
1. Load file metadata by id.
2. Fetch object stream and return as binary response.

## Auth & Permissions
- PUBLIC


## Request
### Path Params
- id: string (required)

### Query Params
- disposition: string (optional, inline|attachment, default inline)


## Required
| field | location | required |
| --- | --- | --- |
| id | path | x |


## Response
### Success
- Content-Type: file mime type
- Content-Disposition: inline or attachment with filename
- Body: binary stream

### Errors
- (404 Not Found) - errorCode: NOT_FOUND when file metadata not found.
- (404 Not Found) - errorCode: 227 when file object is missing in storage.
- (503 Service Unavailable) - errorCode: 281 when download fails.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```