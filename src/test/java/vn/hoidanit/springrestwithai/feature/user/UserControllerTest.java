package vn.hoidanit.springrestwithai.feature.user;

import jakarta.persistence.EntityManager;
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

import vn.hoidanit.springrestwithai.feature.company.Company;
import vn.hoidanit.springrestwithai.feature.company.CompanyRepository;
import vn.hoidanit.springrestwithai.feature.role.Role;
import vn.hoidanit.springrestwithai.feature.role.RoleRepository;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class UserControllerTest {

    private static final String BASE_URL = "/api/v1/users";
    private static final long NON_EXISTENT_ID = 999_999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    private Company company;
    private Company otherCompany;
    private Role hr;
    private Role staff;
    private User existing;

    @BeforeEach
    void setUp() {
        company = saveCompany("TEST_USER_COMPANY");
        otherCompany = saveCompany("TEST_USER_OTHER_COMPANY");
        hr = saveRole("TEST_USER_HR");
        staff = saveRole("TEST_USER_STAFF");

        User user = new User();
        user.setName("Existing User");
        user.setEmail("existing.user@test.com");
        user.setPassword("hashed");
        user.setCompany(company);
        user.setRoles(new ArrayList<>(List.of(hr)));
        existing = userRepository.saveAndFlush(user);
    }

    private Company saveCompany(String name) {
        Company entity = new Company();
        entity.setName(name);
        return companyRepository.saveAndFlush(entity);
    }

    private Role saveRole(String name) {
        Role entity = new Role();
        entity.setName(name);
        return roleRepository.saveAndFlush(entity);
    }

    private static String createBody(String email, String password, Object companyId, String roleIds) {
        return """
                {"name":"Tran Thi B","email":"%s","password":"%s","age":30,"gender":"FEMALE",
                 "address":"Ha Noi","companyId":%s,"roleIds":%s}
                """.formatted(email, password, companyId, roleIds);
    }

    private static String updateBody(Long id, Object companyId, String roleIds) {
        return """
                {"id":%s,"name":"Updated User","age":31,"gender":"MALE","address":"Da Nang",
                 "companyId":%s,"roleIds":%s}
                """.formatted(id, companyId, roleIds);
    }

    // ========== POST ==========

    @Test
    @DisplayName("POST should return 201 with created user, company and roles, without password")
    void createUser_validRequest_returns201() throws Exception {
        String roleIds = "[%d,%d]".formatted(hr.getId(), staff.getId());
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("new.user@test.com", "password123", company.getId(), roleIds)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Tạo người dùng thành công"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.email").value("new.user@test.com"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.company.id").value(company.getId()))
                .andExpect(jsonPath("$.data.company.name").value("TEST_USER_COMPANY"))
                .andExpect(jsonPath("$.data.roles.length()").value(2))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST should return 400 when email is invalid and password is too short")
    void createUser_invalidRequest_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("not-an-email", "short", null, "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    @DisplayName("POST should return 400 when gender is not a valid enum value")
    void createUser_invalidGender_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","email":"g@test.com","password":"password123","gender":"UNKNOWN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("POST should return 409 when email already exists")
    void createUser_duplicateEmail_returns409() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(existing.getEmail(), "password123", null, "[]")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    @Test
    @DisplayName("POST should return 404 when company does not exist")
    void createUser_companyNotFound_returns404() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("new.user@test.com", "password123", NON_EXISTENT_ID, "[]")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @DisplayName("POST should return 404 when a role id does not exist")
    void createUser_roleNotFound_returns404() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("new.user@test.com", "password123", null, "[" + NON_EXISTENT_ID + "]")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @DisplayName("POST should return 401 when no JWT is provided")
    void createUser_noToken_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("new.user@test.com", "password123", null, "[]")))
                .andExpect(status().isUnauthorized());
    }

    // ========== GET ==========

    @Test
    @DisplayName("GET /{id} should return 200 with user, company and roles")
    void getUserById_found_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Lấy thông tin người dùng thành công"))
                .andExpect(jsonPath("$.data.email").value("existing.user@test.com"))
                .andExpect(jsonPath("$.data.company.name").value("TEST_USER_COMPANY"))
                .andExpect(jsonPath("$.data.roles[0].name").value("TEST_USER_HR"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /{id} should return 404 when user does not exist")
    void getUserById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("GET /{id} should return 401 when no JWT is provided")
    void getUserById_noToken_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET should return 200 with paginated users")
    void getAllUsers_withPagination_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL).with(jwt()).param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Lấy danh sách người dùng thành công"))
                .andExpect(jsonPath("$.data.meta.page").value(1))
                .andExpect(jsonPath("$.data.meta.pageSize").value(5))
                .andExpect(jsonPath("$.data.meta.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.result").isArray());
    }

    // ========== PUT ==========

    @Test
    @DisplayName("PUT should return 200, swap company and roles, and keep email")
    void updateUser_validRequest_returns200() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(existing.getId(), otherCompany.getId(), "[" + staff.getId() + "]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Cập nhật người dùng thành công"))
                .andExpect(jsonPath("$.data.name").value("Updated User"))
                .andExpect(jsonPath("$.data.email").value("existing.user@test.com"))
                .andExpect(jsonPath("$.data.company.name").value("TEST_USER_OTHER_COMPANY"))
                .andExpect(jsonPath("$.data.roles.length()").value(1))
                .andExpect(jsonPath("$.data.roles[0].name").value("TEST_USER_STAFF"));
    }

    @Test
    @DisplayName("PUT should return 400 when id is missing")
    void updateUser_invalidRequest_returns400() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(null, null, "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("PUT should return 404 when user does not exist")
    void updateUser_notFound_returns404() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(NON_EXISTENT_ID, null, "[]")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("DELETE should return 200, remove the user and keep its company and roles")
    void deleteUser_found_returns200() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Xóa người dùng thành công"));
        entityManager.flush();

        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isNotFound());
        assertTrue(companyRepository.existsById(company.getId()));
        assertTrue(roleRepository.existsById(hr.getId()));
    }

    @Test
    @DisplayName("DELETE should return 404 when user does not exist")
    void deleteUser_notFound_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // ========== RELATIONSHIP ==========

    @Test
    @DisplayName("Deleting a company should keep its users with no company")
    void deleteCompany_withUsers_detachesUsers() throws Exception {
        // Reload so company.users (inverse side) reflects the FK written in setUp
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/companies/{id}", company.getId()).with(jwt()))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.company").doesNotExist());
    }

    @Test
    @DisplayName("Deleting a role assigned to a user should detach it from the user")
    void deleteRole_assignedToUser_detachesFromUser() throws Exception {
        // Reload so role.users (inverse side) reflects the join row written in setUp
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/roles/{id}", hr.getId()).with(jwt()))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(0));
    }
}
