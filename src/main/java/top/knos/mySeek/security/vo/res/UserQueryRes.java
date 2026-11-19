package top.knos.mySeek.security.vo.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import top.knos.mySeek.security.domain.User;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/1 20:50
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UserQueryRes {
    private Long id;
    private String name;
    private String username;
    private String avatar;
    private String userid;
    private String email;
    private String signature;
    private String title;
    private String group;
    private Integer notifyCount;
    private Integer unreadCount;
    private String country;
    private String access;
    private Boolean enabled;
    private String address;
    private String phone;
    private String homeUrl;

    public static UserQueryRes of(User user) {
        UserQueryRes res = new UserQueryRes();
        res.setName(user.getName());
        res.setAvatar(user.getAvatar());
        res.setUserid(user.getUsername());
        res.setEmail(user.getEmail());
        res.setSignature(user.getSignature());
        res.setTitle(user.getTitle());
        res.setGroup(user.getBusinessGroup());
        res.setNotifyCount(0);
        res.setUnreadCount(0);
        res.setCountry(user.getCountry());
        res.setAccess("admin");
        res.setAddress("");
        res.setPhone(user.getPhone());
        res.setHomeUrl(user.getHomeUrl());
        return res;
    }
}
