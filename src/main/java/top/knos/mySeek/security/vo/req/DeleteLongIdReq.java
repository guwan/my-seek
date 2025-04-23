package top.knos.mySeek.security.vo.req;

import java.util.List;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/6 21:22
 */
public class DeleteLongIdReq {
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
