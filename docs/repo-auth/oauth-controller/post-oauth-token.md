# POST /api/auth/oauth/token


## Summary
- Issue an internal token for service to service authentication.


## Description
1. Validate client credentials.
2. Issue internal access token.

## Auth & Permissions
- PUBLIC


## Request
### Query Params
- client_id: string (required)
- client_secret: string (required)


## Required
| field | location | required |
| --- | --- | --- |
| client_secret | query | x |
| client_id | query | x |


## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": {
    "accessToken": string,
    "acssessExpireIn": integer
  }
}
```

### Errors
- (400 Bad Request) - errorCode: 243 when client_id or client_secret is missing.
- (400 Bad Request) - errorCode: BAD_REQUEST when client credentials are blank.
- (401 Unauthorized) - errorCode: UNAUTHORIZED when client credentials are invalid.
```
{
  "success": false,
  "errorCode": string,
  "errorMessage": string,
  "data": null
}
```