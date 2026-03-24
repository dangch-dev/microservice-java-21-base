# GET /api/assessment/management/sessions

## Summary
- List exam sessions with optional filters.

## Auth & Permissions
- ADMIN

## Request
### Headers
- Authorization: string (Bearer token)

### Query params
```
examId: string (optional)
startTime: string (optional, ISO-8601)
endTime: string (optional, ISO-8601)
searchValue: string (optional)
page: number (optional, default: 0)
size: number (optional, default: 20)
```

## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |

## Response
### Success
```
{
  "success": true,
  "errorCode": null,
  "errorMessage": null,
  "data": [
    {
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
      "creatorFullName": "string | null",
      "creatorEmail": "string | null",
      "creatorAvatarUrl": "string | null"
    }
  ]
}
```

### Errors
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.
