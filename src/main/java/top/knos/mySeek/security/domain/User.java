package top.knos.mySeek.security.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/14 11:05
 */
@Entity(name = "sso_user")
@Table(indexes = {
        @Index(name = "idx_sso_user_username", columnList = "username"),
        @Index(name = "idx_sso_user_email", columnList = "email"),
        @Index(name = "idx_sso_user_phone", columnList = "phone")
})
@Data
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private Integer level;
    private String phone;
    private String avatar;
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


    //创建时间
    @Column(nullable = false)
    private Date createdAt;
    //更新时间
    @Column(nullable = false)
    private Date updateAt;
    @ManyToMany//(cascade={CascadeType.ALL})
    @JoinTable(
            name = "sso_user_authority",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "authority_id")})
    private Set<Authority> authorities = new LinkedHashSet<>();
    @ManyToMany//(cascade={CascadeType.ALL})
    @JoinTable(
            name = "sso_user_data_org",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "data_org_id")})
    private Set<DataOrg> dataOrgs = new LinkedHashSet<>();

    @OneToMany//(cascade={CascadeType.ALL})
    @JoinColumn(name = "user_id")
    private List<UserTag> tags;

    /**
     * 用户来源，自主创建，同步至三佳
     */
    private String source;


    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBusinessGroup() {
        return businessGroup;
    }

    public void setBusinessGroup(String businessGroup) {
        this.businessGroup = businessGroup;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public List<UserTag> getTags() {
        return tags;
    }

    public void setTags(List<UserTag> tags) {
        this.tags = tags;
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(Boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public Boolean getAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(Boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public Boolean getCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(Boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setAuthorities(Set<Authority> authorities) {
        this.authorities = authorities;
    }

    public Date getCreatedAt() {

        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Date updateAt) {
        this.updateAt = updateAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHomeUrl() {
        return homeUrl;
    }

    public void setHomeUrl(String homeUrl) {
        this.homeUrl = homeUrl;
    }

    public Date getLastActivated() {
        return lastActivated;
    }

    public void setLastActivated(Date lastActivated) {
        this.lastActivated = lastActivated;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    @JsonIgnore
    public String getFullName() {
        return name == null ? getUsername() : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id.equals(user.id) &&
                username.equals(user.username) &&
                password.equals(user.password) &&
                Objects.equals(accountNonExpired, user.accountNonExpired) &&
                Objects.equals(accountNonLocked, user.accountNonLocked) &&
                Objects.equals(credentialsNonExpired, user.credentialsNonExpired) &&
                Objects.equals(enabled, user.enabled) &&
                Objects.equals(name, user.name) &&
                Objects.equals(createdAt, user.createdAt) &&
                Objects.equals(updateAt, user.updateAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, password, accountNonExpired, accountNonLocked, credentialsNonExpired, enabled, name, createdAt, updateAt);
    }
}
