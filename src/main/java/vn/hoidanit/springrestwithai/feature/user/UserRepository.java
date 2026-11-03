package vn.hoidanit.springrestwithai.feature.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    // Fetch-join the ManyToOne company (safe with pagination); roles are loaded via @BatchSize
    @Override
    @EntityGraph(attributePaths = "company")
    Page<User> findAll(Pageable pageable);
}
