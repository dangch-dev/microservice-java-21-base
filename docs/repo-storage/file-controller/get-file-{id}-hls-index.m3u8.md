# GET /api/storage/file/{id}/hls/index.m3u8


## Summary
- Get HLS playlist (m3u8) for a video file.


## Description
1. Load file metadata by id.
2. If HLS is not READY, return 202 (processing).
3. Fetch playlist object from storage and return as text.

## Auth & Permissions
- PUBLIC


## Request
### Path Params
- id: string (required)


## Response
### Success
- Status: 200 OK
- Content-Type: application/vnd.apple.mpegurl
- Body: m3u8 text

### Processing
- Status: 202 Accepted (HLS not ready)

### Errors
- (404 Not Found) - errorCode: NOT_FOUND when file metadata not found.
- (404 Not Found) - errorCode: 227 when playlist is missing.
- (503 Service Unavailable) - errorCode: 281 when download fails.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```
