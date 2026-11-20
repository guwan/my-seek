package top.knos.mySeek.primary.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OllamaConfigRequest {

    @NotBlank(message = "base-url 不能为空")
    private String baseUrl;

    @NotBlank(message = "默认聊天模型不能为空")
    private String defaultChatModel;

    @NotBlank(message = "嵌入模型不能为空")
    private String embeddingModel;

    /** NEVER / ALWAYS / WHEN_MISSING，留空时保持 NEVER */
    private String pullModelStrategy;
}
