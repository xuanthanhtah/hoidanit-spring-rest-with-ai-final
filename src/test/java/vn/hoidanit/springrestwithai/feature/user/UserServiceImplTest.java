package vn.hoidanit.springrestwithai.feature.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.hoidanit.springrestwithai.dto.PageResponse;
import vn.hoidanit.springrestwithai.exception.DuplicateResourceException;
import vn.hoidanit.springrestwithai.exception.ResourceNotFoundException;
import vn.hoidanit.springrestwithai.feature.company.Company;
import vn.hoidanit.springrestwithai.feature.company.CompanyRepository;
import vn.hoidanit.springrestwithai.feature.role.Role;
import vn.hoidanit.springrestwithai.feature.role.RoleRepository;
import vn.hoidanit.springrestwithai.feature.user.User.GenderEnum;
import vn.hoidanit.springrestwithai.feature.user.dto.CreateUserRequest;
import vn.hoidanit.springrestwithai.feature.user.dto.UpdateUserRequest;
import vn.hoidanit.springrestwithai.feature.user.dto.UserResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private Company company;
    private Role hr;
    private Role staff;

    @BeforeEach
    void setUp() {
        company = company(1L, "HoiDanIT");
        hr = role(3L, "HR");
        staff = role(5L, "USER");

        user = new User();
        user.setId(1L);
        user.setName("Nguyen Van A");
        user.setEmail("a@example.com");
        user.setPassword("hashed");
        user.setCompany(company);
        user.setRoles(new ArrayList<>(List.of(hr)));
    }

    private static Company company(Long id, String name) {
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        return company;
    }

    private static Role role(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }

    private static CreateUserRequest createRequest(Long companyId, List<Long> roleIds) {
        return new CreateUserRequest("Tran Thi B", "b@example.com", "password123", 30,
                GenderEnum.FEMALE, "Ha Noi", null, companyId, roleIds);
    }

    private static UpdateUserRequest updateRequest(Long id, Long companyId, List<Long> roleIds) {
        return new UpdateUserRequest(id, "Updated", 31, GenderEnum.MALE, "Da Nang", null, companyId, roleIds);
    }

    // ========== CREATE ==========

    @Test
    @DisplayName("Should create user with encoded password, company and roles")
    void createUser_validRequest_returnsUserResponse() {
        when(userRepository.existsByEmail("b@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(roleRepository.findAllById(anyCollection())).thenReturn(List.of(hr, staff));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.createUser(createRequest(1L, List.of(3L, 5L)));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hashed-password", captor.getValue().getPassword());
        assertEquals("b@example.com", result.email());
        assertEquals(GenderEnum.FEMALE, result.gender());
        assertEquals("HoiDanIT", result.company().name());
        assertEquals(2, result.roles().size());
    }

    @Test
    @DisplayName("Should create user without company and roles when ids are null")
    void createUser_nullCompanyAndRoles_returnsUserWithoutRelations() {
        when(userRepository.existsByEmail("b@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.createUser(createRequest(null, null));

        assertNull(result.company());
        assertTrue(result.roles().isEmpty());
        verify(companyRepository, never()).findById(any());
        verify(roleRepository, never()).findAllById(anyCollection());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void createUser_duplicateEmail_throwsDuplicateResourceException() {
        when(userRepository.existsByEmail("b@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> userService.createUser(createRequest(null, null)));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when company does not exist")
    void createUser_companyNotFound_throwsResourceNotFoundException() {
        when(userRepository.existsByEmail("b@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.createUser(createRequest(99L, null)));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException listing missing role ids")
    void createUser_roleNotFound_throwsResourceNotFoundException() {
        when(userRepository.existsByEmail("b@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(roleRepository.findAllById(anyCollection())).thenReturn(List.of(hr));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.createUser(createRequest(null, List.of(3L, 99L))));
        assertEquals(Set.of(99L), ex.getFieldValue());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should de-duplicate role ids before lookup")
    void createUser_duplicateRoleIds_deduplicates() {
        when(userRepository.existsByEmail("b@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(roleRepository.findAllById(Set.of(3L))).thenReturn(List.of(hr));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.createUser(createRequest(null, List.of(3L, 3L)));

        assertEquals(1, result.roles().size());
    }

    // ========== READ ==========

    @Test
    @DisplayName("Should return user with company and roles when found")
    void getUserById_found_returnsUserResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserById(1L);

        assertEquals("a@example.com", result.email());
        assertEquals(1L, result.company().id());
        assertEquals("HR", result.roles().get(0).name());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void getUserById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    @DisplayName("Should return paginated users with 1-based meta")
    void getAllUsers_withPageable_returnsPageResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(page);

        PageResponse<UserResponse> result = userService.getAllUsers(pageable);

        assertEquals(1, result.meta().page());
        assertEquals(1, result.meta().total());
        assertEquals("a@example.com", result.result().get(0).email());
    }

    // ========== UPDATE ==========

    @Test
    @DisplayName("Should update fields and replace company and roles, keeping email and password")
    void updateUser_validRequest_replacesRelations() {
        Company fpt = company(2L, "FPT Software");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyRepository.findById(2L)).thenReturn(Optional.of(fpt));
        when(roleRepository.findAllById(anyCollection())).thenReturn(List.of(staff));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.updateUser(updateRequest(1L, 2L, List.of(5L)));

        assertEquals("Updated", result.name());
        assertEquals("a@example.com", result.email());
        assertEquals("hashed", user.getPassword());
        assertEquals("FPT Software", result.company().name());
        assertEquals(List.of(5L), result.roles().stream().map(UserResponse.RoleSummary::id).toList());
    }

    @Test
    @DisplayName("Should clear company and roles when ids are null")
    void updateUser_nullCompanyAndRoles_clearsRelations() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.updateUser(updateRequest(1L, null, null));

        assertNull(result.company());
        assertTrue(result.roles().isEmpty());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent user")
    void updateUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser(updateRequest(99L, null, null)));
        verify(userRepository, never()).save(any(User.class));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("Should delete user when found")
    void deleteUser_found_deletesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository, times(1)).delete(user);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent user")
    void deleteUser_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).delete(any(User.class));
    }
}
