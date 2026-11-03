# Role — Implementation Context
> Written: 2026-11-02 | Author: Claude Code (session #3)

## Business Context
A Role is a named bundle of Permissions (e.g. `HR` = CREATE_USER + VIEW_USERS + ...).
Users will get roles in Phase 3, and the RBAC filter (Phase 7) will check user → roles → permissions against the request.

## Technical Decisions
- **Role ↔ Permission is ManyToMany, Role is the owning side** (`@JoinTable(name = "permission_role")`). Only changes to `Role.permissions` are written to the join table; `Permission.roles` (`mappedBy`) is read-only for JPA and `@JsonIgnore`d.
- **Delete semantics follow from ownership**:
  - Delete Role → Hibernate removes its `permission_role` rows automatically; permissions are kept.
  - Delete Permission (inverse side) → `PermissionServiceImpl.deletePermission` must first remove it from every `role.getPermissions()`, otherwise the FK on `permission_role` blocks the delete. Covered by `RoleControllerTest.deletePermission_assignedToRole_detachesFromRole`.
- **`permissionIds` replaces the whole list on update** (not additive), per API_SPEC. `null`/`[]` = no permissions. Ids are de-duplicated (`LinkedHashSet`) before `findAllById`, so `[1,1]` does not count as "missing".
- **Missing permission ids → 404 listing all missing ids**, detected by comparing `findAllById` size with the requested set (one query, not one per id).
- **Response embeds `PermissionSummary` (nested record in `RoleResponse`)** — id/name/apiPath/method/module only, matching API_SPEC; no timestamps, no back-reference to roles (no recursion).
- **Read methods are `@Transactional(readOnly = true)`** so the LAZY `permissions` can be mapped to DTOs inside the service without relying on open-in-view.
- **`@BatchSize(size = 50)` on `Role.permissions`** instead of `@EntityGraph` on `findAll(Pageable)`: fetch-joining a collection with pagination makes Hibernate paginate in memory (HHH90003004).
- **`name` is UNIQUE** (service `existsByName` / `existsByNameAndIdNot` → 409, plus DB unique constraint for race safety) — same pattern as Permission/Company.

## Considered and Rejected
- **`Set<Permission>`**: rejected for now to match the List mapping in DATABASE.md; duplicates are prevented by de-duplicating ids in the service instead.
- **Cascade on `Role.permissions`**: rejected — permissions have their own lifecycle; cascading REMOVE would delete shared permissions.
- **Separate assign/unassign endpoints**: not in API_SPEC; full replacement via PUT is enough for now.

## Dependencies
- Depends on: `permission` (`PermissionRepository`, `Permission` entity), shared `dto/PageResponse`, `exception/`
- Depended by: `user` (Phase 3, ManyToMany via `user_role`), RBAC filter (Phase 7)

## Known Limitations
- ⚠️ No `users` inverse mapping yet — Phase 3 must add `@ManyToMany(mappedBy = "roles", fetch = LAZY) @JsonIgnore List<User> users` and make `deleteRole` detach the role from users (same issue as Permission delete: Role will be the inverse side of `user_role`).
- ⚠️ Role name uniqueness relies on MySQL case-insensitive collation (`hr` vs `HR` → 409 only via DB).
- ⚠️ No authorization beyond "authenticated" until Phase 7 RBAC.

## Refactor Log
(Add entries here when significant changes are made. Do NOT edit sections above.)
