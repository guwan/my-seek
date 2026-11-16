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
 * 千问（阿里云）模型提供者
 * 使用 OpenAI 兼容 API
 */
@Slf4j
@Component
public class QwenModelProvider implements ModelProvider {

    private static final List<String> DEFAULT_MODELS = List.of(
            "qwen-turbo",
            "qwen-plus",
            "qwen-max",
            "qwen-long",
            "qwen-coder-plus"
    );

    private final String apiKey;
    private final String baseUrl;

    public QwenModelProvider(@Value("${qwen.api-key:}") String apiKey,
                             @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getProviderName() {
        return "qwen";
    }

    @Override
    public List<String> getAvailableModels() {
        // 这里可以调用阿里云 API 获取实际可用的模型列表
        return DEFAULT_MODELS;
    }

    @Override
    public ChatClient createChatClient(String model) {
        if (!isAvailable()) {
            throw new IllegalStateException("千问 API Key 未配置");
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
