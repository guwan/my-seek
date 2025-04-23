package top.knos.mySeek.security.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import top.knos.mySeek.exception.TipsException;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.repository.UserRepository;
import top.knos.mySeek.support.MailService;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
@Slf4j
public class UserRegisterSupport {
    private static final String format = "_register_token_";
    private static final String randomKey = "sadfasfasf!35asd";
    private static final long validTime = 10 * 60 * 1000;//10分钟失效

    @Value("${mail.send.register.host}")
    private String REGISTER_HOST;
    @Autowired
    private MailService mailService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;

    EncryptUtil encryptUtil = EncryptUtil.getInstance();
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String getToken(String recipient) throws UnsupportedEncodingException {
        String token = recipient + format + new Date().getTime();
        String encodeToke = encryptUtil.AESencode(token, randomKey);
        return URLEncoder.encode(encodeToke, StandardCharsets.UTF_8);
    }

    public String tokenValid(String encodeToken) throws UnsupportedEncodingException {
        String decodeToken = URLDecoder.decode(encodeToken, StandardCharsets.UTF_8);
        decodeToken = encryptUtil.AESdecode(decodeToken, randomKey);
        String[] tokenStrs = decodeToken.split(format);
        String timeString = tokenStrs[1];
        long time = Long.parseLong(timeString);
        long difference = System.currentTimeMillis() - time;
        if (0 < difference && difference < validTime) {
            //mail
            return tokenStrs[0];
        }
        return null;
    }

    private static final String content = "<html><head></head><body><h1>" +
            "这是一封<a href='{site}'>K导航</a>的激活邮件,激活请点击以下链接</h1><h3>" +
            "<a href='{site}/register/token/{token}'>" +
            "{site}/register/token/{token}</a></h3></body></html>";
    private static final String successContent = "<html><head></head><body><h1>" +
            "恭喜你，成功注册<a href='{site}'>K导航</a>系统";
    private String siteStr = "\\{site}";
    private String tokenStr = "\\{token}";

    public boolean sendRegisterMail(String recipient) {
        try {
            String mailContent = content.replaceAll(siteStr, REGISTER_HOST).replaceAll(tokenStr, getToken(recipient));
            mailService.send(recipient, mailContent, "账号激活邮件");
        } catch (Exception e) {
            log.error("发送邮件失败：\n{}", ExceptionUtils.getThrowableCount(e));
            throw new TipsException("发送邮件失败");
        }
        return true;
    }

    public boolean sendRegisterSuccessMail(String recipient) {
        String content = successContent.replaceAll(siteStr, REGISTER_HOST);
        try {
            mailService.send(recipient, content, "账号激活邮件");
        } catch (Exception e) {
            log.error("发送邮件错误：\n{}", ExceptionUtils.getThrowableCount(e));
            return false;
        }
        return true;
    }

    public User getUser(UserRegisterReq userRegisterReq) {
        User dbUser = userRepository.findByEmail(userRegisterReq.getEmail());
        //如果用户存在且未被激活，则删除重建
        if (dbUser != null && !dbUser.getEnabled()) {
            userRepository.delete(dbUser);
        }
        User user = new User();
        user.setUsername(userRegisterReq.getUsername());
        user.setName(userRegisterReq.getUsername());
        user.setPassword(passwordEncoder.encode(userRegisterReq.getPassword()));
        user.setEmail(userRegisterReq.getEmail());
        user.setPhone(userRegisterReq.getPhone());
        user.setJob(userRegisterReq.getJob());
        user.setEnabled(false);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCreatedAt(new Date());
        user.setUpdateAt(new Date());
        return user;
    }
}
