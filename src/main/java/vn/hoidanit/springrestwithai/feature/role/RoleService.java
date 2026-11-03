package vn.hoidanit.springrestwithai.feature.role;

import org.springframework.data.domain.Pageable;

import vn.hoidanit.springrestwithai.dto.PageResponse;
import vn.hoidanit.springrestwithai.feature.role.dto.CreateRoleRequest;
import vn.hoidanit.springrestwithai.feature.role.dto.RoleResponse;
import vn.hoidanit.springrestwithai.feature.role.dto.UpdateRoleRequest;

public interface RoleService {

    PageResponse<RoleResponse> getAllRoles(Pageable pageable);

    RoleResponse getRoleById(Long id);

    RoleResponse createRole(CreateRoleRequest request);

    RoleResponse updateRole(UpdateRoleRequest request);

    void deleteRole(Long id);
}
