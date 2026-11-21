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

    /**
     * 是否启用联网搜索。开启后会先检索实时资料并注入模型上下文。
     */
    private boolean webSearch = false;

    /**
     * 是否开启模型深度思考。对混合思考模型（如 qwen3）会下发 think 参数；
     * 关闭时模型直接作答、不产生推理过程。
     */
    private boolean enableThinking = false;

    /**
     * 是否参考当前用户的知识库。开启后从向量库检索相关片段并注入模型上下文。
     */
    private boolean useKnowledge = true;

    /**
     * 知识库检索词。为空时使用 message；写作场景可传更简短的意图描述。
     */
    @Size(max = 500, message = "知识库检索词长度不能超过 500")
    private String knowledgeQuery;

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
