package top.knos.mySeek.support.vo;

import lombok.Data;

/**
 * 用于搜索下拉框的参数
 *
 * @Author andy
 * @Date 2023/3/14 22:01
 */
@Data
public class PageKeyWords {
    private Integer current=1;
    private Integer pageSize=10;
    private String keyWords;
    private Integer id;

    public void fillNull(){
        if (current==null){
            current=1;
        }
        if (pageSize==null){
            pageSize=10;
        }
    }
}
