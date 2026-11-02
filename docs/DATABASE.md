# Database Schema

> MySQL database design for HR Management System.
> Update this file whenever schema changes.

---

## Entity Relationship Diagram

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────┐
│  companies   │       │      users       │       │    roles     │
├──────────────┤       ├──────────────────┤       ├──────────────┤
│ id (PK)      │       │ id (PK)          │       │ id (PK)      │
│ name         │◄──────│ company_id (FK)  │       │ name         │
│ description  │  1:N  │ name             │       │ description  │
│ address      │       │ email (UNIQUE)   │  N:M  │ created_at   │
│ logo         │       │ password         │◄─────►│ updated_at   │
│ created_at   │       │ age              │       └──────┬───────┘
│ updated_at   │       │ address          │              │
└──────────────┘       │ gender           │              │ N:M
                       │ avatar           │              │
                       │ created_at       │       ┌──────┴───────┐
                       │ updated_at       │       │ permissions  │
                       └────────┬─────────┘       ├──────────────┤
                                │                 │ id (PK)      │
                                │ 1:N             │ name         │
                                │                 │ api_path     │
                       ┌────────┴─────────┐       │ method       │
                       │ refresh_tokens   │       │ module       │
                       ├──────────────────┤       │ created_at   │
                       │ id (PK)          │       │ updated_at   │
                       │ token (UNIQUE)   │       └──────────────┘
                       │ user_id (FK)     │
                       │ expires_at       │       ┌────────────┐
                       │ revoked          │       │ user_role  │
                       │ device_info      │       ├────────────┤
                       │ ip_address       │       │ user_id    │
                       │ created_at       │       │ role_id    │
                       └──────────────────┘       └────────────┘

                                                  ┌─────────────────┐
                                                  │ permission_role  │
                                                  ├─────────────────┤
                                                  │ permission_id   │
                                                  │ role_id         │
                                                  └─────────────────┘
```

---

## Tables

### users

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(100) | NOT NULL | Full name |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Login email |
| password | VARCHAR(255) | NOT NULL | BCrypt hash |
| age | INT | NULLABLE | |
| address | VARCHAR(255) | NULLABLE | |
| gender | VARCHAR(20) | NULLABLE | Enum: MALE, FEMALE, OTHER |
| avatar | VARCHAR(255) | NULLABLE | Avatar image path/URL |
| company_id | BIGINT | FK → companies(id), NULLABLE | Employee's company |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | NULLABLE, ON UPDATE CURRENT_TIMESTAMP | |

Indexes:
- `UNIQUE INDEX idx_users_email ON users(email)`
- `INDEX idx_users_company ON users(company_id)`

### companies

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(255) | NOT NULL | Company name |
| description | TEXT | NULLABLE | |
| address | VARCHAR(255) | NULLABLE | |
| logo | VARCHAR(255) | NULLABLE | Logo image path/URL |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | NULLABLE, ON UPDATE CURRENT_TIMESTAMP | |

### roles

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(100) | NOT NULL | e.g. ADMIN, HR, USER, MANAGER |
| description | VARCHAR(255) | NULLABLE | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | NULLABLE, ON UPDATE CURRENT_TIMESTAMP | |

### permissions

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(100) | NOT NULL | e.g. CREATE_USER, DELETE_USER, VIEW_REPORT |
| api_path | VARCHAR(255) | NOT NULL | e.g. /api/v1/users |
| method | VARCHAR(10) | NOT NULL | GET, POST, PUT, DELETE |
| module | VARCHAR(100) | NOT NULL | e.g. USER, COMPANY, ROLE |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | NULLABLE, ON UPDATE CURRENT_TIMESTAMP | |

Indexes:
- `UNIQUE INDEX idx_permissions_path_method ON permissions(api_path, method)`

### user_role (Join Table)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_id | BIGINT | FK → users(id), NOT NULL | |
| role_id | BIGINT | FK → roles(id), NOT NULL | |

- Composite PK: `(user_id, role_id)`
- ON DELETE CASCADE for both FKs

### permission_role (Join Table)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| permission_id | BIGINT | FK → permissions(id), NOT NULL | |
| role_id | BIGINT | FK → roles(id), NOT NULL | |

- Composite PK: `(permission_id, role_id)`
- ON DELETE CASCADE for both FKs

### refresh_tokens

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| token | VARCHAR(512) | NOT NULL, UNIQUE | JWT refresh token (or hash of it) |
| user_id | BIGINT | FK → users(id), NOT NULL | Token owner |
| expires_at | TIMESTAMP | NOT NULL | When this token expires |
| revoked | BOOLEAN | NOT NULL, DEFAULT false | true = invalidated (logout/rotation) |
| device_info | VARCHAR(255) | NULLABLE | e.g. "Chrome 120 on Windows 11", "iPhone 15 Pro" |
| ip_address | VARCHAR(45) | NULLABLE | IPv4 or IPv6 (max 45 chars) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |

Indexes:
- `UNIQUE INDEX idx_refresh_tokens_token ON refresh_tokens(token)`
- `INDEX idx_refresh_tokens_user ON refresh_tokens(user_id)`
- `INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at)` — for cleanup job

Notes:
- `token` column: store hash (SHA-256) of the JWT, not raw JWT — if DB is compromised, attacker cannot reuse tokens
- `ip_address` VARCHAR(45): supports both IPv4 ("192.168.1.1") and IPv6 ("2001:0db8:85a3:0000:0000:8a2e:0370:7334")
- `device_info`: parsed from `User-Agent` header — best effort, not security-critical
- Cleanup: scheduled job should DELETE rows where `expires_at < NOW()` to prevent table bloat

---

## Relationships Summary

| Relationship | Type | Owner Side | Join Config |
|-------------|------|-----------|-------------|
| User → Company | ManyToOne | User | `@JoinColumn(name = "company_id")` |
| User ↔ Role | ManyToMany | User | `@JoinTable(name = "user_role")` |
| Role ↔ Permission | ManyToMany | Role | `@JoinTable(name = "permission_role")` |
| User → RefreshToken | OneToMany | RefreshToken | `@JoinColumn(name = "user_id")` |

### JPA Mapping Notes
- All `@ManyToOne`: use `FetchType.LAZY`
- All `@ManyToMany`: use `FetchType.LAZY`
- Inverse side (`@ManyToMany(mappedBy = "...")`) must have `@JsonIgnore` to prevent infinite recursion
- Deleting a User does NOT cascade delete the Company
- Deleting a User DOES remove entries in `user_role` (cascade on join table)
- Deleting a User SHOULD revoke all their refresh tokens (set `revoked = true`), not cascade delete
- Deleting a Role DOES remove entries in `permission_role` and `user_role`
- RefreshToken: never cascade delete from User — revoke instead (audit trail)

---

## JPA Entity Mapping Reference

### User.java
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "company_id")
private Company company;

@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "user_role",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
private List<Role> roles;

@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
@JsonIgnore
private List<RefreshToken> refreshTokens;
```

