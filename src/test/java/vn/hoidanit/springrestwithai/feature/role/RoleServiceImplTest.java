package vn.hoidanit.springrestwithai.feature.role;

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
import vn.hoidanit.springrestwithai.feature.permission.Permission;
import vn.hoidanit.springrestwithai.feature.permission.PermissionRepository;
import vn.hoidanit.springrestwithai.feature.role.dto.CreateRoleRequest;
import vn.hoidanit.springrestwithai.feature.role.dto.RoleResponse;
import vn.hoidanit.springrestwithai.feature.role.dto.UpdateRoleRequest;
import vn.hoidanit.springrestwithai.feature.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role;
    private Permission createUser;
    private Permission viewUsers;

    @BeforeEach
    void setUp() {
        createUser = permission(1L, "CREATE_USER", "POST");
        viewUsers = permission(4L, "VIEW_USERS", "GET");

        role = new Role();
        role.setId(1L);
        role.setName("HR");
        role.setDescription("Human resources");
        role.setPermissions(new ArrayList<>(List.of(createUser)));
    }

    private static Permission permission(Long id, String name, String method) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setName(name);
        permission.setApiPath("/api/v1/users");
        permission.setMethod(method);
        permission.setModule("USER");
        return permission;
    }

    // ========== CREATE ==========

    @Test
    @DisplayName("Should create role with the requested permissions when name is unique")
    void createRole_validRequest_returnsRoleWithPermissions() {
        CreateRoleRequest request = new CreateRoleRequest("HR", "Human resources", List.of(1L, 4L));
        when(roleRepository.existsByName("HR")).thenReturn(false);
        when(permissionRepository.findAllById(Set.of(1L, 4L))).thenReturn(List.of(createUser, viewUsers));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse result = roleService.createRole(request);

        assertEquals("HR", result.name());
        assertEquals(2, result.permissions().size());
        assertEquals("CREATE_USER", result.permissions().get(0).name());
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("Should create role with empty permissions when permissionIds is null")
    void createRole_nullPermissionIds_returnsRoleWithoutPermissions() {
        CreateRoleRequest request = new CreateRoleRequest("USER", null, null);
        when(roleRepository.existsByName("USER")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse result = roleService.createRole(request);

        assertNotNull(result.permissions());
        assertTrue(result.permissions().isEmpty());
        verify(permissionRepository, never()).findAllById(anyCollection());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when role name already exists")
    void createRole_duplicateName_throwsDuplicateResourceException() {
        CreateRoleRequest request = new CreateRoleRequest("HR", null, List.of(1L));
        when(roleRepository.existsByName("HR")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> roleService.createRole(request));

        assertTrue(ex.getMessage().contains("HR"));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException listing missing ids when a permission does not exist")
    void createRole_permissionNotFound_throwsResourceNotFoundException() {
        CreateRoleRequest request = new CreateRoleRequest("HR", null, List.of(1L, 99L));
        when(roleRepository.existsByName("HR")).thenReturn(false);
        when(permissionRepository.findAllById(Set.of(1L, 99L))).thenReturn(List.of(createUser));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> roleService.createRole(request));

        assertTrue(ex.getMessage().contains("99"));
        verify(roleRepository, never()).save(any(Role.class));
    }

    // ========== GET BY ID ==========

    @Test
    @DisplayName("Should return role with its permissions when id exists")
    void getRoleById_found_returnsRoleResponse() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        RoleResponse result = roleService.getRoleById(1L);

        assertEquals(1L, result.id());
        assertEquals("HR", result.name());
        assertEquals("Human resources", result.description());
        assertEquals(1, result.permissions().size());
        assertEquals("/api/v1/users", result.permissions().get(0).apiPath());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when id does not exist")
    void getRoleById_notFound_throwsResourceNotFoundException() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> roleService.getRoleById(99L));

        assertTrue(ex.getMessage().contains("99"));
    }

    // ========== GET ALL ==========

    @Test
    @DisplayName("Should return paginated roles with 1-based meta")
    void getAllRoles_hasData_returnsPageResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Role> page = new PageImpl<>(List.of(role), pageable, 1);
        when(roleRepository.findAll(pageable)).thenReturn(page);

        PageResponse<RoleResponse> result = roleService.getAllRoles(pageable);

        assertEquals(1, result.meta().page());
        assertEquals(10, result.meta().pageSize());
        assertEquals(1L, result.meta().total());
        assertEquals("HR", result.result().get(0).name());
        assertEquals(1, result.result().get(0).permissions().size());
    }

    @Test
    @DisplayName("Should return empty list (not null) when there are no roles")
    void getAllRoles_noData_returnsEmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        when(roleRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        PageResponse<RoleResponse> result = roleService.getAllRoles(pageable);

        assertNotNull(result.result());
        assertTrue(result.result().isEmpty());
        assertEquals(0L, result.meta().total());
    }

    // ========== UPDATE ==========

    @Test
    @DisplayName("Should replace the whole permission list (not additive) when updating")
    void updateRole_validRequest_replacesPermissions() {
        UpdateRoleRequest request = new UpdateRoleRequest(1L, "HR_UPDATED", "Updated", List.of(4L));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.existsByNameAndIdNot("HR_UPDATED", 1L)).thenReturn(false);
        when(permissionRepository.findAllById(Set.of(4L))).thenReturn(List.of(viewUsers));
        when(roleRepository.save(role)).thenReturn(role);

        RoleResponse result = roleService.updateRole(request);

        assertEquals("HR_UPDATED", result.name());
        assertEquals(1, result.permissions().size());
        assertEquals("VIEW_USERS", result.permissions().get(0).name());
        verify(roleRepository, times(1)).save(role);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent role")
    void updateRole_notFound_throwsResourceNotFoundException() {
        UpdateRoleRequest request = new UpdateRoleRequest(99L, "HR", null, List.of());
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.updateRole(request));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when name belongs to another role")
    void updateRole_duplicateName_throwsDuplicateResourceException() {
        UpdateRoleRequest request = new UpdateRoleRequest(1L, "ADMIN", null, List.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.existsByNameAndIdNot("ADMIN", 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> roleService.updateRole(request));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating with a non-existent permission")
    void updateRole_permissionNotFound_throwsResourceNotFoundException() {
        UpdateRoleRequest request = new UpdateRoleRequest(1L, "HR", null, List.of(99L));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.existsByNameAndIdNot("HR", 1L)).thenReturn(false);
        when(permissionRepository.findAllById(Set.of(99L))).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> roleService.updateRole(request));
        verify(roleRepository, never()).save(any(Role.class));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("Should delete role when id exists")
    void deleteRole_found_deletesRole() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        roleService.deleteRole(1L);

        verify(roleRepository, times(1)).delete(role);
    }

    @Test
    @DisplayName("Should detach role from its users before deleting")
    void deleteRole_assignedToUsers_detachesUsers() {
        User user = new User();
        user.setRoles(new ArrayList<>(List.of(role)));
        role.getUsers().add(user);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        roleService.deleteRole(1L);

        assertTrue(user.getRoles().isEmpty());
        assertTrue(role.getUsers().isEmpty());
        verify(roleRepository, times(1)).delete(role);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent role")
    void deleteRole_notFound_throwsResourceNotFoundException() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.deleteRole(99L));
        verify(roleRepository, never()).delete(any(Role.class));
    }
}
