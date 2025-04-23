package top.knos.mySeek.support.vo;

import lombok.Data;

import java.util.List;

/**
 * 返回给前端的分页信息
 *
 * @Author andy
 * @Date 2022/3/12 16:33
 */
@Data
public class PageVo<T> {
    private Integer current;
    private Integer pageSize;
    private Long total;
    private List<T> data;
}
