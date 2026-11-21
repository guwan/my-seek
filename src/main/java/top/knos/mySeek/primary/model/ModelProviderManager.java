package top.knos.mySeek.primary.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型提供者管理器。
 * <p>既支持 Spring 启动期注入的静态 Provider（ollama/doubao/qwen），
 * 也支持运行期动态注册的用户自定义 OpenAI 兼容接口。</p>
 */
@Slf4j
@Component
public class ModelProviderManager {

    /** 用并发 Map 保证运行期增删与读取线程安全 */
    private final Map<String, ModelProvider> providers = new ConcurrentHashMap<>();

    @Autowired
    public ModelProviderManager(List<ModelProvider> providerList) {
        for (ModelProvider provider : providerList) {
            providers.put(provider.getProviderName(), provider);
            log.info("注册模型提供者: {}", provider.getProviderName());
        }
    }

    /**
     * 运行时注册 / 覆盖一个提供者（用于用户自定义接口）。
     */
    public void registerProvider(String name, ModelProvider provider) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("提供者名称不能为空");
        }
        providers.put(name, provider);
        log.info("动态注册模型提供者: {}", name);
    }

    /**
     * 运行时注销一个提供者。内置提供者不允许被移除。
     */
    public void unregisterProvider(String name) {
        if (isBuiltIn(name)) {
            log.warn("内置提供者不允许注销: {}", name);
            return;
        }
        providers.remove(name);
        log.info("注销模型提供者: {}", name);
    }

    private boolean isBuiltIn(String name) {
        return "ollama".equals(name) || "doubao".equals(name) || "qwen".equals(name);
    }

    /**
     * 获取所有可用的模型提供者（只读视图）
     */
    public Map<String, ModelProvider> getAllProviders() {
        return Collections.unmodifiableMap(providers);
    }

    /**
     * 根据名称获取模型提供者
     */
    public Optional<ModelProvider> getProvider(String name) {
        return Optional.ofNullable(providers.get(name));
    }

    /**
     * 获取所有可用的模型列表
     */
    public Map<String, List<String>> getAllAvailableModels() {
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, ModelProvider> entry : providers.entrySet()) {
            if (entry.getValue().isAvailable()) {
                result.put(entry.getKey(), entry.getValue().getAvailableModels());
            }
        }
        return result;
    }

    /**
     * 创建 ChatClient
     * 格式: provider:model (例如: doubao:doubao-pro-32k)
     */
    public ChatClient createChatClient(String modelIdentifier) {
        return createChatClient(modelIdentifier, false);
    }

    /**
     * 创建 ChatClient，并按开关下发是否开启模型深度思考。
     */
    public ChatClient createChatClient(String modelIdentifier, boolean enableThinking) {
        if (modelIdentifier == null || modelIdentifier.isEmpty()) {
            throw new IllegalArgumentException("模型标识不能为空");
        }

        // 兼容 ollama:deepseek-r1:8b 这类模型名中带冒号的情况：只用第一段作为 provider
        String[] parts = modelIdentifier.split(":", 2);
        String providerName = parts[0];
        String modelName = parts.length > 1 ? parts[1] : null;

        ModelProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("未知的模型提供者: " + providerName);
        }

        if (!provider.isAvailable()) {
            throw new IllegalStateException("模型提供者不可用: " + providerName);
        }

        return provider.createChatClient(modelName, enableThinking);
    }

    /**
     * 获取默认的 ChatClient（不开启深度思考）。
     */
    public ChatClient getDefaultChatClient() {
        return getDefaultChatClient(false);
    }

    /**
     * 获取默认的 ChatClient，并把深度思考开关传递给提供者。
     */
    public ChatClient getDefaultChatClient(boolean enableThinking) {
        if (providers.containsKey("ollama") && providers.get("ollama").isAvailable()) {
            return providers.get("ollama").createChatClient(null, enableThinking);
        }
        if (providers.containsKey("doubao") && providers.get("doubao").isAvailable()) {
            return providers.get("doubao").createChatClient("doubao-pro-32k", enableThinking);
        }
        if (providers.containsKey("qwen") && providers.get("qwen").isAvailable()) {
            return providers.get("qwen").createChatClient("qwen-plus", enableThinking);
        }
        throw new IllegalStateException("没有可用的模型提供者");
    }

    /**
     * 检查是否有可用的模型提供者
     */
    public boolean hasAvailableProvider() {
        return providers.values().stream().anyMatch(ModelProvider::isAvailable);
    }
}
