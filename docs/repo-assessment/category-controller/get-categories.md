# GET /api/assessment/categories

## Summary
- List all categories.

## Auth & Permissions
- ADMIN

## Request
### Headers
- Authorization: string (Bearer token)

## Response
### Success
```
{
  "success": boolean,
  "errorCode": string | null,
  "errorMessage": string | null,
  "data": [
    {
      "id": string,
      "name": string,
      "description": string | null
    }
  ]
}
```

### Errors
- (401 Unauthorized) - errorCode: UNAUTHORIZED when access token is missing/invalid.
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

## Logic (Internal)
1. Validate ADMIN permission.
2. Load categories and return as a list.

## Notes
- Empty list returns `data: []`.


