package top.knos.mySeek.primary.model;

import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * 模型提供者接口
 */
public interface ModelProvider {
    
    /**
     * 获取提供者名称
     */
    String getProviderName();
    
    /**
     * 获取可用的模型列表
     */
    List<String> getAvailableModels();
    
    /**
     * 创建 ChatClient
     */
    ChatClient createChatClient(String model);

    /**
     * 创建 ChatClient，并指定是否开启模型深度思考。
     * 默认忽略该参数，由支持的提供者（如 Ollama）覆盖实现。
     */
    default ChatClient createChatClient(String model, boolean enableThinking) {
        return createChatClient(model);
    }
    
    /**
     * 是否支持流式响应
     */
    boolean supportsStreaming();
    
    /**
     * 检查提供者是否可用
     */
    boolean isAvailable();
}
