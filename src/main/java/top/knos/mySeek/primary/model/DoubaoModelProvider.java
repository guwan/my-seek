package top.knos.mySeek.primary.model;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 豆包（字节跳动）模型提供者
 * 使用 OpenAI 兼容 API
 */
@Slf4j
@Component
public class DoubaoModelProvider implements ModelProvider {

    private static final List<String> DEFAULT_MODELS = List.of(
            "doubao-pro-32k",
            "doubao-lite-32k",
            "doubao-pro-128k",
            "doubao-vision-pro-32k"
    );

    private final String apiKey;
    private final String baseUrl;

    public DoubaoModelProvider(@Value("${doubao.api-key:}") String apiKey,
                               @Value("${doubao.base-url:https://ark.cn-beijing.volces.com/api/v3}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getProviderName() {
        return "doubao";
    }

    @Override
    public List<String> getAvailableModels() {
        // 这里可以调用豆包 API 获取实际可用的模型列表
        return DEFAULT_MODELS;
    }

    @Override
    public ChatClient createChatClient(String model) {
        if (!isAvailable()) {
            throw new IllegalStateException("豆包 API Key 未配置");
        }

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiClient(client)
                .options(options)
                .build();

        return ChatClient.builder(chatModel).build();
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
