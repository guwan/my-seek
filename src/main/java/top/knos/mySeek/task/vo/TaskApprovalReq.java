package top.knos.mySeek.task.vo;

import lombok.Data;

@Data
public class TaskApprovalReq {
    private boolean approved;
    private String reason;
}
