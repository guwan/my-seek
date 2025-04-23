package top.knos.mySeek.common;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/11 11:41
 */
public enum MenuType {
    MENU(1,"菜单"),
    INTERFACE(2,"接口");
    Integer code;
    String name;

    MenuType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
