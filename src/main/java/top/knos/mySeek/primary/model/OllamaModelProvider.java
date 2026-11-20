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
import java.util.Objects;

/**
 * Ollama 本地模型提供者。
 * <p>base-url 与默认模型支持运行时修改：配置服务调用 {@link #reconfigure} 后，
 * 新的 OllamaApi 会立刻用于模型列表、连通性检查与 ChatClient 构建。</p>
 */
@Slf4j
@Component
public class OllamaModelProvider implements ModelProvider {

    /** Ollama 不可用时返回的兜底模型列表 */
    private static final List<String> FALLBACK_MODELS = List.of("deepseek-r1:8b", "llama2", "qwen:7b");

    /** 模型列表缓存时长（毫秒），避免每次请求都同步调用 Ollama */
    private static final long MODEL_CACHE_TTL = 30_000L;

    private volatile String baseUrl;
    private volatile String defaultChatModel;
    private volatile String embeddingModel;
    private volatile String pullModelStrategy;
    private volatile OllamaApi ollamaApi;

    /** 缓存的模型列表及其过期时间 */
    private volatile List<String> cachedModels;
    private volatile long cacheExpireAt;

    public OllamaModelProvider(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${spring.ai.ollama.chat.model:deepseek-r1:8b}") String defaultChatModel,
            @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}") String embeddingModel,
            @Value("${spring.ai.ollama.init.pull-model-strategy:NEVER}") String pullModelStrategy) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.defaultChatModel = defaultChatModel;
        this.embeddingModel = embeddingModel;
        this.pullModelStrategy = pullModelStrategy;
        this.ollamaApi = OllamaApi.builder().baseUrl(this.baseUrl).build();
    }

    /** 运行时应用新配置；地址变化才重建客户端并清空模型缓存。 */
    public synchronized void reconfigure(String newBaseUrl, String newDefaultChatModel,
                                         String newEmbeddingModel, String newPullModelStrategy) {
        String normalized = normalizeBaseUrl(newBaseUrl);
        if (!Objects.equals(this.baseUrl, normalized)) {
            this.baseUrl = normalized;
            this.ollamaApi = OllamaApi.builder().baseUrl(normalized).build();
            this.cachedModels = null;
            this.cacheExpireAt = 0;
        }
        this.defaultChatModel = newDefaultChatModel;
        this.embeddingModel = newEmbeddingModel;
        this.pullModelStrategy = newPullModelStrategy;
        log.info("Ollama 运行时配置已更新: baseUrl={}, defaultModel={}, embeddingModel={}",
                this.baseUrl, this.defaultChatModel, this.embeddingModel);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getDefaultChatModel() {
        return defaultChatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public String getPullModelStrategy() {
        return pullModelStrategy;
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
        OllamaApi api = ollamaApi;
        try {
            List<String> models = new ArrayList<>();
            for (OllamaApi.Model model : api.listModels().models()) {
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
        String useModel = (model == null || model.isBlank()) ? defaultChatModel : model;
        var builder = OllamaChatOptions.builder().model(useModel);
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

    private static String normalizeBaseUrl(String url) {
        if (url == null) return "http://localhost:11434";
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
