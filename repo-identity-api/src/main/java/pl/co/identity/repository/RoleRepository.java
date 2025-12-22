package pl.co.identity.repository;

import pl.co.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.identity.entity.RoleName;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, String> {
    Optional<Role> findByName(RoleName name);
}
