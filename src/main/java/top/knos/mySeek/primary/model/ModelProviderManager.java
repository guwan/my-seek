package top.knos.mySeek.primary.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 模型提供者管理器
 */
@Slf4j
@Component
public class ModelProviderManager {

    private final Map<String, ModelProvider> providers = new HashMap<>();

    @Autowired
    public ModelProviderManager(List<ModelProvider> providerList) {
        for (ModelProvider provider : providerList) {
            providers.put(provider.getProviderName(), provider);
            log.info("注册模型提供者: {}", provider.getProviderName());
        }
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
        if (modelIdentifier == null || modelIdentifier.isEmpty()) {
            throw new IllegalArgumentException("模型标识不能为空");
        }

        // 解析 provider 和 model
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

        return provider.createChatClient(modelName);
    }

    /**
     * 获取默认的 ChatClient
     */
    public ChatClient getDefaultChatClient() {
        // 优先使用 Ollama（本地）
        if (providers.containsKey("ollama") && providers.get("ollama").isAvailable()) {
            return providers.get("ollama").createChatClient(null);
        }
        
        // 其次使用豆包
        if (providers.containsKey("doubao") && providers.get("doubao").isAvailable()) {
            return providers.get("doubao").createChatClient("doubao-pro-32k");
        }
        
        // 最后使用千问
        if (providers.containsKey("qwen") && providers.get("qwen").isAvailable()) {
            return providers.get("qwen").createChatClient("qwen-plus");
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
