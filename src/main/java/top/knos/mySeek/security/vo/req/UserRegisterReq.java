package top.knos.mySeek.security.vo.req;

public class UserRegisterReq {
    private String username;
    private String email;
    private String phone;
    private String job;
    private String password;
    private String retypePassword;
    private String captcha;
    private String agreeTerms;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRetypePassword() {
        return retypePassword;
    }

    public void setRetypePassword(String retypePassword) {
        this.retypePassword = retypePassword;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public String getAgreeTerms() {
        return agreeTerms;
    }

    public void setAgreeTerms(String agreeTerms) {
        this.agreeTerms = agreeTerms;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }
}
