package top.knos.mySeek.security.support;

import org.springframework.security.core.context.SecurityContextHolder;
import top.knos.mySeek.constant.AuthEnum;
import top.knos.mySeek.security.domain.Authority;
import top.knos.mySeek.security.domain.User;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SecurityUtils {
    public static User getUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = null;
        if (!"anonymousUser".equals(principal)) {
            user = (User) principal;
        }
        return user;
    }

    public static String getAccess(Collection<Authority> auths) {

        //有admin权限，显示admin权限，前端只能有admin和user二选一
        boolean match = auths.stream().anyMatch(authority -> authority.getAuthority().equals(AuthEnum.ADMIN.getName()));
        if (match) {
            return AuthEnum.ADMIN.getName();
        } else {
            return AuthEnum.USER.getName();
        }
    }

    public static Set<Authority> getAuth(String access, List<Authority> auths) {
        Set<Authority> newAuths = new HashSet<>();
        for (Authority auth : auths) {
            if (auth.getAuthority().equals(access)) {
                newAuths.add(auth);
            }
        }
        return newAuths;
    }

    public static boolean currentIsAdmin() {
        User user = SecurityUtils.getUser();
        return user
                .getAuthorities()
                .stream()
                .map(Authority::getAuthority)
                .anyMatch(authority -> authority.equals(AuthEnum.ADMIN.getName()));
    }

}
