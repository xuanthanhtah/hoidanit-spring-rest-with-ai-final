# API Specification

> All endpoints return `ApiResponse<T>` wrapper.
> Update this file whenever endpoints change.

---

## Base URL

```
Development: http://localhost:8080/api/v1
Production:  https://api.example.com/api/v1
```

---

## Authentication

All endpoints require JWT in `Authorization: Bearer <accessToken>` header,
except those marked as  **Public**.

---

## 1. Auth

### POST /auth/login 

Login and receive tokens.

**Request Body:**
```json
{
  "email": "admin@example.com",
  "password": "password123"
}
```

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  },
  "message": "Login successful",
  "timestamp": "20xx-02-28T10:00:00"
}
```

**Also sets cookie:**
```
Set-Cookie: refresh_token=eyJ...;
            HttpOnly; Secure; SameSite=Lax;
            Path=/api/v1/auth; Max-Age=259200
```

**Errors:**
| Status | When |
|--------|------|
| 400 | Missing email or password |
| 401 | Invalid credentials |

---

### POST /auth/register 

Register a new user account.

**Request Body:**
```json
{
  "name": "Nguyen Van A",
  "email": "user@example.com",
  "password": "password123",
  "age": 25,
  "gender": "MALE",
  "address": "Ho Chi Minh City"
}
```

**Success Response (201):**
```json
{
  "statusCode": 201,
  "data": {
    "id": 1,
    "name": "Nguyen Van A",
    "email": "user@example.com",
    "age": 25,
    "gender": "MALE",
    "address": "Ho Chi Minh City",
    "createdAt": "20xx-02-28T10:00:00Z"
  },
  "message": "User registered",
  "timestamp": "20xx-02-28T10:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 400 | Validation failed (blank name, invalid email, password < 8 chars) |
| 409 | Email already exists |

---

### POST /auth/refresh 

Get new access token using refresh token.

**Sources (backend checks in order):**
1. Cookie `refresh_token` (SPA — browser sends automatically)
2. Request body `refreshToken` (Mobile — sends explicitly)

**Request Body (mobile only):**
```json
{
  "refreshToken": "eyJ..."
}
```

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": {
    "accessToken": "eyJ...(new)",
    "refreshToken": "eyJ...(new)"
  },
  "message": "Token refreshed",
  "timestamp": "20xx-02-28T10:15:00"
}
```

Also sets new `refresh_token` cookie (replaces old one).

**Errors:**
| Status | When |
|--------|------|
| 401 | No refresh token provided |
| 401 | Refresh token expired or revoked |

---

### POST /auth/logout

Invalidate refresh token and clear cookie.

**Request:** No body needed. Token taken from cookie or Authorization header.

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": null,
  "message": "Logged out",
  "timestamp": "20xx-02-28T11:00:00"
}
```

**Also clears cookie:**
```
Set-Cookie: refresh_token=; Max-Age=0; Path=/api/v1/auth
```

---

### GET /auth/me

Get current logged-in user info.

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": {
    "id": 1,
    "name": "Nguyen Van A",
    "email": "user@example.com",
    "age": 25,
    "gender": "MALE",
    "address": "Ho Chi Minh City",
    "avatar": null,
    "company": {
      "id": 1,
      "name": "HoiDanIT"
    },
    "roles": [
      { "id": 1, "name": "ADMIN" }
    ]
  },
  "message": "Success",
  "timestamp": "20xx-02-28T10:00:00"
}
```

---

## 2. Users

### GET /users 🔒

List all users with pagination.

**Query Parameters:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| page | int | 1 | Page number (1-based) |
| size | int | 10 | Items per page |
| sort | string | id,asc | Sort field and direction |

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": {
    "meta": {
      "page": 1,
      "pageSize": 10,
      "pages": 5,
      "total": 50
    },
    "result": [
      {
        "id": 1,
        "name": "Nguyen Van A",
        "email": "user@example.com",
        "age": 25,
        "gender": "MALE",
        "address": "Ho Chi Minh City",
        "avatar": null,
        "company": { "id": 1, "name": "HoiDanIT" },
        "roles": [{ "id": 1, "name": "ADMIN" }],
        "createdAt": "20xx-02-28T10:00:00Z",
        "updatedAt": null
      }
    ]
  },
  "message": "Fetch all users",
  "timestamp": "20xx-02-28T10:00:00"
}
```

---

### GET /users/{id} 🔒

