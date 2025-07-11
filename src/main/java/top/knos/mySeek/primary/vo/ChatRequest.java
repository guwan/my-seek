package top.knos.mySeek.primary.vo;

import lombok.Data;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

@Data
public class ChatRequest {
    private String conversationId;
    private String message;
    private List<Message> messages;


    public ChatRequest() {
    }
    public ChatRequest(String conversationId, String message) {
        this.conversationId = conversationId;
        this.message = message;
    }
}