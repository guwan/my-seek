package top.knos.mySeek.primary.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "conversationId 不能为空")
    @Size(max = 64, message = "conversationId 长度不能超过 64")
    private String conversationId;

    @NotBlank(message = "message 不能为空")
    @Size(max = 8000, message = "message 长度不能超过 8000")
    private String message;

    /**
     * 模型标识，格式为 {@code provider:model}，为空时使用默认模型。
     */
    @Size(max = 128, message = "model 长度不能超过 128")
    private String model;

    public ChatRequest() {
    }

    public ChatRequest(String conversationId, String message) {
        this.conversationId = conversationId;
        this.message = message;
    }

    public ChatRequest(String conversationId, String message, String model) {
        this.conversationId = conversationId;
        this.message = message;
        this.model = model;
    }
}