Get a single user by ID.

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": {
    "id": 1,
    "name": "Nguyen Van A",
    "email": "user@example.com",
    "age": 25,
    "gender": "MALE",
    "address": "Ho Chi Minh City",
    "avatar": null,
    "company": { "id": 1, "name": "HoiDanIT" },
    "roles": [{ "id": 1, "name": "ADMIN" }],
    "createdAt": "20xx-02-28T10:00:00Z",
    "updatedAt": null
  },
  "message": "Fetch user",
  "timestamp": "20xx-02-28T10:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 404 | User not found |

---

### POST /users 🔒

Create a new user (admin operation — assigns company and roles).

**Request Body:**
```json
{
  "name": "Tran Thi B",
  "email": "tran@example.com",
  "password": "password123",
  "age": 30,
  "gender": "FEMALE",
  "address": "Ha Noi",
  "companyId": 1,
  "roleIds": [3, 5]
}
```

**Success Response (201):**
```json
{
  "statusCode": 201,
  "data": {
    "id": 2,
    "name": "Tran Thi B",
    "email": "tran@example.com",
    "age": 30,
    "gender": "FEMALE",
    "address": "Ha Noi",
    "avatar": null,
    "company": { "id": 1, "name": "HoiDanIT" },
    "roles": [
      { "id": 3, "name": "HR" },
      { "id": 5, "name": "USER" }
    ],
    "createdAt": "20xx-02-28T10:00:00Z"
  },
  "message": "User created",
  "timestamp": "20xx-02-28T10:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 400 | Validation failed |
| 404 | Company or Role not found |
| 409 | Email already exists |

---

### PUT /users 🔒

Update an existing user.

**Request Body:**
```json
{
  "id": 2,
  "name": "Tran Thi B Updated",
  "age": 31,
  "gender": "FEMALE",
  "address": "Da Nang",
  "companyId": 2,
  "roleIds": [3]
}
```

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": {
    "id": 2,
    "name": "Tran Thi B Updated",
    "email": "tran@example.com",
    "age": 31,
    "gender": "FEMALE",
    "address": "Da Nang",
    "avatar": null,
    "company": { "id": 2, "name": "FPT Software" },
    "roles": [{ "id": 3, "name": "HR" }],
    "updatedAt": "20xx-02-28T11:00:00Z"
  },
  "message": "User updated",
  "timestamp": "20xx-02-28T11:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 400 | Validation failed |
| 404 | User, Company, or Role not found |

**Note:** `email` and `password` are NOT updatable through this endpoint.

---

### DELETE /users/{id} 🔒

Delete a user. Also revokes all their refresh tokens.

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": null,
  "message": "User deleted",
  "timestamp": "20xx-02-28T12:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 404 | User not found |

---

## 3. Companies

### GET /companies 🔒

List all companies with pagination.

**Query Parameters:** same as `/users` (page, size, sort)

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": {
    "meta": { "page": 1, "pageSize": 10, "pages": 2, "total": 15 },
    "result": [
      {
        "id": 1,
        "name": "HoiDanIT",
        "description": "Education platform",
        "address": "Ho Chi Minh City",
        "logo": "/logos/hoidanit.png",
        "createdAt": "20xx-01-01T00:00:00Z",
        "updatedAt": null
      }
    ]
  },
  "message": "Fetch all companies",
  "timestamp": "20xx-02-28T10:00:00"
}
```

---

### GET /companies/{id} 🔒

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": {
    "id": 1,
    "name": "HoiDanIT",
    "description": "Education platform",
    "address": "Ho Chi Minh City",
    "logo": "/logos/hoidanit.png",
    "createdAt": "20xx-01-01T00:00:00Z",
    "updatedAt": null
  },
  "message": "Fetch company",
  "timestamp": "20xx-02-28T10:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 404 | Company not found |

---

### POST /companies 🔒

**Request Body:**
```json
{
  "name": "FPT Software",
  "description": "IT outsourcing",
  "address": "Ha Noi",
  "logo": "/logos/fpt.png"
}
```

