package top.knos.mySeek.primary.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.primary.vo.ChatRequest;


import reactor.core.publisher.Flux;
import top.knos.mySeek.support.vo.ChatInfoVo;

import java.util.List;


@Service
@Slf4j
public class ChatService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private RedisChatMemory redisChatMemory;

    public String chat(ChatRequest request) {
        String conversationId = request.getConversationId();
        String userInput = request.getMessage();

        // 获取历史消息
        //List<Message> history = redisChatMemory.get(sessionId,20);

        // 添加用户新消息
        UserMessage userMessage = new UserMessage(userInput);
        redisChatMemory.add(conversationId, userMessage);

        // 将历史消息和新消息一起发送给AI
        // 调用 AI
        var response = chatClient
                .prompt()
                .advisors(advisorSpec -> {
                    MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor
                            .builder(redisChatMemory)
                            .conversationId(conversationId)
                            .order(50)
                            .build();
                    advisorSpec.advisors(advisor);
                })
                .user(userMessage.getText())
                .call();
//        var response = chatClient.prompt()
//                .advisors(a -> a
//                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
//                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
//                .user(userMessage.getText())
//                .call();


        // 保存AI回复到历史记录
        String aiReply = response.content();

        // 存储 AI 回复
        redisChatMemory.add(conversationId, List.of(new AssistantMessage(aiReply)));
        return aiReply;
    }

    public Flux<String> chatStream(ChatRequest request) {
        String conversationId = request.getConversationId();
        String userInput = request.getMessage();

        // 获取历史消息
        List<Message> history = redisChatMemory.get(conversationId);

        // 添加用户新消息
        UserMessage userMessage = new UserMessage(userInput);
        redisChatMemory.add(conversationId, userMessage);

        // 将历史消息和新消息一起发送给AI
        // 调用 AI
        var response = chatClient
                .prompt()
                .advisors(advisorSpec -> {
                    MessageChatMemoryAdvisor advisor = MessageChatMemoryAdvisor
                            .builder(redisChatMemory)
                            .conversationId(conversationId)
                            .order(50)
                            .build();
                    advisorSpec.advisors(advisor);
                })
                .user(userMessage.getText())
                .stream()
                .content();
//        var response = chatClient.prompt()
//                .advisors(a -> a
//                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
//                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
//                .user(userMessage.getText())
//                .stream()
//                .content();

        // 保存AI回复到历史记录
        StringBuilder sb = new StringBuilder();


        return response
                .doOnNext(sb::append)
                .doOnComplete(() -> {
                    log.info(sb.toString());
                    redisChatMemory.add(conversationId, new AssistantMessage(sb.toString()));
                });
    }

    public ChatInfoVo chatInfo(String conversationId) {
        Message message = redisChatMemory.getFirstOne(conversationId);
        String text = message.getText();
        String title = text != null ? text.substring(0, Math.min(100, text.length())) : "";
        ChatInfoVo vo = new ChatInfoVo();
        vo.setTitle(title);
        vo.setId(conversationId);
        return vo;
    }
}