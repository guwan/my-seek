package top.knos.mySeek.security.service;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import top.knos.mySeek.security.domain.Authority;

import java.util.Collection;
import java.util.List;

/**
 * 基于角色的权限控制服务。
 */
@Slf4j
@Component
public class RbacAuthorityService {
    @Autowired
    private AuthorityService authorityService;

    /**
     * 判断当前authentication 是否具有该请求对应的链接的权限
     * @param request
     * @param authentication
     * @return
     */
    public boolean hasPermission(HttpServletRequest request, Authentication authentication) {
        String url = request.getRequestURI();
        List<String> roles = authorityService.findRolesByUrl(url);
        //不需要任何角色就能访问
        if(CollectionUtils.isEmpty(roles)){
            return true;
        }
        //需要满足角色条件才能访问，拿当前用户的角色去判断是不是符合角色条件。
        if(CollectionUtils.isNotEmpty(authentication.getAuthorities())){
            Collection<Authority> auths = (Collection<Authority>) authentication.getAuthorities();
            for (Authority auth : auths) {
                if(roles.contains(auth.getAuthority())){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断当前authentication 是否具有该请求对应的链接的权限
     * 如果不具有权限，则抛出 JwtException
     * @param request
     * @param authentication
     */
    public void checkPermission(HttpServletRequest request, Authentication authentication) {
        boolean hasPermission  = hasPermission(request,authentication);
        if(!hasPermission){
            throw new JwtException("您的角色没有被授权访问该链接。");
        }
    }

}
