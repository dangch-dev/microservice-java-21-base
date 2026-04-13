# GET /api/storage/file/{id}/hls/{segment}


## Summary
- Get a single HLS segment for a video file.


## Description
1. Validate segment path.
2. Load file metadata by id.
3. If HLS is not READY, return 202 (processing).
4. Fetch segment object from storage and return as binary.

## Auth & Permissions
- PUBLIC


## Request
### Path Params
- id: string (required)
- segment: string (required) - e.g. `seg_000.ts`


## Response
### Success
- Status: 200 OK
- Content-Type: video/mp2t
- Body: binary segment stream

### Processing
- Status: 202 Accepted (HLS not ready)

### Errors
- (404 Not Found) - errorCode: NOT_FOUND when file metadata not found.
- (404 Not Found) - when segment name is invalid.
- (503 Service Unavailable) - errorCode: 281 when download fails.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```
