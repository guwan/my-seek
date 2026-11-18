package top.knos.mySeek.primary.model;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

/**
 * 通用 OpenAI 兼容接口提供者。
 * <p>不标注 {@code @Component}，由 {@code AiProviderConfigService} 在运行时按用户配置构造并注册。
 * 支持任何兼容 OpenAI Chat Completions 协议的服务（DeepSeek、Moonshot、智谱、OpenAI、Ollama OpenAI 兼容端点等）。</p>
 */
@Slf4j
public class OpenAICompatibleProvider implements ModelProvider {

    private final String providerName;
    private final String apiKey;
    private final String baseUrl;
    private final List<String> models;

    public OpenAICompatibleProvider(String providerName, String apiKey, String baseUrl, List<String> models) {
        this.providerName = providerName;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.models = models == null ? List.of() : List.copyOf(models);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public List<String> getAvailableModels() {
        return models;
    }

    @Override
    public ChatClient createChatClient(String model) {
        String useModel = (model == null || model.isBlank()) ? models.stream().findFirst().orElse("") : model;
        if (useModel.isBlank()) {
            throw new IllegalStateException("未指定模型，且该接口未配置任何模型: " + providerName);
        }
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(useModel)
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
        return apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank()
                && !models.isEmpty();
    }
}
