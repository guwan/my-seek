package top.knos.mySeek.security;

import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import top.knos.mySeek.security.service.RbacAuthorityService;

public class JwtTokenFilterConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

  private final JwtTokenProvider jwtTokenProvider;
  private final RbacAuthorityService rbacAuthorityService;

  public JwtTokenFilterConfigurer(JwtTokenProvider jwtTokenProvider, RbacAuthorityService rbacAuthorityService) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.rbacAuthorityService = rbacAuthorityService;
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    JwtTokenFilter customFilter = new JwtTokenFilter(jwtTokenProvider,rbacAuthorityService);
    http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
  }

}
