package top.knos.mySeek.security.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import top.knos.mySeek.security.domain.Authority;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.service.impl.UserService;
import top.knos.mySeek.security.vo.req.UserQueryReq;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class UserControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userManagementRejectsNonAdmin() {
        User user = new User();
        user.setId(1L);
        Authority role = new Authority();
        role.setAuthority("user");
        user.setAuthorities(Set.of(role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        UserService userService = mock(UserService.class);
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);

        assertThrows(AccessDeniedException.class, () -> controller.queryUser(new UserQueryReq()));
        verifyNoInteractions(userService);
    }
}
