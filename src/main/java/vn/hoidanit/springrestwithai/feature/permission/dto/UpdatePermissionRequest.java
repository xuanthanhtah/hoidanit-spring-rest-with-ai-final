package vn.hoidanit.springrestwithai.feature.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePermissionRequest(
        @NotNull(message = "Id không được để trống")
        Long id,

        @NotBlank(message = "Tên quyền hạn không được để trống")
        @Size(max = 100, message = "Tên quyền hạn không được quá 100 ký tự")
        String name,

        @NotBlank(message = "API path không được để trống")
        @Size(max = 255, message = "API path không được quá 255 ký tự")
        String apiPath,

        @NotBlank(message = "Method không được để trống")
        @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE", message = "Method phải là GET, POST, PUT, PATCH hoặc DELETE")
        String method,

        @NotBlank(message = "Module không được để trống")
        @Size(max = 100, message = "Module không được quá 100 ký tự")
        String module) {
}
