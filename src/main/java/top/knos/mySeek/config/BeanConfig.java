package top.knos.mySeek.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfig {

    /**
     * 默认聊天客户端。
     * <p>聊天记忆 Advisor 不在此处默认注册：Spring AI 2.0 起 Advisor 需要按请求携带会话 ID，
     * 由 {@code ChatService} 在每次请求时按用户维度注入。</p>
     */
    @Bean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
