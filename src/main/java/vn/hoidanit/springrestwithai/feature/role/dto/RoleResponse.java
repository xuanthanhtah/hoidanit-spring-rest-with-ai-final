package vn.hoidanit.springrestwithai.feature.role.dto;

import vn.hoidanit.springrestwithai.feature.permission.Permission;
import vn.hoidanit.springrestwithai.feature.role.Role;

import java.time.Instant;
import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        String description,
        List<PermissionSummary> permissions,
        Instant createdAt,
        Instant updatedAt) {

    public record PermissionSummary(Long id, String name, String apiPath, String method, String module) {

        public static PermissionSummary fromEntity(Permission permission) {
            return new PermissionSummary(
                    permission.getId(),
                    permission.getName(),
                    permission.getApiPath(),
                    permission.getMethod(),
                    permission.getModule());
        }
    }

    public static RoleResponse fromEntity(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream().map(PermissionSummary::fromEntity).toList(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }
}
