# POST /api/auth/signup

## Summary
- Register a new user and return access and refresh tokens.

## Auth & Permissions
- PUBLIC

## Request
### Body
```
{
  "email": string,
  "password": string,
  "fullName": string,
  "phoneNumber": string | null,
  "avatarUrl": string | null,
  "address": string | null
}
```

## Required
| field | location | required |
| --- | --- | --- |
| email | body | x |
| fullName | body | x |
| password | body | x |

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "accessToken": string,
    "refreshToken": string,
    "acssessExpireIn": integer,
    "refreshExpireIn": integer
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 243 when required fields are missing (email, password, ullName).
- (400 Bad Request) - errorCode: BAD_REQUEST when email format or field length is invalid.
- (400 Bad Request) - errorCode: 202 when request body has invalid data type or JSON.
- (409 Conflict) - errorCode: 255 when email already exists.
- (400 Bad Request) - errorCode: 221 when default role is not found.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```

## Logic (Internal)
1. Validate request payload.
2. Check duplicate email and resolve default role.
3. Create user with ROLE_USER and issue tokens.

## Notes
- Email verification is issued separately.



