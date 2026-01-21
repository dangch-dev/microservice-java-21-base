# POST /api/storage/file/upload

## Summary
- Upload a file and return its metadata.

## Auth & Permissions
- PUBLIC

## Request
### Form Data
- file: file (required)

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "fileId": string,
    "filename": string,
    "mimeType": string,
    "sizeBytes": integer
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 243 when file is missing or empty.
- (503 Service Unavailable) - errorCode: 281 when upload to storage fails.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```

## Logic (Internal)
1. Validate file input.
2. Upload to object storage and persist metadata.

