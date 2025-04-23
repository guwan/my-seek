package top.knos.mySeek.security.vo.req;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/5 19:41
 */
public class UserAddReq {

    private Long id;
    private String name;
    private String username;
    private String phone;
    private String dialPhone;
    private String email;
    private Integer level;
    private Boolean enabled;
    private String password;
    private String access;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDialPhone() {
        return dialPhone;
    }

    public void setDialPhone(String dialPhone) {
        this.dialPhone = dialPhone;
    }
}
