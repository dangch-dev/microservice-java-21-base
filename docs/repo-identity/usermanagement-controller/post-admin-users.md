# POST /admin/users

## Summary
- Create a new user by admin.

## Auth & Permissions
- ADMIN

## Request
### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "email": string,
  "password": string,
  "fullName": string,
  "phoneNumber": string | null,
  "avatarUrl": string | null,
  "address": string | null,
  "status": string (ACTIVE|BLOCKED),
  "roleIds": [string] | null
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
- (400 Bad Request) - errorCode: 243 when required fields are missing (email, password, ullName, status).
- (400 Bad Request) - errorCode: BAD_REQUEST when email format or field length is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (409 Conflict) - errorCode: CONFLICT when email already exists.
- (400 Bad Request) - errorCode: 221 when role id is not found.
- (400 Bad Request) - errorCode: 204 when status is invalid.
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
1. Validate request and resolve roles.
2. Create user with specified status and roles.
