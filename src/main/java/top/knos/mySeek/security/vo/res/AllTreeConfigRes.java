package top.knos.mySeek.security.vo.res;


import top.knos.mySeek.security.vo.TreeData;

import java.util.List;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/11 15:47
 */
public class AllTreeConfigRes {
    private List<TreeData> treeData;

    public List<TreeData> getTreeData() {
        return treeData;
    }

    public void setTreeData(List<TreeData> treeData) {
        this.treeData = treeData;
    }
}
