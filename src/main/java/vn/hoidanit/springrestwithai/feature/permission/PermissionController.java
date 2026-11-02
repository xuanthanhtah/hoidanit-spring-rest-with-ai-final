package vn.hoidanit.springrestwithai.feature.permission;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.hoidanit.springrestwithai.dto.ApiResponse;
import vn.hoidanit.springrestwithai.dto.PageResponse;
import vn.hoidanit.springrestwithai.feature.permission.dto.CreatePermissionRequest;
import vn.hoidanit.springrestwithai.feature.permission.dto.PermissionResponse;
import vn.hoidanit.springrestwithai.feature.permission.dto.UpdatePermissionRequest;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PermissionResponse>>> getAllPermissions(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        PageResponse<PermissionResponse> permissions = this.permissionService.getAllPermissions(pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quyền hạn thành công", permissions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermissionById(@PathVariable Long id) {
        PermissionResponse permission = this.permissionService.getPermissionById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin quyền hạn thành công", permission));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        PermissionResponse created = this.permissionService.createPermission(request);
        URI location = URI.create("/api/v1/permissions/" + created.id());
        return ResponseEntity.created(location)
                .body(ApiResponse.created("Tạo quyền hạn thành công", created));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
            @Valid @RequestBody UpdatePermissionRequest request) {
        PermissionResponse updated = this.permissionService.updatePermission(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quyền hạn thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        this.permissionService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa quyền hạn thành công", null));
    }
}
