package vn.hoidanit.springrestwithai.feature.role;

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

import vn.hoidanit.springrestwithai.feature.permission.Permission;
import vn.hoidanit.springrestwithai.feature.permission.PermissionRepository;

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
class RoleControllerTest {

    private static final String BASE_URL = "/api/v1/roles";
    private static final long NON_EXISTENT_ID = 999_999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private EntityManager entityManager;

    private Permission viewItems;
    private Permission createItem;
    private Role existing;

    @BeforeEach
    void setUp() {
        viewItems = savePermission("TEST_VIEW_ITEMS", "GET");
        createItem = savePermission("TEST_CREATE_ITEM", "POST");

        Role role = new Role();
        role.setName("TEST_ROLE");
        role.setDescription("Role for tests");
        role.setPermissions(new ArrayList<>(List.of(viewItems)));
        existing = roleRepository.saveAndFlush(role);
    }

    private Permission savePermission(String name, String method) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setApiPath("/api/v1/test-role-items");
        permission.setMethod(method);
        permission.setModule("TEST");
        return permissionRepository.saveAndFlush(permission);
    }

    private static String body(String name, String permissionIds) {
        return """
                {"name":"%s","description":"desc","permissionIds":%s}
                """.formatted(name, permissionIds);
    }

    private static String updateBody(Long id, String name, String permissionIds) {
        return """
                {"id":%d,"name":"%s","description":"updated","permissionIds":%s}
                """.formatted(id, name, permissionIds);
    }

    // ========== POST ==========

    @Test
    @DisplayName("POST should return 201 with created role and its permissions")
    void createRole_validRequest_returns201() throws Exception {
        String ids = "[%d,%d]".formatted(viewItems.getId(), createItem.getId());
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST_NEW_ROLE", ids)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Tạo vai trò thành công"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("TEST_NEW_ROLE"))
                .andExpect(jsonPath("$.data.permissions.length()").value(2))
                .andExpect(jsonPath("$.data.permissions[0].apiPath").value("/api/v1/test-role-items"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST should return 400 when name is blank")
    void createRole_invalidRequest_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("", "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.details.length()").value(1));
    }

    @Test
    @DisplayName("POST should return 409 when role name already exists")
    void createRole_duplicate_returns409() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(existing.getName(), "[]")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST should return 404 when a permission id does not exist")
    void createRole_permissionNotFound_returns404() throws Exception {
        mockMvc.perform(post(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST_NEW_ROLE", "[" + NON_EXISTENT_ID + "]")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @DisplayName("POST should return 401 when no JWT is provided")
    void createRole_noToken_returns401() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST_NEW_ROLE", "[]")))
                .andExpect(status().isUnauthorized());
    }

    // ========== GET /{id} ==========

    @Test
    @DisplayName("GET /{id} should return 200 with role and permissions")
    void getRoleById_found_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Lấy thông tin vai trò thành công"))
                .andExpect(jsonPath("$.data.id").value(existing.getId()))
                .andExpect(jsonPath("$.data.name").value("TEST_ROLE"))
                .andExpect(jsonPath("$.data.permissions.length()").value(1))
                .andExpect(jsonPath("$.data.permissions[0].name").value("TEST_VIEW_ITEMS"));
    }

    @Test
    @DisplayName("GET /{id} should return 404 when role does not exist")
    void getRoleById_notFound_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("GET /{id} should return 401 when no JWT is provided")
    void getRoleById_noToken_returns401() throws Exception {
        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ========== GET (list) ==========

    @Test
    @DisplayName("GET should return 200 with paginated roles")
    void getAllRoles_withPagination_returns200() throws Exception {
        mockMvc.perform(get(BASE_URL).with(jwt()).param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Lấy danh sách vai trò thành công"))
                .andExpect(jsonPath("$.data.meta.page").value(1))
                .andExpect(jsonPath("$.data.meta.pageSize").value(5))
                .andExpect(jsonPath("$.data.meta.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.result").isArray());
    }

    // ========== PUT ==========

    @Test
    @DisplayName("PUT should return 200 and replace the permission list")
    void updateRole_validRequest_returns200() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(existing.getId(), "TEST_ROLE_UPDATED", "[" + createItem.getId() + "]")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Cập nhật vai trò thành công"))
                .andExpect(jsonPath("$.data.name").value("TEST_ROLE_UPDATED"))
                .andExpect(jsonPath("$.data.permissions.length()").value(1))
                .andExpect(jsonPath("$.data.permissions[0].name").value("TEST_CREATE_ITEM"));
    }

    @Test
    @DisplayName("PUT should return 400 when id is missing")
    void updateRole_invalidRequest_returns400() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST_ROLE_UPDATED", "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("PUT should return 404 when role does not exist")
    void updateRole_notFound_returns404() throws Exception {
        mockMvc.perform(put(BASE_URL).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(NON_EXISTENT_ID, "TEST_ROLE_UPDATED", "[]")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("DELETE should return 200, remove the role and keep its permissions")
    void deleteRole_found_returns200() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Xóa vai trò thành công"));
        entityManager.flush();

        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isNotFound());
        assertTrue(permissionRepository.existsById(viewItems.getId()));
    }

    @Test
    @DisplayName("DELETE should return 404 when role does not exist")
    void deleteRole_notFound_returns404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTENT_ID).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // ========== RELATIONSHIP ==========

    @Test
    @DisplayName("Deleting a permission assigned to a role should detach it from the role")
    void deletePermission_assignedToRole_detachesFromRole() throws Exception {
        // Reload so permission.roles (inverse side) reflects the join row written in setUp
        entityManager.clear();

        mockMvc.perform(delete("/api/v1/permissions/{id}", viewItems.getId()).with(jwt()))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get(BASE_URL + "/{id}", existing.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permissions.length()").value(0));
    }
}
