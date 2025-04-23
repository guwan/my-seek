package top.knos.mySeek.security.vo.res;

import java.util.List;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/9 14:49
 */
public class MenuRes {
    private List<MenuRes> children;
    /** @name 在菜单中隐藏子节点 */
    private Boolean hideChildrenInMenu;
    /** @name 在菜单中隐藏自己和子节点 */
    private Boolean hideInMenu;
    /** @name 菜单的icon */
    private String icon;
    /** @name 自定义菜单的国际化 key */
    //private boolean locale;
    /** @name 菜单的名字 */
    private String name;
    /** @name 用于标定选中的值，默认是 path */
    //private String key;
    /** @name disable 菜单选项 */
    private Boolean disabled;
    /** @name 路径,可以设定为网页链接 */
    private String path;
    /**
     * 当此节点被选中的时候也会选中 parentKeys 的节点
     *
     * @name 自定义父节点
     */
    private List<String> parentKeys;
    /** @name 隐藏自己，并且将子节点提升到与自己平级 */
    private Boolean flatMenu;
    /** @name 指定外链打开形式，同a标签 */
    private String target;

    public List<MenuRes> getChildren() {
        return children;
    }

    public void setChildren(List<MenuRes> children) {
        this.children = children;
    }

    public Boolean getHideChildrenInMenu() {
        return hideChildrenInMenu;
    }

    public void setHideChildrenInMenu(Boolean hideChildrenInMenu) {
        this.hideChildrenInMenu = hideChildrenInMenu;
    }

    public Boolean getHideInMenu() {
        return hideInMenu;
    }

    public void setHideInMenu(Boolean hideInMenu) {
        this.hideInMenu = hideInMenu;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getParentKeys() {
        return parentKeys;
    }

    public void setParentKeys(List<String> parentKeys) {
        this.parentKeys = parentKeys;
    }

    public Boolean getFlatMenu() {
        return flatMenu;
    }

    public void setFlatMenu(Boolean flatMenu) {
        this.flatMenu = flatMenu;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