### RefreshToken.java
```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;           // SHA-256 hash of JWT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(length = 255)
    private String deviceInfo;      // Parsed from User-Agent

    @Column(length = 45)
    private String ipAddress;       // IPv4 or IPv6

    @CreationTimestamp
    private Instant createdAt;

    // No-arg constructor (required by JPA)
    public RefreshToken() {}

    // Getters and Setters ...
}
```

### Company.java
```java
@OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
@JsonIgnore
private List<User> users;
```

### Role.java
```java
@ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
@JsonIgnore
private List<User> users;

@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "permission_role",
    joinColumns = @JoinColumn(name = "role_id"),
    inverseJoinColumns = @JoinColumn(name = "permission_id")
)
private List<Permission> permissions;
```

### Permission.java
```java
@ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
@JsonIgnore
private List<Role> roles;
```

---

## Sample Data

### Roles
| id | name | description |
|----|------|-------------|
| 1 | SUPER_ADMIN | Full system access |
| 2 | ADMIN | Company-level admin |
| 3 | HR | Human resources management |
| 4 | MANAGER | Department manager |
| 5 | USER | Regular employee |

### Permissions (examples)
| id | name | api_path | method | module |
|----|------|----------|--------|--------|
| 1 | CREATE_USER | /api/v1/users | POST | USER |
| 2 | UPDATE_USER | /api/v1/users | PUT | USER |
| 3 | DELETE_USER | /api/v1/users/{id} | DELETE | USER |
| 4 | VIEW_USERS | /api/v1/users | GET | USER |
| 5 | CREATE_COMPANY | /api/v1/companies | POST | COMPANY |
| 6 | UPDATE_COMPANY | /api/v1/companies | PUT | COMPANY |
| 7 | DELETE_COMPANY | /api/v1/companies/{id} | DELETE | COMPANY |
| 8 | VIEW_COMPANIES | /api/v1/companies | GET | COMPANY |
| 9 | ASSIGN_ROLE | /api/v1/roles/assign | POST | ROLE |
| 10 | VIEW_ROLES | /api/v1/roles | GET | ROLE |

### Role → Permission Mapping
| Role | Permissions |
|------|-------------|
| SUPER_ADMIN | All permissions |
| ADMIN | All except system config |
| HR | CREATE_USER, UPDATE_USER, VIEW_USERS, VIEW_COMPANIES |
| MANAGER | VIEW_USERS, VIEW_COMPANIES, VIEW_ROLES |
| USER | VIEW_USERS (self only), VIEW_COMPANIES |

---

## Migration Notes

- **Total tables: 7** — users, companies, roles, permissions, user_role, permission_role, refresh_tokens
- Schema managed by Hibernate `ddl-auto`:
  - `dev` profile: `update` (auto-create/alter tables)
  - `prod` profile: `validate` (manual migration only)
- For production: use Flyway or Liquibase for versioned migrations (not yet set up)
- Gender stored as `VARCHAR(20)` with `@Enumerated(EnumType.STRING)` — never ORDINAL
- `refresh_tokens` cleanup: schedule a job to DELETE expired rows (`expires_at < NOW()`) daily to prevent table bloat