**Success Response (201):**
```json
{
  "statusCode": 201,
  "data": {
    "id": 2,
    "name": "FPT Software",
    "description": "IT outsourcing",
    "address": "Ha Noi",
    "logo": "/logos/fpt.png",
    "createdAt": "20xx-02-28T10:00:00Z"
  },
  "message": "Company created",
  "timestamp": "20xx-02-28T10:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 400 | Validation failed (blank name) |

---

### PUT /companies 🔒

**Request Body:**
```json
{
  "id": 2,
  "name": "FPT Software Updated",
  "description": "Technology services",
  "address": "Da Nang",
  "logo": "/logos/fpt-new.png"
}
```

**Success Response (200):** same structure as POST.

**Errors:**
| Status | When |
|--------|------|
| 400 | Validation failed |
| 404 | Company not found |

---

### DELETE /companies/{id} 🔒

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": null,
  "message": "Company deleted",
  "timestamp": "20xx-02-28T12:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 404 | Company not found |

**Note:** Deleting a company sets `company_id = null` on all associated users (does NOT delete users).

---

## 4. Roles

### GET /roles 🔒

List all roles with pagination.

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": {
    "meta": { "page": 1, "pageSize": 10, "pages": 1, "total": 5 },
    "result": [
      {
        "id": 1,
        "name": "ADMIN",
        "description": "Full system access",
        "permissions": [
          { "id": 1, "name": "CREATE_USER", "apiPath": "/api/v1/users", "method": "POST", "module": "USER" },
          { "id": 4, "name": "VIEW_USERS", "apiPath": "/api/v1/users", "method": "GET", "module": "USER" }
        ],
        "createdAt": "20xx-01-01T00:00:00Z",
        "updatedAt": null
      }
    ]
  },
  "message": "Fetch all roles",
  "timestamp": "20xx-02-28T10:00:00"
}
```

---

### GET /roles/{id} 🔒

**Success Response (200):** single role with permissions array (same structure as list item above).

**Errors:**
| Status | When |
|--------|------|
| 404 | Role not found |

---

### POST /roles 🔒

**Request Body:**
```json
{
  "name": "HR",
  "description": "Human resources management",
  "permissionIds": [1, 2, 4, 8]
}
```

**Success Response (201):**
```json
{
  "statusCode": 201,
  "data": {
    "id": 3,
    "name": "HR",
    "description": "Human resources management",
    "permissions": [
      { "id": 1, "name": "CREATE_USER", "apiPath": "/api/v1/users", "method": "POST", "module": "USER" },
      { "id": 2, "name": "UPDATE_USER", "apiPath": "/api/v1/users", "method": "PUT", "module": "USER" },
      { "id": 4, "name": "VIEW_USERS", "apiPath": "/api/v1/users", "method": "GET", "module": "USER" },
      { "id": 8, "name": "VIEW_COMPANIES", "apiPath": "/api/v1/companies", "method": "GET", "module": "COMPANY" }
    ],
    "createdAt": "20xx-02-28T10:00:00Z"
  },
  "message": "Role created",
  "timestamp": "20xx-02-28T10:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 400 | Validation failed (blank name) |
| 404 | One or more Permission IDs not found |

---

### PUT /roles 🔒

**Request Body:**
```json
{
  "id": 3,
  "name": "HR",
  "description": "Updated description",
  "permissionIds": [1, 2, 4, 5, 8]
}
```

**Success Response (200):** same structure as POST.

**Note:** `permissionIds` replaces the entire permission list (not additive).

**Errors:**
| Status | When |
|--------|------|
| 400 | Validation failed |
| 404 | Role or Permission not found |

---

### DELETE /roles/{id} 🔒

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": null,
  "message": "Role deleted",
  "timestamp": "20xx-02-28T12:00:00"
}
```

**Note:** Also removes this role from all users who had it (clears join table entries).

**Errors:**
| Status | When |
|--------|------|
| 404 | Role not found |

---

## 5. Permissions

### GET /permissions 🔒

