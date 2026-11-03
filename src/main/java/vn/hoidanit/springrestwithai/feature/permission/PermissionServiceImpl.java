package vn.hoidanit.springrestwithai.feature.permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.springrestwithai.dto.PageResponse;
import vn.hoidanit.springrestwithai.exception.DuplicateResourceException;
import vn.hoidanit.springrestwithai.exception.ResourceNotFoundException;
import vn.hoidanit.springrestwithai.feature.permission.dto.CreatePermissionRequest;
import vn.hoidanit.springrestwithai.feature.permission.dto.PermissionResponse;
import vn.hoidanit.springrestwithai.feature.permission.dto.UpdatePermissionRequest;

@Service
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);
    private static final String RESOURCE_NAME = "Quyền hạn";

    private final PermissionRepository permissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public PageResponse<PermissionResponse> getAllPermissions(Pageable pageable) {
        Page<Permission> page = this.permissionRepository.findAll(pageable);
        return PageResponse.of(page, page.map(PermissionResponse::fromEntity).getContent());
    }

    @Override
    public PermissionResponse getPermissionById(Long id) {
        return PermissionResponse.fromEntity(findPermissionOrThrow(id));
    }

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (this.permissionRepository.existsByApiPathAndMethod(request.apiPath(), request.method())) {
            throw duplicateException(request.method(), request.apiPath());
        }
        Permission permission = new Permission();
        permission.setName(request.name());
        permission.setApiPath(request.apiPath());
        permission.setMethod(request.method());
        permission.setModule(request.module());
        Permission saved = this.permissionRepository.save(permission);
        log.info("Created permission id={} ({} {})", saved.getId(), saved.getMethod(), saved.getApiPath());
        return PermissionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(UpdatePermissionRequest request) {
        Permission permission = findPermissionOrThrow(request.id());
        if (this.permissionRepository.existsByApiPathAndMethodAndIdNot(
                request.apiPath(), request.method(), request.id())) {
            throw duplicateException(request.method(), request.apiPath());
        }
        permission.setName(request.name());
        permission.setApiPath(request.apiPath());
        permission.setMethod(request.method());
        permission.setModule(request.module());
        Permission saved = this.permissionRepository.save(permission);
        log.info("Updated permission id={}", saved.getId());
        return PermissionResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        Permission permission = findPermissionOrThrow(id);
        // Permission is the inverse side: detach from owning roles so permission_role rows are removed
        permission.getRoles().forEach(role -> role.getPermissions().remove(permission));
        permission.getRoles().clear();
        this.permissionRepository.delete(permission);
        log.info("Deleted permission id={}", id);
    }

    private Permission findPermissionOrThrow(Long id) {
        return this.permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "id", id));
    }

    private DuplicateResourceException duplicateException(String method, String apiPath) {
        return new DuplicateResourceException(RESOURCE_NAME, "apiPath + method", method + " " + apiPath);
    }
}
