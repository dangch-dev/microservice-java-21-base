# POST /api/assessment/management/attempts/{attemptId}/manual-grading/heartbeat


## Summary
- Renew manual grading lock (keep session alive).


## Description
1. Validate admin permission.
2. Renew lock if `attemptId + adminId + sessionId` match current lock owner.
3. Return updated TTL and server time.

## Auth & Permissions
- ADMIN


## Request
### Path Params
- attemptId: string (required)

### Headers
- Authorization: string (Bearer token)
- X-Session-Id: string (required, manual grading session id)


## Required
| field | location | required |
| --- | --- | --- |
| attemptId | path | x |
| Authorization | header | x |
| X-Session-Id | header | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "ownerId": string,
    "sessionId": string,
    "ttlSeconds": number
  }
}
```

### Errors
- (409 Conflict) - errorCode: 425 when lock is lost or owned by another admin (returns lock info if available).
- (400 Bad Request) - errorCode: 221 when `X-Session-Id` is missing/invalid.
- (401 Unauthorized) - errorCode: 241 when access token is missing/invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: 230 when user is not ADMIN.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- Client should call heartbeat periodically (e.g. every 45s) to keep the lock alive.
