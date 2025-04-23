package top.knos.mySeek.security.vo.req;

import java.util.List;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/6 21:22
 */
public class DeleteReq {
    private List<Integer> ids;

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }
}
