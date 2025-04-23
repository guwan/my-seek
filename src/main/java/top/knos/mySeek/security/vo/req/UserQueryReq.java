package top.knos.mySeek.security.vo.req;

import lombok.Data;
import lombok.EqualsAndHashCode;
import top.knos.mySeek.support.vo.PageBaseReq;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/5 19:41
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class UserQueryReq extends PageBaseReq {

    private Long id;
    private String name;
    private String username;
    private String phone;
    private String email;
    private Integer level;
    private Boolean enabled;
}
