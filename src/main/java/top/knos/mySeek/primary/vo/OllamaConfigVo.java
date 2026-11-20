package top.knos.mySeek.primary.vo;

import lombok.Data;

@Data
public class OllamaConfigVo {

    private String baseUrl;
    private String defaultChatModel;
    private String embeddingModel;
    private String pullModelStrategy;
    /** 值来自数据库配置还是应用启动环境变量 */
    private boolean configured;
    private String updatedAt;
}
