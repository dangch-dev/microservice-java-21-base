# POST /api/id/users/lookup


## Summary
- Lookup basic user info by userIds.


## Description
1. Validate request and userIds list.
2. Load users by ids.
3. Return basic fields for each matched user.

## Auth & Permissions
- AUTHENTICATED


## Request
### Headers
- Authorization: string (Bearer token)

### Body
```
{
  "userIds": [string]
}
```


## Required
| field | location | required |
| --- | --- | --- |
| Authorization | header | x |
| userIds | body | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": [
    {
      "userId": string,
      "fullName": string,
      "avatarUrl": string | null,
      "email": string,
      "roleName": string | null
    }
  ]
}
```

### Errors
- (400 Bad Request) - errorCode: 243 when `userIds` is missing/empty.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing/invalid.
- (401 Unauthorized) - errorCode: 234 when access token is expired.
- (403 Forbidden) - errorCode: 233 when email is not verified.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```


## Notes
- Response only includes users found in the system.
- Order of returned users is not guaranteed.
