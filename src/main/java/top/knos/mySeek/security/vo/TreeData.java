package top.knos.mySeek.security.vo;

import lombok.Data;

import java.util.List;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/11 15:43
 */
@Data
public class TreeData {
    private Integer key;
    private Integer id;
    private String title;
    /** 角色标识：admin / user；数据组织为空 */
    private String authority;
    private List<TreeData> children;
    private Integer parentId;


}
