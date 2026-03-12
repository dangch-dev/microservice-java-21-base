# POST /api/assessment/management/exams/{examId}/sessions

## Summary
- Create an exam session and assignments.

## Auth & Permissions
- ADMIN

## Request
### Headers
- Authorization: string (Bearer token)

### Body (example: USER/CLASS)
```
{
  "title": "Session A",
  "startAt": "2026-03-12T00:00:00Z",
  "endAt": "2026-03-12T23:59:59Z",
  "targetType": "USER",
  "accessCode": "ABC123",
  "userIds": ["userId1", "userId2"]
}
```

### Body (example: GUEST)
```
{
  "title": "Session Guest",
  "targetType": "GUEST",
  "accessCode": "GUEST01",
  "guestInfo": [
    { "fullName": "Nguyen Van A", "email": "a@example.com", "phoneNumber": "090..." },
    { "fullName": "Tran B", "email": "b@example.com" }
  ]
}
```

## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |
| title | body | x |
| targetType | body | x |
| userIds | body | x (USER/CLASS) |
| guestInfo | body | x (GUEST) |

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
    "title": "string",
    "startAt": "2026-03-12T00:00:00Z",
    "endAt": "2026-03-12T23:59:59Z",
    "targetType": "USER|CLASS|GUEST",
    "code": "string | null",
    "accessCode": "string | null",
    "assignmentCount": 0
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 221 when `title` or required fields are missing/invalid.
- (400 Bad Request) - errorCode: 221 when exam has no published version.
- (400 Bad Request) - errorCode: 221 when guestInfo has duplicate email.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type/JSON.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing.
- (401 Unauthorized) - errorCode: 241 when access token is invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```

## Notes
- `targetType=GUEST`: each assignment gets its own `code`. `code` on session may be null.
- `accessCode` is shared by all assignments in the session.
