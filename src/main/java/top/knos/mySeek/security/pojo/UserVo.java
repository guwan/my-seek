package top.knos.mySeek.security.pojo;

import lombok.Data;
import org.springframework.security.core.userdetails.UserDetails;
import top.knos.mySeek.security.domain.Authority;
import top.knos.mySeek.security.domain.DataOrg;
import top.knos.mySeek.security.domain.UserTag;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/14 11:05
 */
@Data
public class UserVo implements UserDetails {
    private Long id;
    //登录名
    private String username;
    private String password;
    private Boolean accountNonExpired = true;
    private Boolean accountNonLocked = true;
    private Boolean credentialsNonExpired = true;
    private Boolean enabled = true;
    //昵称
    private String name;
    private String avatar;
    private Integer level;
    private String phone;
    private String dialPhone;
    private String job;
    private String email;
    private String homeUrl;
    private String signature;
    private String title;
    private String businessGroup;
    private String country;
    private String deptId;
    //密码输入失败错误次数
    private Integer passwordFailTimes;
    /**
     * 最后活动时间
     */
    private Date lastActivated;


    private Date createdAt;
    private Date updateAt;
    private Set<Authority> authorities = new LinkedHashSet<>();
    private Set<DataOrg> dataOrgs = new LinkedHashSet<>();

    private List<UserTag> tags;

    /**
     * 用户来源，自主创建，同步至三佳
     */
    private String source;

    @Override
    public Set<Authority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired == null || accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked == null || accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired == null || credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled == null || enabled;
    }

}
