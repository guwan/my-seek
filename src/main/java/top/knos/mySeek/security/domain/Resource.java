package top.knos.mySeek.security.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/16 13:23
 */
@Entity(name = "sso_resource")
@Table(indexes = {
        @Index(name = "idx_sso_resource_path", columnList = "path"),
        @Index(name = "idx_sso_resource_parent_id", columnList = "parent_id"),
        @Index(name = "idx_sso_resource_type", columnList = "type")
})
public class Resource implements Serializable {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;


    /*@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Resource parent;*/


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

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Resource parent;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Resource getParent() {
        return parent;
    }

    public void setParent(Resource parent) {
        this.parent = parent;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getShowName() {
        return showName;
    }

    public void setShowName(String showName) {
        this.showName = showName;
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

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
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

    public String getParentKeys() {
        return parentKeys;
    }

    public void setParentKeys(String parentKeys) {
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

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return Objects.equals(id, resource.id) && Objects.equals(path, resource.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, path);
    }
}
