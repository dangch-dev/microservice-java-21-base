# POST /api/assessment/attempts/{attemptId}/submit


## Summary
- Submit an attempt (finalize endTime and mark as SUBMITTED).


## Description
1. Load attempt (deleted = false) and verify owner.
2. Reject if attempt is already SUBMITTED.
3. Clamp endTime: if endTime is null, set to now or startTime + duration (whichever is earlier).
4. If gradingStatus is null, set AUTO_GRADING, then mark status SUBMITTED.
5. TODO: enqueue attemptId for grading.

## Auth & Permissions
- USER


## Request
### Path Params
- attemptId: string (required)

### Headers
- Authorization: string (Bearer token)


## Required
| field | location | required |
| --- | --- | --- |
| attemptId | path | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": null
}
```

### Errors
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (404 Not Found) - errorCode: 227 when attempt does not exist.
- (403 Forbidden) - errorCode: 230 when attempt does not belong to current user.
- (409 Conflict) - errorCode: 420 when attempt is already submitted.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```
