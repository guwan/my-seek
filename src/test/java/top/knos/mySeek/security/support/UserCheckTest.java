package top.knos.mySeek.security.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import top.knos.mySeek.exception.TipsException;
import top.knos.mySeek.security.repository.UserRepository;
import top.knos.mySeek.security.service.GrecaptchaService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserCheckTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final GrecaptchaService grecaptchaService = mock(GrecaptchaService.class);
    private final UserCheck userCheck = new UserCheck(userRepository, grecaptchaService);

    private UserRegisterReq registerReq(String email) {
        UserRegisterReq req = new UserRegisterReq();
        req.setAgreeTerms("on");
        req.setEmail(email);
        req.setPhone("13800138000");
        req.setUsername("tester");
        req.setPassword("password");
        req.setJob("developer");
        req.setCaptcha("1234");
        return req;
    }

    @Test
    @DisplayName("Invalid email is rejected")
    void invalidEmailRejected() {
        assertThrows(TipsException.class, () -> userCheck.checkRegister(registerReq(null)));
        assertThrows(TipsException.class, () -> userCheck.checkRegister(registerReq("not-an-email")));
    }

    @Test
    @DisplayName("Valid registration request passes the check")
    void validRegisterAccepted() {
        when(grecaptchaService.validate(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByUsername(Mockito.anyString())).thenReturn(null);
        when(userRepository.findByEmail(Mockito.anyString())).thenReturn(null);

        assertDoesNotThrow(() -> userCheck.checkRegister(registerReq("tester@example.com")));
    }

    @Test
    @DisplayName("Phone number length is validated")
    void phoneLengthValidated() {
        assertThrows(TipsException.class, () -> userCheck.checkPhone("123"));
        assertThrows(TipsException.class, () -> userCheck.checkPhone(null));
        assertDoesNotThrow(() -> userCheck.checkPhone("13800138000"));
    }
}
