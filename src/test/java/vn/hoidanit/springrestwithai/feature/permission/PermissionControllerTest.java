package vn.hoidanit.springrestwithai.feature.permission;

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
class PermissionControllerTest {

    private static final String BASE_URL = "/api/v1/permissions";
    private static final long NON_EXISTENT_ID = 999_999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PermissionRepository permissionRepository;

    private Permission existing;

    @BeforeEach
    void setUp() {
        Permission permission = new Permission();
        permission.setName("TEST_VIEW_ITEMS");
        permission.setApiPath("/api/v1/test-items");
        permission.setMethod("GET");
        permission.setModule("TEST");
        existing = permissionRepository.saveAndFlush(permission);
    }

    private static String body(String name, String apiPath, String method, String module) {
        return """
                {"name":"%s","apiPath":"%s","method":"%s","module":"%s"}
                """.formatted(name, apiPath, method, module);
    }

    private static String updateBody(Long id, String name, String apiPath, String method, String module) {
        return """
                {"id":%d,"name":"%s","apiPath":"%s","method":"%s","module":"%s"}
                """.formatted(id, name, apiPath, method, module);
    }

    // ========== POST ==========

    @Test
    @DisplayName("POST should return 201 with created permission")
    void createPermission_validRequest_returns201() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST_CREATE_ITEM", "/api/v1/test-items", "POST", "TEST")))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Tạo quyền hạn thành công"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("TEST_CREATE_ITEM"))
                .andExpect(jsonPath("$.data.method").value("POST"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST should return 400 when fields are blank or method is invalid")
    void createPermission_invalidRequest_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "/api/v1/test-items", "FETCH", "TEST")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    @DisplayName("POST should return 409 when apiPath + method already exists")
    void createPermission_duplicate_returns409() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ANOTHER_NAME", existing.getApiPath(), existing.getMethod(), "TEST")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST should return 401 when no JWT is provided")
    void createPermission_noToken_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST_CREATE_ITEM", "/api/v1/test-items", "POST", "TEST")))
                .andExpect(status().isUnauthorized());
    }

    // ========== GET /{id} ==========

    @Test
    @DisplayName("GET /{id} should return 200 with permission")
    void getPermissionById_found_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Lấy thông tin quyền hạn thành công"))
                .andExpect(jsonPath("$.data.id").value(existing.getId()))
                .andExpect(jsonPath("$.data.apiPath").value("/api/v1/test-items"))
                .andExpect(jsonPath("$.data.module").value("TEST"));
    }

    @Test
    @DisplayName("GET /{id} should return 404 when permission does not exist")
    void getPermissionById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("GET /{id} should return 401 when no JWT is provided")
    void getPermissionById_noToken_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ========== GET (list) ==========

    @Test
    @DisplayName("GET should return 200 with paginated permissions")
    void getAllPermissions_withPagination_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL).with(jwt()).param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Lấy danh sách quyền hạn thành công"))
                .andExpect(jsonPath("$.data.meta.page").value(1))
                .andExpect(jsonPath("$.data.meta.pageSize").value(5))
                .andExpect(jsonPath("$.data.meta.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.result").isArray());
    }

    // ========== PUT ==========

    @Test
    @DisplayName("PUT should return 200 with updated permission")
    void updatePermission_validRequest_returns200() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(existing.getId(), "TEST_UPDATED", "/api/v1/test-items", "PUT", "TEST")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Cập nhật quyền hạn thành công"))
                .andExpect(jsonPath("$.data.name").value("TEST_UPDATED"))
                .andExpect(jsonPath("$.data.method").value("PUT"));
    }

    @Test
    @DisplayName("PUT should return 400 when id is missing")
    void updatePermission_invalidRequest_returns400() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST_UPDATED", "/api/v1/test-items", "PUT", "TEST")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("PUT should return 404 when permission does not exist")
    void updatePermission_notFound_returns404() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(NON_EXISTENT_ID, "TEST_UPDATED", "/api/v1/test-items", "PUT", "TEST")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @DisplayName("PUT should return 409 when changed to an existing apiPath + method")
    void updatePermission_duplicate_returns409() throws Exception {
        Permission other = new Permission();
        other.setName("TEST_DELETE_ITEM");
        other.setApiPath("/api/v1/test-items");
        other.setMethod("DELETE");
        other.setModule("TEST");
        Permission saved = permissionRepository.saveAndFlush(other);

        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(saved.getId(), "TEST_DELETE_ITEM", "/api/v1/test-items", "GET", "TEST")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("DELETE should return 200 and remove the permission")
    void deletePermission_found_returns200() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Xóa quyền hạn thành công"));

        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE should return 404 when permission does not exist")
    void deletePermission_notFound_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(404));
    }
}
