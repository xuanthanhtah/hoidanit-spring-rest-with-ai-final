package vn.hoidanit.springrestwithai.feature.company;

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
import vn.hoidanit.springrestwithai.feature.company.dto.CompanyResponse;
import vn.hoidanit.springrestwithai.feature.company.dto.CreateCompanyRequest;
import vn.hoidanit.springrestwithai.feature.company.dto.UpdateCompanyRequest;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CompanyResponse>>> getAllCompanies(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        PageResponse<CompanyResponse> companies = this.companyService.getAllCompanies(pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách công ty thành công", companies));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyById(@PathVariable Long id) {
        CompanyResponse company = this.companyService.getCompanyById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin công ty thành công", company));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @Valid @RequestBody CreateCompanyRequest request) {
        CompanyResponse created = this.companyService.createCompany(request);
        URI location = URI.create("/api/v1/companies/" + created.id());
        return ResponseEntity.created(location)
                .body(ApiResponse.created("Tạo công ty thành công", created));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @Valid @RequestBody UpdateCompanyRequest request) {
        CompanyResponse updated = this.companyService.updateCompany(request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật công ty thành công", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable Long id) {
        this.companyService.deleteCompany(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa công ty thành công", null));
    }
}
