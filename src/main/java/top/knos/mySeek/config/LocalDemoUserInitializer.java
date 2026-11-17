package top.knos.mySeek.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.knos.mySeek.security.domain.Authority;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.repository.AuthorityRepository;
import top.knos.mySeek.security.repository.UserRepository;

import java.util.Date;
import java.util.Set;

@Slf4j
@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalDemoUserInitializer {

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Bean
    CommandLineRunner localDemoUserRunner() {
        return args -> {
            if (environment.acceptsProfiles(Profiles.of("prod"))) {
                return;
            }
            if (userRepository.findByUsername("demo") != null) {
                return;
            }

            User user = new User();
            user.setUsername("demo");
            user.setPassword(passwordEncoder.encode("123456"));
            user.setName("演示账号");
            user.setEnabled(true);
            user.setAccountNonExpired(true);
            user.setAccountNonLocked(true);
            user.setCredentialsNonExpired(true);
            Date now = new Date();
            user.setCreatedAt(now);
            user.setUpdateAt(now);
            user.setSource("local-demo");

            Authority userAuthority = authorityRepository.findByAuthority("user");
            if (userAuthority != null) {
                user.setAuthorities(Set.of(userAuthority));
            }

            userRepository.save(user);
            log.info("local 演示账号已创建：demo / 123456");
        };
    }
}
