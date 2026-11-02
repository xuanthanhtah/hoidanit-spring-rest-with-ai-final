package vn.hoidanit.springrestwithai.feature.company.dto;

import vn.hoidanit.springrestwithai.feature.company.Company;

import java.time.Instant;

public record CompanyResponse(
        Long id,
        String name,
        String description,
        String address,
        String logo,
        Instant createdAt,
        Instant updatedAt) {

    public static CompanyResponse fromEntity(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getDescription(),
                company.getAddress(),
                company.getLogo(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }
}
