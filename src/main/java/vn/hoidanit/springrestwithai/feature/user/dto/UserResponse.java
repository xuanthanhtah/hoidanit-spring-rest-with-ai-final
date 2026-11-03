package vn.hoidanit.springrestwithai.feature.user.dto;

import vn.hoidanit.springrestwithai.feature.company.Company;
import vn.hoidanit.springrestwithai.feature.role.Role;
import vn.hoidanit.springrestwithai.feature.user.User;
import vn.hoidanit.springrestwithai.feature.user.User.GenderEnum;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String name,
        String email,
        Integer age,
        GenderEnum gender,
        String address,
        String avatar,
        CompanySummary company,
        List<RoleSummary> roles,
        Instant createdAt,
        Instant updatedAt) {

    public record CompanySummary(Long id, String name) {

        public static CompanySummary fromEntity(Company company) {
            return company == null ? null : new CompanySummary(company.getId(), company.getName());
        }
    }

    public record RoleSummary(Long id, String name) {

        public static RoleSummary fromEntity(Role role) {
            return new RoleSummary(role.getId(), role.getName());
        }
    }

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getGender(),
                user.getAddress(),
                user.getAvatar(),
                CompanySummary.fromEntity(user.getCompany()),
                user.getRoles().stream().map(RoleSummary::fromEntity).toList(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
