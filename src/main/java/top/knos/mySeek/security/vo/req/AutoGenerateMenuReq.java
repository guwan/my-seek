package top.knos.mySeek.security.vo.req;

import lombok.Data;

@Data
public class AutoGenerateMenuReq {
    private Boolean autoGenerate4Admin;
    //菜单名
    private String showName;
    //父级菜单
    private String parentName;
    private String parentShowName;
    //路径
    private String path;
    //资源类型
    private Integer type;
}