List all permissions with pagination.

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": {
    "meta": { "page": 1, "pageSize": 10, "pages": 1, "total": 10 },
    "result": [
      {
        "id": 1,
        "name": "CREATE_USER",
        "apiPath": "/api/v1/users",
        "method": "POST",
        "module": "USER",
        "createdAt": "20xx-01-01T00:00:00Z",
        "updatedAt": null
      }
    ]
  },
  "message": "Fetch all permissions",
  "timestamp": "20xx-02-28T10:00:00"
}
```

---

### GET /permissions/{id} 🔒

**Success Response (200):** single permission (same structure as list item above).

**Errors:**
| Status | When |
|--------|------|
| 404 | Permission not found |

---

### POST /permissions 🔒

**Request Body:**
```json
{
  "name": "CREATE_USER",
  "apiPath": "/api/v1/users",
  "method": "POST",
  "module": "USER"
}
```

**Success Response (201):**
```json
{
  "statusCode": 201,
  "data": {
    "id": 1,
    "name": "CREATE_USER",
    "apiPath": "/api/v1/users",
    "method": "POST",
    "module": "USER",
    "createdAt": "20xx-02-28T10:00:00Z"
  },
  "message": "Permission created",
  "timestamp": "20xx-02-28T10:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 400 | Validation failed (blank name, invalid method) |
| 409 | Duplicate apiPath + method combination |

---

### PUT /permissions 🔒

**Request Body:**
```json
{
  "id": 1,
  "name": "CREATE_USER",
  "apiPath": "/api/v1/users",
  "method": "POST",
  "module": "USER"
}
```

**Success Response (200):** same structure as POST.

**Errors:**
| Status | When |
|--------|------|
| 400 | Validation failed |
| 404 | Permission not found |
| 409 | Duplicate apiPath + method (if changed to existing combo) |

---

### DELETE /permissions/{id} 🔒

**Success Response (200):**
```json
{
  "statusCode": 200,
  "data": null,
  "message": "Permission deleted",
  "timestamp": "20xx-02-28T12:00:00"
}
```

**Note:** Also removes this permission from all roles (clears join table entries).

**Errors:**
| Status | When |
|--------|------|
| 404 | Permission not found |

---

## Error Response Format

All errors follow this structure:

```json
{
  "statusCode": 400,
  "data": null,
  "message": "Detailed error message",
  "timestamp": "20xx-02-28T10:00:00"
}
```

Validation errors include field details:

```json
{
  "statusCode": 400,
  "data": {
    "email": "Invalid email format",
    "password": "Password must be 8-100 characters",
    "name": "Name is required"
  },
  "message": "Validation failed",
  "timestamp": "20xx-02-28T10:00:00"
}
```

---

## 6. Files

### POST /files 🔒

Upload a single file to the server. The returned `fileName` is then used to update
`avatar` (user) or `logo` (company) via their respective PUT endpoints.

**Request:** `multipart/form-data`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| file | file | yes | The file to upload |
| folder | string | yes | Target sub-folder: `avatars` or `logos` |

**Validation rules (rejected with 400 if violated):**

| Rule | Constraint |
|------|-----------|
| File name | Must not be blank, no special characters except `-` `_` `.` |
| Allowed extensions | `jpg`, `jpeg`, `png`, `gif`, `webp` |
| Max file size | 5 MB (5,242,880 bytes) |
| Allowed folders | `avatars`, `logos` |

**Success Response (201):**
```json
{
  "statusCode": 201,
  "data": {
    "fileName": "1709123456789_avatar.jpg",
    "folder": "avatars",
    "fileUrl": "/uploads/avatars/1709123456789_avatar.jpg",
    "size": 204800,
    "uploadedAt": "20xx-02-28T10:00:00Z"
  },
  "message": "File uploaded",
  "timestamp": "20xx-02-28T10:00:00"
}
```

**Errors:**
| Status | When |
|--------|------|
| 400 | No file provided |
| 400 | File name is blank or contains invalid characters |
| 400 | File extension not allowed (only jpg/jpeg/png/gif/webp) |
| 400 | File size exceeds 5 MB |
| 400 | Folder value is not `avatars` or `logos` |

**Usage flow:**

```
1. POST /api/v1/files  →  { fileName: "1709123456789_avatar.jpg", ... }
2a. PUT /api/v1/users  →  { id: 1, ..., avatar: "1709123456789_avatar.jpg" }
2b. PUT /api/v1/companies  →  { id: 1, ..., logo: "1709123456789_logo.png" }
```

**File storage:**
- Files are saved under `{upload-dir}/{folder}/` on the server file system
- `upload-dir` is configured via `app.upload.base-dir` in `application.yml`
- Stored file name = `{epochMillis}_{sanitizedOriginalName}` to avoid collisions
- Served as static resources at `/uploads/**`

---

## Endpoint Summary

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /auth/login | 🔓 | Login |
| POST | /auth/register | 🔓 | Register |
| POST | /auth/refresh | 🔓 | Refresh token |
| POST | /auth/logout | 🔒 | Logout |
| GET | /auth/me | 🔒 | Current user info |
| GET | /users | 🔒 | List users |
| GET | /users/{id} | 🔒 | Get user |
| POST | /users | 🔒 | Create user |
| PUT | /users | 🔒 | Update user |
| DELETE | /users/{id} | 🔒 | Delete user |
| GET | /companies | 🔒 | List companies |
| GET | /companies/{id} | 🔒 | Get company |
| POST | /companies | 🔒 | Create company |
| PUT | /companies | 🔒 | Update company |
| DELETE | /companies/{id} | 🔒 | Delete company |
| GET | /roles | 🔒 | List roles |
| GET | /roles/{id} | 🔒 | Get role |
| POST | /roles | 🔒 | Create role |
| PUT | /roles | 🔒 | Update role |
| DELETE | /roles/{id} | 🔒 | Delete role |
| GET | /permissions | 🔒 | List permissions |
| GET | /permissions/{id} | 🔒 | Get permission |
| POST | /permissions | 🔒 | Create permission |
| PUT | /permissions | 🔒 | Update permission |
| DELETE | /permissions/{id} | 🔒 | Delete permission |
| POST | /files | 🔒 | Upload file (avatar / logo) |