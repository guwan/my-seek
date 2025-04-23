package top.knos.mySeek.support.vo;

import lombok.Data;

/**
 * TODO
 *
 * @Author andy
 * @Date 2022/3/13 20:38
 */
@Data
public class PageReq <T>{
    private Integer current;
    private Integer pageSize;
    private T query;
}
