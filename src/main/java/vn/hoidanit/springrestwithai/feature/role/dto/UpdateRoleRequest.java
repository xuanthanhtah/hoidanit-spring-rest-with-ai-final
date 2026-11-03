package vn.hoidanit.springrestwithai.feature.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateRoleRequest(
        @NotNull(message = "Id không được để trống")
        Long id,

        @NotBlank(message = "Tên vai trò không được để trống")
        @Size(max = 100, message = "Tên vai trò không được quá 100 ký tự")
        String name,

        @Size(max = 255, message = "Mô tả không được quá 255 ký tự")
        String description,

        List<@NotNull(message = "Permission id không được null") Long> permissionIds) {
}
