package pl.co.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.co.auth.entity.Role;
import pl.co.auth.repository.RoleRepository;
import pl.co.common.security.RoleName;

@Component
@RequiredArgsConstructor
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName.name())
                    .orElseGet(() -> roleRepository.save(Role.builder().name(roleName.name()).build()));
        }
    }
}
