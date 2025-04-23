package top.knos.mySeek.primary.domain;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class ChatHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String sessionId;
    private List<MessageRecord> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public ChatHistory() {
        this.messages = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public ChatHistory(String sessionId) {
        this();
        this.sessionId = sessionId;
    }
    
    public void addMessage(Message message) {
        MessageRecord record = new MessageRecord(message);
        this.messages.add(record);
        this.updatedAt = LocalDateTime.now();
    }
    
    public List<Message> getMessagesForModel() {
        List<Message> result = new ArrayList<>();
        for (MessageRecord record : messages) {
            switch (record.getType()) {
                case "SYSTEM":
                    result.add(new SystemMessage(record.getContent()));
                    break;
                case "USER":
                    result.add(new UserMessage(record.getContent()));
                    break;
                case "ASSISTANT":
                    result.add(new AssistantMessage(record.getContent()));
                    break;
            }
        }
        return result;
    }
    
    // Getter and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public List<MessageRecord> getMessages() { return messages; }
    public void setMessages(List<MessageRecord> messages) { this.messages = messages; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // 内部类用于序列化Message
    public static class MessageRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String type;
        private String content;
        private LocalDateTime timestamp;
        
        public MessageRecord() {
            this.timestamp = LocalDateTime.now();
        }
        
        public MessageRecord(Message message) {
            this();
            this.content = message.getText();

            switch (message) {
                case SystemMessage _ -> this.type = "SYSTEM";
                case UserMessage _ -> this.type = "USER";
                case AssistantMessage _ -> this.type = "ASSISTANT";
                default -> {
                }
            }
        }
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}