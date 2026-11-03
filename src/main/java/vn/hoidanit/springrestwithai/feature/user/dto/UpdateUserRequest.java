package vn.hoidanit.springrestwithai.feature.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import vn.hoidanit.springrestwithai.feature.user.User.GenderEnum;

import java.util.List;

// email and password are intentionally not updatable through this request
public record UpdateUserRequest(
        @NotNull(message = "Id không được để trống")
        Long id,

        @NotBlank(message = "Tên không được để trống")
        @Size(max = 100, message = "Tên không được quá 100 ký tự")
        String name,

        @Min(value = 1, message = "Tuổi phải lớn hơn 0")
        @Max(value = 150, message = "Tuổi không hợp lệ")
        Integer age,

        GenderEnum gender,

        @Size(max = 255, message = "Địa chỉ không được quá 255 ký tự")
        String address,

        @Size(max = 255, message = "Avatar không được quá 255 ký tự")
        String avatar,

        Long companyId,

        List<@NotNull(message = "Role id không được null") Long> roleIds) {
}
