# Project Status

> Last updated: 2026-11-02 | By: Claude Code | Session: #2
>
> AI: update this file at the end of every session when asked.
> Follow this exact format. Keep it concise — under 80 lines.

---

## Completed
- ✅ Setup: project skeleton, docs (CLAUDE.md, PROJECT-RULES, ARCHITECTURE, DATABASE, API_SPEC), ADR-001 refresh token, ADR-002 file upload, .claude/commands
- ✅ 2026-11-02: Permission + Company CRUD (`/api/v1/permissions`, `/api/v1/companies`) — entity, repo, DTOs, service, controller, shared `PageResponse`, CONTEXT.md each, 22 unit tests passing

## In Progress
- 🔄 Integration tests `PermissionControllerTest` + `CompanyControllerTest` (14 cases each) written but NOT yet run — blocked by MySQL credentials

## Deferred Issues
- [P1] Permission delete must detach from roles once Role exists (Phase 2) — see feature/permission/CONTEXT.md
- [P1] Company delete must null out `users.company_id` once User has the `company` relation (Phase 3) — see feature/company/CONTEXT.md (also: logo file not deleted from storage, Phase 6)
- [P2] `application-test.yml` uses dev DB (rules want separate test DB + create-drop); legacy `UserController` returns entities, no `/api/v1` prefix (fix in Phase 3)

## Warnings
- ⚠️ MySQL rejects `root`/`123456` from application*.yml (Access denied) — every @SpringBootTest fails until fixed. `./mvnw` not executable → use `sh ./mvnw`
- ⚠️ `companies.name` is UNIQUE (decided this session, not in original spec) — relies on MySQL case-insensitive collation
- ⚠️ `AppException`/`InvalidRequestException` from PROJECT-RULES don't exist; code uses `DuplicateResourceException` (409) + `ResourceNotFoundException` (404)

## Next Tasks
1. **[P0]** Base exceptions (AppException, ResourceNotFoundException, InvalidRequestException) + GlobalExceptionHandler + ApiResponse record
2. **[P0]** SecurityConfig cơ bản (permitAll tạm) + JwtConfig (JwtEncoder/JwtDecoder — chuẩn bị sẵn, chưa enforce)
3. **[P1]** Fix MySQL credentials, run `PermissionControllerTest` + `CompanyControllerTest` → tick Phase 1 milestones
4. **[P1]** Role CRUD + ManyToMany Permission (Phase 2)

## Milestones

### Phase 0 — Foundation
- [ ] Base exception classes (AppException, ResourceNotFoundException, InvalidRequestException)
- [ ] GlobalExceptionHandler
- [ ] ApiResponse wrapper
- [ ] SecurityConfig (permitAll tạm thời)
- [ ] JwtConfig (JwtEncoder, JwtDecoder — chuẩn bị sẵn)

### Phase 1 — Independent Entities
- [ ] Permission CRUD + unit test + integration test + CONTEXT.md
- [ ] Company CRUD + unit test + integration test + CONTEXT.md

### Phase 2 — Role (depends on Permission)
- [ ] Role CRUD + ManyToMany Permission + test + CONTEXT.md

### Phase 3 — User (depends on Role + Company)
- [ ] User CRUD + ManyToOne Company + ManyToMany Role + test + CONTEXT.md

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