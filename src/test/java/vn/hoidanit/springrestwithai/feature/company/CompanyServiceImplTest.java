package vn.hoidanit.springrestwithai.feature.company;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import vn.hoidanit.springrestwithai.dto.PageResponse;
import vn.hoidanit.springrestwithai.exception.DuplicateResourceException;
import vn.hoidanit.springrestwithai.exception.ResourceNotFoundException;
import vn.hoidanit.springrestwithai.feature.company.dto.CompanyResponse;
import vn.hoidanit.springrestwithai.feature.company.dto.CreateCompanyRequest;
import vn.hoidanit.springrestwithai.feature.company.dto.UpdateCompanyRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("HoiDanIT");
        company.setDescription("Education platform");
        company.setAddress("Ho Chi Minh City");
        company.setLogo("/logos/hoidanit.png");
    }

    // ========== CREATE ==========

    @Test
    @DisplayName("Should create company when name is unique")
    void createCompany_validRequest_returnsCompanyResponse() {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "HoiDanIT", "Education platform", "Ho Chi Minh City", "/logos/hoidanit.png");
        when(companyRepository.existsByName("HoiDanIT")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        CompanyResponse result = companyService.createCompany(request);

        assertEquals(1L, result.id());
        assertEquals("HoiDanIT", result.name());
        assertEquals("Ho Chi Minh City", result.address());
        verify(companyRepository, times(1)).save(any(Company.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when company name already exists")
    void createCompany_duplicateName_throwsDuplicateResourceException() {
        CreateCompanyRequest request = new CreateCompanyRequest("HoiDanIT", null, null, null);
        when(companyRepository.existsByName("HoiDanIT")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> companyService.createCompany(request));

        assertTrue(ex.getMessage().contains("HoiDanIT"));
        verify(companyRepository, never()).save(any(Company.class));
    }

    // ========== GET BY ID ==========

    @Test
    @DisplayName("Should return company when id exists")
    void getCompanyById_found_returnsCompanyResponse() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        CompanyResponse result = companyService.getCompanyById(1L);

        assertEquals(1L, result.id());
        assertEquals("HoiDanIT", result.name());
        assertEquals("Education platform", result.description());
        assertEquals("Ho Chi Minh City", result.address());
        assertEquals("/logos/hoidanit.png", result.logo());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when id does not exist")
    void getCompanyById_notFound_throwsResourceNotFoundException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> companyService.getCompanyById(99L));

        assertTrue(ex.getMessage().contains("99"));
    }

    // ========== GET ALL ==========

    @Test
    @DisplayName("Should return paginated companies with 1-based meta")
    void getAllCompanies_hasData_returnsPageResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Company> page = new PageImpl<>(List.of(company), pageable, 1);
        when(companyRepository.findAll(pageable)).thenReturn(page);

        PageResponse<CompanyResponse> result = companyService.getAllCompanies(pageable);

        assertEquals(1, result.meta().page());
        assertEquals(10, result.meta().pageSize());
        assertEquals(1, result.meta().pages());
        assertEquals(1L, result.meta().total());
        assertEquals(1, result.result().size());
        assertEquals("HoiDanIT", result.result().get(0).name());
    }

    @Test
    @DisplayName("Should return empty list (not null) when there are no companies")
    void getAllCompanies_noData_returnsEmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        when(companyRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        PageResponse<CompanyResponse> result = companyService.getAllCompanies(pageable);

        assertNotNull(result.result());
        assertTrue(result.result().isEmpty());
        assertEquals(0L, result.meta().total());
    }

    // ========== UPDATE ==========

    @Test
    @DisplayName("Should update company fields when id exists and name is unique")
    void updateCompany_validRequest_returnsUpdatedCompany() {
        UpdateCompanyRequest request = new UpdateCompanyRequest(
                1L, "HoiDanIT Updated", "Tech services", "Da Nang", "/logos/new.png");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameAndIdNot("HoiDanIT Updated", 1L)).thenReturn(false);
        when(companyRepository.save(company)).thenReturn(company);

        CompanyResponse result = companyService.updateCompany(request);

        assertEquals("HoiDanIT Updated", result.name());
        assertEquals("Da Nang", result.address());
        assertEquals("/logos/new.png", result.logo());
        verify(companyRepository, times(1)).save(company);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent company")
    void updateCompany_notFound_throwsResourceNotFoundException() {
        UpdateCompanyRequest request = new UpdateCompanyRequest(99L, "Any", null, null, null);
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> companyService.updateCompany(request));
        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when name belongs to another company")
    void updateCompany_duplicateName_throwsDuplicateResourceException() {
        UpdateCompanyRequest request = new UpdateCompanyRequest(1L, "FPT Software", null, null, null);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.existsByNameAndIdNot("FPT Software", 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> companyService.updateCompany(request));
        verify(companyRepository, never()).save(any(Company.class));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("Should delete company when id exists")
    void deleteCompany_found_deletesCompany() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        companyService.deleteCompany(1L);

        verify(companyRepository, times(1)).delete(company);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent company")
    void deleteCompany_notFound_throwsResourceNotFoundException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> companyService.deleteCompany(99L));
        verify(companyRepository, never()).delete(any(Company.class));
    }
}
