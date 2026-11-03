# User — Implementation Context
> Written: 2026-11-03 | Author: Claude Code (session #4)

## Business Context
A User is an employee account: profile (name, age, gender, address, avatar), login credentials (email + BCrypt password),
at most one Company (employer) and any number of Roles (which carry Permissions for RBAC in Phase 7).

## Technical Decisions
- **User owns both relations**:
  - `@ManyToOne(LAZY) @JoinColumn("company_id") company` (nullable, indexed `idx_users_company`)
  - `@ManyToMany(LAZY) @JoinTable("user_role") roles` (`@BatchSize(50)`)
  - Inverse sides `Company.users` / `Role.users` are `mappedBy`, LAZY and `@JsonIgnore`d.
- **Delete semantics follow from ownership**:
  - Delete User → Hibernate removes its `user_role` rows; company and roles are kept.
  - Delete Role (inverse) → `RoleServiceImpl.deleteRole` removes the role from every `user.getRoles()` first.
  - Delete Company (inverse) → `CompanyServiceImpl.deleteCompany` sets `user.company = null` first; users survive.
  - Covered by `UserControllerTest.deleteCompany_withUsers_detachesUsers` / `deleteRole_assignedToUser_detachesFromUser`.
- **PUT replaces the resource**: `companyId = null` clears the company; `roleIds` replaces the whole list (`null`/`[]` = no roles).
  Role ids are de-duplicated; all missing ids are reported in one 404 (same pattern as Role → Permission).
- **Email and password are not updatable via PUT** (API_SPEC). Email unique → 409 (service check + DB unique constraint).
- **Password 8–100 chars** (PROJECT-RULES/API_SPEC), BCrypt-encoded; never returned (`UserResponse` has no password field).
- **Response embeds `CompanySummary(id, name)` and `RoleSummary(id, name)`** — nested records, no back-references.
- **List query**: `findAll(Pageable)` overridden with `@EntityGraph("company")` (ManyToOne fetch-join is safe with paging);
  roles come via `@BatchSize` (collection fetch-join would paginate in memory). Read methods are `@Transactional(readOnly = true)`.

## Considered and Rejected
- **Blocking company/role delete while users reference it (409)**: rejected — API_SPEC/DATABASE.md say users survive / join rows are removed.
- **Cascade on `company` / `roles`**: rejected — both have their own lifecycle.
- **Updating email/password via PUT**: rejected per spec; will be dedicated flows (auth / change-password).

## Dependencies
- Depends on: `company` (`CompanyRepository`), `role` (`RoleRepository`), `PasswordEncoder` (`config/SecurityConfig`), shared `dto/`, `exception/`
- Depended by: `security/CustomUserDetailsService`, auth (Phase 4), refresh tokens (Phase 5), RBAC (Phase 7)

## Known Limitations
- ⚠️ Deleting a user does not revoke refresh tokens yet — RefreshToken arrives in Phase 5 (API_SPEC requires revoke).
- ⚠️ `CustomUserDetailsService` still grants a hard-coded `ROLE_USER`; real roles/permissions are loaded in Phase 4/7.
- ⚠️ Email uniqueness relies on MySQL case-insensitive collation for `A@x.com` vs `a@x.com`.
- ⚠️ No authorization beyond "authenticated" until Phase 7 RBAC.

## Refactor Log
(Add entries here when significant changes are made. Do NOT edit sections above.)
