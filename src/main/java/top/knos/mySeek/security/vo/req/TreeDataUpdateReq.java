package top.knos.mySeek.security.vo.req;

import java.util.List;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/11 16:04
 */
public class TreeDataUpdateReq {
    private List<Integer>  checkedKeys;
    private Integer id;

    public List<Integer> getCheckedKeys() {
        return checkedKeys;
    }

    public void setCheckedKeys(List<Integer> checkedKeys) {
        this.checkedKeys = checkedKeys;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
