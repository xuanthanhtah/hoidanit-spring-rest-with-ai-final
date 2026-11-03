package vn.hoidanit.springrestwithai.feature.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.springrestwithai.dto.PageResponse;
import vn.hoidanit.springrestwithai.exception.DuplicateResourceException;
import vn.hoidanit.springrestwithai.exception.ResourceNotFoundException;
import vn.hoidanit.springrestwithai.feature.company.Company;
import vn.hoidanit.springrestwithai.feature.company.CompanyRepository;
import vn.hoidanit.springrestwithai.feature.role.Role;
import vn.hoidanit.springrestwithai.feature.role.RoleRepository;
import vn.hoidanit.springrestwithai.feature.user.dto.CreateUserRequest;
import vn.hoidanit.springrestwithai.feature.user.dto.UpdateUserRequest;
import vn.hoidanit.springrestwithai.feature.user.dto.UserResponse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String RESOURCE_NAME = "Người dùng";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, CompanyRepository companyRepository,
            RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // readOnly transaction keeps the session open while lazy company/roles are mapped to DTOs
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> page = this.userRepository.findAll(pageable);
        return PageResponse.of(page, page.map(UserResponse::fromEntity).getContent());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.fromEntity(findUserOrThrow(id));
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (this.userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(RESOURCE_NAME, "email", request.email());
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(this.passwordEncoder.encode(request.password()));
        user.setAge(request.age());
        user.setGender(request.gender());
        user.setAddress(request.address());
        user.setAvatar(request.avatar());
        user.setCompany(findCompanyOrNull(request.companyId()));
        user.setRoles(findRolesOrThrow(request.roleIds()));
        User saved = this.userRepository.save(user);
        log.info("Created user id={} with {} roles", saved.getId(), saved.getRoles().size());
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UpdateUserRequest request) {
        User user = findUserOrThrow(request.id());
        user.setName(request.name());
        user.setAge(request.age());
        user.setGender(request.gender());
        user.setAddress(request.address());
        user.setAvatar(request.avatar());
        // PUT replaces the resource: null companyId clears the company, roleIds replaces the whole list
        user.setCompany(findCompanyOrNull(request.companyId()));
        user.setRoles(findRolesOrThrow(request.roleIds()));
        User saved = this.userRepository.save(user);
        log.info("Updated user id={} with {} roles", saved.getId(), saved.getRoles().size());
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        // User owns user_role, so Hibernate removes its join rows; company and roles are kept
        this.userRepository.delete(user);
        log.info("Deleted user id={}", id);
    }

    private User findUserOrThrow(Long id) {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "id", id));
    }

    private Company findCompanyOrNull(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return this.companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Công ty", "id", companyId));
    }

    private List<Role> findRolesOrThrow(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(roleIds);
        List<Role> roles = this.roleRepository.findAllById(uniqueIds);
        if (roles.size() != uniqueIds.size()) {
            roles.forEach(role -> uniqueIds.remove(role.getId()));
            throw new ResourceNotFoundException("Vai trò", "id", uniqueIds);
        }
        return new ArrayList<>(roles);
    }
}
