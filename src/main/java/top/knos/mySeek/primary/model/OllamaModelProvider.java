package top.knos.mySeek.primary.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Ollama 本地模型提供者。
 * <p>{@link OllamaApi} 实例只创建一次并复用，避免每次请求都新建 HTTP 客户端。</p>
 */
@Slf4j
@Component
public class OllamaModelProvider implements ModelProvider {

    /** Ollama 不可用时返回的兜底模型列表 */
    private static final List<String> FALLBACK_MODELS = List.of("deepseek-r1:8b", "llama2", "qwen:7b");

    /** 模型列表缓存时长（毫秒），避免每次请求都同步调用 Ollama */
    private static final long MODEL_CACHE_TTL = 30_000L;

    private final String baseUrl;
    private final OllamaApi ollamaApi;

    /** 缓存的模型列表及其过期时间 */
    private volatile List<String> cachedModels;
    private volatile long cacheExpireAt;

    public OllamaModelProvider(@Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.ollamaApi = OllamaApi.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public List<String> getAvailableModels() {
        List<String> cached = cachedModels;
        if (cached != null && System.currentTimeMillis() < cacheExpireAt) {
            return cached;
        }
        try {
            List<String> models = new ArrayList<>();
            for (OllamaApi.Model model : ollamaApi.listModels().models()) {
                if (model.name() != null) {
                    models.add(model.name());
                }
            }
            this.cachedModels = List.copyOf(models);
            this.cacheExpireAt = System.currentTimeMillis() + MODEL_CACHE_TTL;
            return this.cachedModels;
        } catch (Exception e) {
            log.warn("无法获取 Ollama 模型列表: {}", e.getMessage());
            return FALLBACK_MODELS;
        }
    }

    @Override
    public ChatClient createChatClient(String model) {
        return createChatClient(model, false);
    }

    /**
     * 对混合思考模型（qwen3、deepseek-r1 等）下发 think 参数，
     * 这样关闭时直接作答、开启时才会输出 <think> 推理过程。
     */
    @Override
    public ChatClient createChatClient(String model, boolean enableThinking) {
        var builder = OllamaChatOptions.builder().model(model);
        if (enableThinking) {
            builder.enableThinking();
        } else {
            builder.disableThinking();
        }

        OllamaChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .options(builder.build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public boolean isAvailable() {
        try {
            return !ollamaApi.listModels().models().isEmpty();
        } catch (Exception e) {
            log.debug("Ollama 服务不可用: {}", e.getMessage());
            return false;
        }
    }
    public String getBaseUrl() {
        return baseUrl;
    }
}
