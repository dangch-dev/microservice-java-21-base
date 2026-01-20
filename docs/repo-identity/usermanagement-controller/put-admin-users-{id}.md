# PUT /admin/users/{id}

## Summary
- Update user fields by id.

## Auth & Permissions
- ADMIN

## Request
### Path Params
- id: string (required)

### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "status": string | null (ACTIVE|BLOCKED),
  "roleIds": [string] | null,
  "fullName": string | null,
  "phoneNumber": string | null,
  "avatarUrl": string | null,
  "address": string | null
}
```

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "id": string,
    "email": string,
    "fullName": string,
    "phoneNumber": string | null,
    "avatarUrl": string | null,
    "address": string | null,
    "roles": [string],
    "status": string (ACTIVE|BLOCKED)
  }
}
```

### Errors
- (400 Bad Request) - errorCode: BAD_REQUEST when field length is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (400 Bad Request) - errorCode: 221 when role id is not found.
- (400 Bad Request) - errorCode: 204 when status is invalid.
- (404 Not Found) - errorCode: NOT_FOUND when user not found.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing or invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: FORBIDDEN when user is not ADMIN.
- (403 Forbidden) - errorCode: 233 when email is not verified.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```

## Logic (Internal)
1. Load user by id.
2. Update provided fields and return updated user.
