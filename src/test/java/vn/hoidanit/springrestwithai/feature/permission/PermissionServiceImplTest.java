package vn.hoidanit.springrestwithai.feature.permission;

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
import vn.hoidanit.springrestwithai.feature.permission.dto.CreatePermissionRequest;
import vn.hoidanit.springrestwithai.feature.permission.dto.PermissionResponse;
import vn.hoidanit.springrestwithai.feature.permission.dto.UpdatePermissionRequest;
import vn.hoidanit.springrestwithai.feature.role.Role;

import java.util.ArrayList;
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
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private Permission permission;

    @BeforeEach
    void setUp() {
        permission = new Permission();
        permission.setId(1L);
        permission.setName("CREATE_USER");
        permission.setApiPath("/api/v1/users");
        permission.setMethod("POST");
        permission.setModule("USER");
    }

    // ========== CREATE ==========

    @Test
    @DisplayName("Should create permission when apiPath + method is unique")
    void createPermission_validRequest_returnsPermissionResponse() {
        CreatePermissionRequest request = new CreatePermissionRequest("CREATE_USER", "/api/v1/users", "POST", "USER");
        when(permissionRepository.existsByApiPathAndMethod("/api/v1/users", "POST")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenReturn(permission);

        PermissionResponse result = permissionService.createPermission(request);

        assertEquals(1L, result.id());
        assertEquals("CREATE_USER", result.name());
        assertEquals("POST", result.method());
        verify(permissionRepository, times(1)).save(any(Permission.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when apiPath + method already exists")
    void createPermission_duplicateApiPathAndMethod_throwsDuplicateResourceException() {
        CreatePermissionRequest request = new CreatePermissionRequest("CREATE_USER", "/api/v1/users", "POST", "USER");
        when(permissionRepository.existsByApiPathAndMethod("/api/v1/users", "POST")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> permissionService.createPermission(request));

        assertTrue(ex.getMessage().contains("POST /api/v1/users"));
        verify(permissionRepository, never()).save(any(Permission.class));
    }

    // ========== GET BY ID ==========

    @Test
    @DisplayName("Should return permission when id exists")
    void getPermissionById_found_returnsPermissionResponse() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));

        PermissionResponse result = permissionService.getPermissionById(1L);

        assertEquals(1L, result.id());
        assertEquals("CREATE_USER", result.name());
        assertEquals("/api/v1/users", result.apiPath());
        assertEquals("POST", result.method());
        assertEquals("USER", result.module());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when id does not exist")
    void getPermissionById_notFound_throwsResourceNotFoundException() {
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> permissionService.getPermissionById(99L));

        assertTrue(ex.getMessage().contains("99"));
    }

    // ========== GET ALL ==========

    @Test
    @DisplayName("Should return paginated permissions with 1-based meta")
    void getAllPermissions_hasData_returnsPageResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Permission> page = new PageImpl<>(List.of(permission), pageable, 1);
        when(permissionRepository.findAll(pageable)).thenReturn(page);

        PageResponse<PermissionResponse> result = permissionService.getAllPermissions(pageable);

        assertEquals(1, result.meta().page());
        assertEquals(10, result.meta().pageSize());
        assertEquals(1, result.meta().pages());
        assertEquals(1L, result.meta().total());
        assertEquals(1, result.result().size());
        assertEquals("CREATE_USER", result.result().get(0).name());
    }

    @Test
    @DisplayName("Should return empty list (not null) when there are no permissions")
    void getAllPermissions_noData_returnsEmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        when(permissionRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        PageResponse<PermissionResponse> result = permissionService.getAllPermissions(pageable);

        assertNotNull(result.result());
        assertTrue(result.result().isEmpty());
        assertEquals(0L, result.meta().total());
    }

    // ========== UPDATE ==========

    @Test
    @DisplayName("Should update permission fields when id exists and combo is unique")
    void updatePermission_validRequest_returnsUpdatedPermission() {
        UpdatePermissionRequest request = new UpdatePermissionRequest(
                1L, "UPDATE_USER", "/api/v1/users", "PUT", "USER");
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(permissionRepository.existsByApiPathAndMethodAndIdNot("/api/v1/users", "PUT", 1L)).thenReturn(false);
        when(permissionRepository.save(permission)).thenReturn(permission);

        PermissionResponse result = permissionService.updatePermission(request);

        assertEquals("UPDATE_USER", result.name());
        assertEquals("PUT", result.method());
        verify(permissionRepository, times(1)).save(permission);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent permission")
    void updatePermission_notFound_throwsResourceNotFoundException() {
        UpdatePermissionRequest request = new UpdatePermissionRequest(
                99L, "UPDATE_USER", "/api/v1/users", "PUT", "USER");
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> permissionService.updatePermission(request));
        verify(permissionRepository, never()).save(any(Permission.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when apiPath + method belongs to another permission")
    void updatePermission_duplicateApiPathAndMethod_throwsDuplicateResourceException() {
        UpdatePermissionRequest request = new UpdatePermissionRequest(
                1L, "VIEW_USERS", "/api/v1/users", "GET", "USER");
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(permissionRepository.existsByApiPathAndMethodAndIdNot("/api/v1/users", "GET", 1L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> permissionService.updatePermission(request));
        verify(permissionRepository, never()).save(any(Permission.class));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("Should delete permission when id exists")
    void deletePermission_found_deletesPermission() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));

        permissionService.deletePermission(1L);

        verify(permissionRepository, times(1)).delete(permission);
    }

    @Test
    @DisplayName("Should detach permission from all roles before deleting it")
    void deletePermission_assignedToRoles_detachesFromRoles() {
        Role role = new Role();
        role.setPermissions(new ArrayList<>(List.of(permission)));
        permission.setRoles(new ArrayList<>(List.of(role)));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));

        permissionService.deletePermission(1L);

        assertTrue(role.getPermissions().isEmpty());
        assertTrue(permission.getRoles().isEmpty());
        verify(permissionRepository, times(1)).delete(permission);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent permission")
    void deletePermission_notFound_throwsResourceNotFoundException() {
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> permissionService.deletePermission(99L));
        verify(permissionRepository, never()).delete(any(Permission.class));
    }
}
