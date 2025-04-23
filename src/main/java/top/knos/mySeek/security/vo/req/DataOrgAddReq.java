package top.knos.mySeek.security.vo.req;

import lombok.Data;

import java.util.Date;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/5/5 19:41
 */
@Data
public class DataOrgAddReq {


    private Integer id;
    private String name;
    private String code;
    private String description;
    private Boolean status;
    private String createdBy;
    private Date createdDate;
    private String updatedBy;
    private Date updatedDate;
}
