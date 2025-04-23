package top.knos.mySeek.primary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.knos.mySeek.primary.vo.ChatRequest;


import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
@Slf4j
public class ChatService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private RedisChatMemory redisChatMemory;

    public String chat(ChatRequest request) {
        String sessionId = request.getSessionId();
        String userInput = request.getMessage();

        // 获取历史消息
        //List<Message> history = redisChatMemory.get(sessionId,20);

        // 添加用户新消息
        UserMessage userMessage = new UserMessage(userInput);
        redisChatMemory.add(sessionId, userMessage);

        // 将历史消息和新消息一起发送给AI
        // 调用 AI
        var response = chatClient.prompt()
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
                .user(userMessage.getText())
                .call();


        // 保存AI回复到历史记录
        String aiReply = response.content();

        // 存储 AI 回复
        redisChatMemory.add(sessionId, List.of(new AssistantMessage(aiReply)));
        return aiReply;
    }

    public Flux<String> chatStream(ChatRequest request) {
        String sessionId = request.getSessionId();
        String userInput = request.getMessage();

        // 获取历史消息
        List<Message> history = redisChatMemory.get(sessionId, 20);

        // 添加用户新消息
        UserMessage userMessage = new UserMessage(userInput);
        redisChatMemory.add(sessionId, userMessage);

        // 将历史消息和新消息一起发送给AI
        // 调用 AI
        var response = chatClient.prompt()
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
                .user(userMessage.getText())
                .stream()
                .content();


        // 保存AI回复到历史记录
        StringBuilder sb = new StringBuilder();


        return response
                .doOnNext(sb::append)
                .doOnComplete(() -> {
                    log.info("流式聊天：{}", sb);
                });
    }
}