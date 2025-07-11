package top.knos.mySeek.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.knos.mySeek.primary.service.RedisChatMemory;

@Configuration
public class BeanConfig {

    @Bean
    public ChatClient chatClient(OllamaChatModel chatModel, RedisChatMemory redisChatMemory) {
        return ChatClient
                .builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor
                        .builder(redisChatMemory)
                        .build())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
