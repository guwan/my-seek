package top.knos.mySeek.security.vo.res;

import java.util.List;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/11 15:47
 */
public class SelectKeysRes {
    private List<Integer> checkedKeys;

    public List<Integer> getCheckedKeys() {
        return checkedKeys;
    }

    public void setCheckedKeys(List<Integer> checkedKeys) {
        this.checkedKeys = checkedKeys;
    }
}
