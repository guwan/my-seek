package top.knos.mySeek.security.vo.req;

import lombok.Data;
import lombok.EqualsAndHashCode;
import top.knos.mySeek.support.vo.SortAndFilter;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/5 19:41
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class MenuQueryReq extends SortAndFilter {
    private Integer id;

    private Integer parentId;
    /**
     * 资源类型：1菜单，2，接口数据
     */
    private Integer type;
    private Integer sort;
    /**
     * 展示到后台勾选时的名字
     */
    private String showName;
    /** @name 在菜单中隐藏子节点 */
    private Boolean hideChildrenInMenu;
    /** @name 在菜单中隐藏自己和子节点 */
    private Boolean hideInMenu;
    /** @name 菜单的icon */
    private String icon;
    /** @name 自定义菜单的国际化 key */
    private String locale;
    /** @name 菜单的名字 */
    private String name;
    /** @name disable 菜单选项 */
    private Boolean disabled;
    /** @name 路径,可以设定为网页链接 */
    private String path;
    /**
     * 当此节点被选中的时候也会选中 parentKeys 的节点
     *
     * @name 自定义父节点
     */
    private String parentKeys;
    /** @name 隐藏自己，并且将子节点提升到与自己平级 */
    private Boolean flatMenu;
    /** @name 指定外链打开形式，同a标签 */
    private String target;

}
