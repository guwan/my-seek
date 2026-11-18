package top.knos.mySeek.security.support;

import org.springframework.security.core.Authentication;
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        // 匿名/其他类型的 principal 一律视为未登录用户，避免 ClassCastException
        return principal instanceof User user ? user : null;
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
        return user != null
                && user.getAuthorities().stream()
                .map(Authority::getAuthority)
                .anyMatch(authority -> authority.equals(AuthEnum.ADMIN.getName()));
    }

}
