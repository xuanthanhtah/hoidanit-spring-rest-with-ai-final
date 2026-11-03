package vn.hoidanit.springrestwithai.feature.role;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.springrestwithai.dto.PageResponse;
import vn.hoidanit.springrestwithai.exception.DuplicateResourceException;
import vn.hoidanit.springrestwithai.exception.ResourceNotFoundException;
import vn.hoidanit.springrestwithai.feature.permission.Permission;
import vn.hoidanit.springrestwithai.feature.permission.PermissionRepository;
import vn.hoidanit.springrestwithai.feature.role.dto.CreateRoleRequest;
import vn.hoidanit.springrestwithai.feature.role.dto.RoleResponse;
import vn.hoidanit.springrestwithai.feature.role.dto.UpdateRoleRequest;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);
    private static final String RESOURCE_NAME = "Vai trò";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    // readOnly transaction keeps the session open while the lazy permissions are mapped to DTOs
    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> getAllRoles(Pageable pageable) {
        Page<Role> page = this.roleRepository.findAll(pageable);
        return PageResponse.of(page, page.map(RoleResponse::fromEntity).getContent());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        return RoleResponse.fromEntity(findRoleOrThrow(id));
    }

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (this.roleRepository.existsByName(request.name())) {
            throw duplicateException(request.name());
        }
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissions(findPermissionsOrThrow(request.permissionIds()));
        Role saved = this.roleRepository.save(role);
        log.info("Created role id={} ({}) with {} permissions",
                saved.getId(), saved.getName(), saved.getPermissions().size());
        return RoleResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(UpdateRoleRequest request) {
        Role role = findRoleOrThrow(request.id());
        if (this.roleRepository.existsByNameAndIdNot(request.name(), request.id())) {
            throw duplicateException(request.name());
        }
        role.setName(request.name());
        role.setDescription(request.description());
        // permissionIds replaces the whole list (not additive)
        role.setPermissions(findPermissionsOrThrow(request.permissionIds()));
        Role saved = this.roleRepository.save(role);
        log.info("Updated role id={} with {} permissions", saved.getId(), saved.getPermissions().size());
        return RoleResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = findRoleOrThrow(id);
        // Role is the inverse side of user_role: detach from owning users so their join rows are removed
        role.getUsers().forEach(user -> user.getRoles().remove(role));
        role.getUsers().clear();
        // Role owns permission_role, so Hibernate removes its join rows; permissions themselves are kept
        this.roleRepository.delete(role);
        log.info("Deleted role id={}", id);
    }

    private Role findRoleOrThrow(Long id) {
        return this.roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "id", id));
    }

    private List<Permission> findPermissionsOrThrow(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(permissionIds);
        List<Permission> permissions = this.permissionRepository.findAllById(uniqueIds);
        if (permissions.size() != uniqueIds.size()) {
            permissions.forEach(permission -> uniqueIds.remove(permission.getId()));
            throw new ResourceNotFoundException("Quyền hạn", "id", uniqueIds);
        }
        return new ArrayList<>(permissions);
    }

    private DuplicateResourceException duplicateException(String name) {
        return new DuplicateResourceException(RESOURCE_NAME, "name", name);
    }
}
