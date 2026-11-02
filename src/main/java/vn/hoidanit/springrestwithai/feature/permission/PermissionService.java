package vn.hoidanit.springrestwithai.feature.permission;

import org.springframework.data.domain.Pageable;

import vn.hoidanit.springrestwithai.dto.PageResponse;
import vn.hoidanit.springrestwithai.feature.permission.dto.CreatePermissionRequest;
import vn.hoidanit.springrestwithai.feature.permission.dto.PermissionResponse;
import vn.hoidanit.springrestwithai.feature.permission.dto.UpdatePermissionRequest;

public interface PermissionService {

    PageResponse<PermissionResponse> getAllPermissions(Pageable pageable);

    PermissionResponse getPermissionById(Long id);

    PermissionResponse createPermission(CreatePermissionRequest request);

    PermissionResponse updatePermission(UpdatePermissionRequest request);

    void deletePermission(Long id);
}
