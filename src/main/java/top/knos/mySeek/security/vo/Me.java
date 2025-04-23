package top.knos.mySeek.security.vo;

import lombok.Data;
import top.knos.mySeek.security.domain.UserTag;
import top.knos.mySeek.security.vo.res.RoleQueryRes;

import java.util.List;

/**
 * 当前用户
 *
 * @Author 卢文武
 * @Date 2021/4/27 17:23
 */
@Data
public class Me {
    private String username;
    private String name;
    private String avatar;
    private Long id;
    private String email;
    private String signature;
    private String title;
    private String group;
    private List<UserTag> tags;
    private Integer notifyCount;
    private Integer unreadCount;
    private String country;
    private String access;
    private String Geographic;
    private String phone;
    private String dialPhone;
    private List<RoleQueryRes> roles;

}
