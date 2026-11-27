package top.knos.mySeek.task.vo;

import lombok.Data;

@Data
public class TaskCreateReq {
    private String title;
    private String goal;
    private String constraints;
    /** provider:model，留空使用默认模型 */
    private String modelCode;
    /** 专家 code，留空使用通用助理 */
    private String agentCode;
}
