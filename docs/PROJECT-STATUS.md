# Project Status

> Last updated: 2026-11-03 | By: Claude Code | Session: #4
>
> AI: update this file at the end of every session when asked.
> Follow this exact format. Keep it concise — under 80 lines.

---

## Completed
- ✅ Setup: project skeleton, docs (CLAUDE.md, PROJECT-RULES, ARCHITECTURE, DATABASE, API_SPEC), ADR-001 refresh token, ADR-002 file upload, .claude/commands
- ✅ 2026-11-02: Permission + Company CRUD (`/api/v1/permissions`, `/api/v1/companies`) — entity, repo, DTOs, service, controller, shared `PageResponse`, CONTEXT.md each
- ✅ 2026-11-02: Role CRUD (`/api/v1/roles`) + ManyToMany Permission (Role owns `permission_role`, `Permission.roles` inverse); permission delete detaches from roles — 81/81 tests passing (unit + integration on MySQL test DB)

- ✅ 2026-11-03: User CRUD (`/api/v1/users`) rewritten with DTOs + ManyToOne Company + ManyToMany Role (User owns `user_role`); company/role delete now detach users — 115/115 tests passing

## In Progress
- (none)

## Deferred Issues
- [P1] User delete must revoke refresh tokens (Phase 5) — see feature/user/CONTEXT.md
- [P2] Company delete does not remove logo file from storage (Phase 6)
- [P2] `CustomUserDetailsService` grants hard-coded `ROLE_USER` — load real roles in Phase 4

## Warnings
- ⚠️ `./mvnw` not executable → use `sh ./mvnw`
- ⚠️ `companies.name` and `roles.name` are UNIQUE (decided, not in original spec) — rely on MySQL case-insensitive collation
- ⚠️ `AppException`/`InvalidRequestException` from PROJECT-RULES don't exist; code uses `DuplicateResourceException` (409) + `ResourceNotFoundException` (404)

## Next Tasks
1. **[P0]** Base exceptions (AppException, ResourceNotFoundException, InvalidRequestException) + GlobalExceptionHandler + ApiResponse record
2. **[P0]** SecurityConfig cơ bản (permitAll tạm) + JwtConfig (JwtEncoder/JwtDecoder — chuẩn bị sẵn, chưa enforce)
3. **[P1]** Phase 4 — Authentication (login/register/me, enforce JWT)

## Milestones

### Phase 0 — Foundation
- [ ] Base exception classes (AppException, ResourceNotFoundException, InvalidRequestException)
- [ ] GlobalExceptionHandler
- [ ] ApiResponse wrapper
- [ ] SecurityConfig (permitAll tạm thời)
- [ ] JwtConfig (JwtEncoder, JwtDecoder — chuẩn bị sẵn)

### Phase 1 — Independent Entities
- [x] Permission CRUD + unit test + integration test + CONTEXT.md
- [x] Company CRUD + unit test + integration test + CONTEXT.md

### Phase 2 — Role (depends on Permission)
- [x] Role CRUD + ManyToMany Permission + test + CONTEXT.md

### Phase 3 — User (depends on Role + Company)
- [x] User CRUD + ManyToOne Company + ManyToMany Role + test + CONTEXT.md

### Phase 4 — Authentication
- [ ] CustomUserDetailsService
- [ ] POST /auth/login + POST /auth/register + test
- [ ] Enable JWT enforce in SecurityConfig
- [ ] GET /auth/me + test

### Phase 5 — Refresh Token (ADR-001)
- [ ] RefreshToken entity + repository
- [ ] POST /auth/refresh (cookie SPA + body mobile)
- [ ] POST /auth/logout (revoke + clear cookie)
- [ ] Full auth flow test

### Phase 6 — File Upload (ADR-002)
- [ ] StorageService (upload, delete, getUrl)
- [ ] POST /api/v1/files/upload (multipart/form-data)
- [ ] File validation (size, MIME type whitelist)
- [ ] Integration test

### Phase 7 — RBAC (Permission-based Authorization)
- [ ] Middleware: match request (path + method) → Permission → Role
- [ ] Integrate into SecurityFilterChain
- [ ] Test: 200 (authorized) + 403 (forbidden)
- [ ] Add 401/403 test cases to Phase 1-3 endpoints

### Phase 8 — Polish
- [ ] Pagination + sorting for all list endpoints
- [ ] Search / filter (if needed)
- [ ] Scheduled job: cleanup expired refresh tokens
- [ ] Full review (/review-pr) + final docs update