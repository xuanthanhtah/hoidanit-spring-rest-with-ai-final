# Company — Implementation Context
> Written: 2026-11-02 | Author: Claude Code (session #1)

## Business Context
A Company is the employer organization in the HR system (name, description, address, logo).
Users (employees) will belong to at most one company (Phase 3, `users.company_id`).

## Technical Decisions
- **`name` is unique**: added on top of the original spec (user decision, 2026-11-02) so companies can't be duplicated. Enforced twice: a service-level `existsByName` check (clean 409) and a DB unique constraint `idx_companies_name` (race safety).
- **Update uses `existsByNameAndIdNot`**: lets a company be saved with its own unchanged name without colliding with itself.
- **Mirrors the Permission module structure**: `PUT /api/v1/companies` with `id` in body, shared `dto/PageResponse<T>` (1-based meta + result), Vietnamese response messages, `DuplicateResourceException` (409) / `ResourceNotFoundException` (404).
- **`description` is `TEXT`; request validates `@Size(max = 65535)`**: matches the DATABASE.md column type and returns a 400 instead of a DB error on oversized input.
- **`logo` is a plain string (path/URL)**: file upload is a separate flow (ADR-002, `POST /files`); the client uploads first, then sends the returned path here.

## Considered and Rejected
- **Partial update (PATCH semantics) on PUT**: rejected — PUT replaces the whole resource, so omitted optional fields become `null`; consistent with Permission.
- **Case-insensitive `existsByNameIgnoreCase`**: rejected for now — MySQL's default collation already compares case-insensitively, so the DB constraint and the derived query agree.

## Dependencies
- Depends on: none (shared `dto/`, `exception/` only)
- Depended by: `user` (Phase 3, ManyToOne `company`)

## Known Limitations
- ⚠️ No `users` inverse mapping yet — `User` has no `company` relation. Phase 3 must add `@OneToMany(mappedBy = "company", fetch = LAZY)` and make `deleteCompany` set `company = null` on its users before deleting (API_SPEC requires users to survive; otherwise the FK blocks the delete).
- ⚠️ Deleting a company does not remove its logo file from storage — handle when StorageService exists (Phase 6).
- ⚠️ No authorization beyond "authenticated" — any valid JWT can manage companies until Phase 7 RBAC.
- ⚠️ Name uniqueness depends on DB collation: on a case-sensitive collation, `"FPT"` and `"fpt"` would both be allowed.
- ⚠️ Integration tests run against the shared dev DB (`application-test.yml`) and rely on `@Transactional` rollback.

## Refactor Log
(Add entries here when significant changes are made. Do NOT edit sections above.)
