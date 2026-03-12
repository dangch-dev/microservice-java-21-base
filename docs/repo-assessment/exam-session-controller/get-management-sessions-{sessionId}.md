# GET /api/assessment/management/sessions/{sessionId}

## Summary
- Get session details.

## Auth & Permissions
- ADMIN

## Request
### Headers
- Authorization: string (Bearer token)

## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |
| sessionId | path | x |

## Response
### Success
```
{
  "success": true,
  "errorCode": null,
  "errorMessage": null,
  "data": {
    "id": "string",
    "examId": "string",
    "examName": "string | null",
    "examDescription": "string | null",
    "title": "string",
    "startAt": "2026-03-12T00:00:00Z",
    "endAt": "2026-03-12T23:59:59Z",
    "targetType": "USER|CLASS|GUEST",
    "code": "string | null",
    "accessCode": "string | null",
    "assignments": [
      {
        "id": "string",
        "sessionId": "string",
        "userId": "string | null",
        "code": "string",
        "accessCode": "string | null",
        "attemptId": "string | null",
        "attemptStatus": "string | null",
        "gradingStatus": "string | null",
        "score": "number | null",
        "maxScore": "number | null",
        "percent": "number | null",
        "userFullName": "string | null",
        "userEmail": "string | null",
        "userPhoneNumber": "string | null"
      }
    ]
  }
}
```

## Notes
- Assignment user info is resolved via internal identity lookup; fields can be null if userId is null or not found.

### Errors
- (404 Not Found) - errorCode: 227 when session not found.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.
