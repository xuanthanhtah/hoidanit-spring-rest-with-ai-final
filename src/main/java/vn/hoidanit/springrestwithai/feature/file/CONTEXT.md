# File — Implementation Context
> Written: 2026-11-03 | Author: Claude Code (session #4)

## Business Context
Uploads a single image (user avatar or company logo) and returns its stored `fileName`.
The client then saves that name through `PUT /api/v1/users` (`avatar`) or `PUT /api/v1/companies` (`logo`) — ADR-002.

## Technical Decisions
- **No entity / table**: files live on disk under `{app.upload.base-dir}/{folder}/`; only the name is stored on User/Company.
- **Invalid file names are rejected (400), not sanitized** (user decision 2026-11-03, follows API_SPEC over the ADR-002 example):
  only `[A-Za-z0-9._-]`, max 200 chars (so `{epochMillis}_{name}` fits the 255-char avatar/logo columns).
- **Validation order**: folder whitelist → file present/non-empty → name → extension (case-insensitive, last dot) → size ≤ 5 MB.
  All limits/whitelists come from `UploadProperties` (`app.upload.*`).
- **Path traversal**: name regex blocks `/` and `\`; target is still `normalize()`d and prefix-checked (defense in depth).
- **Size limit is checked twice**: servlet `spring.servlet.multipart.max-file-size: 5MB` and the service check.
  `MaxUploadSizeExceededException` is mapped to **400** (API_SPEC), not the default 413.
- **Serving**: `WebConfig` registers a resource handler `/uploads/** → file:{base-dir}/` instead of overriding
  `spring.web.resources.static-locations` (ADR-002 snippet) — that would serve files at `/avatars/...` and drop `classpath:/static`.
  `SecurityConfig` permits `GET /uploads/**`; `POST /api/v1/files` requires JWT.
- **Missing `file` part / `folder` param → 400**, non-multipart request → 415 (new handlers in `GlobalExceptionHandler`).

## Considered and Rejected
- **Sanitizing names** (ADR-002 example): rejected in favor of the API_SPEC rule above.
- **UUID names**: ADR-002 chose `System.currentTimeMillis()` prefix for readability.
- **Content sniffing (magic bytes)**: not in spec; files are served with an image Content-Type by extension, so a disguised file is not executed.

## Dependencies
- Depends on: `config/UploadProperties`, `config/WebConfig`, `exception/InvalidRequestException`, `exception/FileStorageException`
- Depended by: none in code — clients use the returned name for `user.avatar` / `company.logo`

## Known Limitations
- ⚠️ Two uploads of the same name in the same millisecond collide → `FileAlreadyExistsException` → 500 (accepted in ADR-002).
- ⚠️ Orphan files: uploads never attached to a user/company, and old avatars/logos after replacement or delete, are not cleaned up.
- ⚠️ Local disk only — multiple instances need a shared volume (ADR-002).
- ⚠️ `avatar`/`logo` are not checked to exist on disk when saved via PUT.

## Refactor Log
(Add entries here when significant changes are made. Do NOT edit sections above.)
