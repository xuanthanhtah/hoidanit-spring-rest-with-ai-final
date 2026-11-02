package vn.hoidanit.springrestwithai.feature.company;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanyControllerTest {

    private static final String BASE_URL = "/api/v1/companies";
    private static final long NON_EXISTENT_ID = 999_999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    private Company existing;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setName("TEST_COMPANY_EXISTING");
        company.setDescription("Existing test company");
        company.setAddress("Ho Chi Minh City");
        company.setLogo("/logos/test.png");
        existing = companyRepository.saveAndFlush(company);
    }

    private static String body(String name, String address) {
        return """
                {"name":"%s","description":"Test description","address":"%s","logo":"/logos/test.png"}
                """.formatted(name, address);
    }

    private static String updateBody(Long id, String name, String address) {
        return """
                {"id":%d,"name":"%s","description":"Test description","address":"%s","logo":"/logos/test.png"}
                """.formatted(id, name, address);
    }

    // ========== POST ==========

    @Test
    @DisplayName("POST should return 201 with created company")
    void createCompany_validRequest_returns201() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST_COMPANY_NEW", "Ha Noi")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Tạo công ty thành công"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("TEST_COMPANY_NEW"))
                .andExpect(jsonPath("$.data.address").value("Ha Noi"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST should return 400 when name is blank")
    void createCompany_invalidRequest_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "Ha Noi")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.details.length()").value(1));
    }

    @Test
    @DisplayName("POST should return 409 when company name already exists")
    void createCompany_duplicate_returns409() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(existing.getName(), "Ha Noi")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST should return 401 when no JWT is provided")
    void createCompany_noToken_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST_COMPANY_NEW", "Ha Noi")))
                .andExpect(status().isUnauthorized());
    }

    // ========== GET /{id} ==========

    @Test
    @DisplayName("GET /{id} should return 200 with company")
    void getCompanyById_found_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Lấy thông tin công ty thành công"))
                .andExpect(jsonPath("$.data.id").value(existing.getId()))
                .andExpect(jsonPath("$.data.name").value("TEST_COMPANY_EXISTING"))
                .andExpect(jsonPath("$.data.address").value("Ho Chi Minh City"));
    }

    @Test
    @DisplayName("GET /{id} should return 404 when company does not exist")
    void getCompanyById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("GET /{id} should return 401 when no JWT is provided")
    void getCompanyById_noToken_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ========== GET (list) ==========

    @Test
    @DisplayName("GET should return 200 with paginated companies")
    void getAllCompanies_withPagination_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL).with(jwt()).param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Lấy danh sách công ty thành công"))
                .andExpect(jsonPath("$.data.meta.page").value(1))
                .andExpect(jsonPath("$.data.meta.pageSize").value(5))
                .andExpect(jsonPath("$.data.meta.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.result").isArray());
    }

    // ========== PUT ==========

    @Test
    @DisplayName("PUT should return 200 with updated company")
    void updateCompany_validRequest_returns200() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(existing.getId(), "TEST_COMPANY_UPDATED", "Da Nang")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Cập nhật công ty thành công"))
                .andExpect(jsonPath("$.data.name").value("TEST_COMPANY_UPDATED"))
                .andExpect(jsonPath("$.data.address").value("Da Nang"));
    }

    @Test
    @DisplayName("PUT should return 400 when id is missing")
    void updateCompany_invalidRequest_returns400() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST_COMPANY_UPDATED", "Da Nang")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("PUT should return 404 when company does not exist")
    void updateCompany_notFound_returns404() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(NON_EXISTENT_ID, "TEST_COMPANY_UPDATED", "Da Nang")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @DisplayName("PUT should return 409 when renamed to another company's name")
    void updateCompany_duplicate_returns409() throws Exception {
        Company other = new Company();
        other.setName("TEST_COMPANY_OTHER");
        Company saved = companyRepository.saveAndFlush(other);

        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(saved.getId(), existing.getName(), "Da Nang")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("DELETE should return 200 and remove the company")
    void deleteCompany_found_returns200() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Xóa công ty thành công"));

        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE should return 404 when company does not exist")
    void deleteCompany_notFound_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(404));
    }
}
