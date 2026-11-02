# Permission — Implementation Context
> Written: 2026-11-02 | Author: Claude Code (session #1)

## Business Context
A Permission maps one API endpoint (`apiPath` + HTTP `method`) to a named action (e.g. `CREATE_USER`) grouped by `module`.
Roles will bundle permissions (Phase 2), and the RBAC filter (Phase 7) will match incoming requests against them.

## Technical Decisions
- **Uniqueness on (`api_path`, `method`), not `name`**: the RBAC matcher looks up by path + method, so that pair must be unique. Enforced twice: a service-level `exists...` check (clean 409) and a DB unique constraint `idx_permissions_path_method` (race safety).
- **Update uses `existsByApiPathAndMethodAndIdNot`**: lets a permission be saved unchanged without colliding with itself.
- **`method` is a validated `String` (`GET|POST|PUT|PATCH|DELETE`), not an enum**: keeps the JSON/DB value identical to the HTTP verb and avoids enum deserialization errors returning a generic 400.
- **`PUT /api/v1/permissions` with `id` in body**: follows `docs/API_SPEC.md` (differs from `PUT /users/{id}` in the legacy user module).
- **Shared `dto/PageResponse<T>` (meta + result) with 1-based pages**: matches the API_SPEC list format; `spring.data.web.pageable.one-indexed-parameters=true` makes `?page=1` the first page. Company/Role/User should reuse it.
- **Reused `DuplicateResourceException` (409) / `ResourceNotFoundException` (404)**: the `AppException` hierarchy from PROJECT-RULES does not exist yet.

## Considered and Rejected
- **Returning Spring `Page<T>` directly**: rejected — leaks Spring internals (`pageable`, `sort`, 0-based `number`) into the API contract.
- **Unique constraint on `name`**: rejected — names are labels for humans; the endpoint pair is the real identity.

## Dependencies
- Depends on: none (shared `dto/`, `exception/` only)
- Depended by: `role` (Phase 2, ManyToMany via `permission_role`), RBAC filter (Phase 7)

## Known Limitations
- ⚠️ No `roles` inverse mapping yet — `Role` does not exist. Phase 2 must add `@ManyToMany(mappedBy = "permissions", fetch = LAZY) @JsonIgnore List<Role> roles` and make `deletePermission` detach the permission from roles before deleting (otherwise the FK on `permission_role` blocks the delete).
- ⚠️ No authorization beyond "authenticated" — any valid JWT can manage permissions until Phase 7 RBAC.
- ⚠️ `apiPath` is stored as-is (no normalization of trailing slashes / case) — RBAC matcher must use the same format.
- ⚠️ Integration tests run against the shared dev DB (`application-test.yml`) and rely on `@Transactional` rollback.

## Refactor Log
(Add entries here when significant changes are made. Do NOT edit sections above.)
