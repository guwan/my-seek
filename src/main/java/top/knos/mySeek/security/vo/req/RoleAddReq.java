package top.knos.mySeek.security.vo.req;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/5 19:41
 */
public class RoleAddReq{

    private Integer id;
    private String name;
    private String authority;
    private String mark;
    private Integer sort;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
