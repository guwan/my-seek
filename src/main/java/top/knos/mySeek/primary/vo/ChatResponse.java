package top.knos.mySeek.primary.vo;

public class ChatResponse {
    private String sessionId;
    private ChatMessage message;
    
    public ChatResponse() {}
    
    public ChatResponse(String sessionId, ChatMessage message) {
        this.sessionId = sessionId;
        this.message = message;
    }
    
    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public ChatMessage getMessage() { return message; }
    public void setMessage(ChatMessage message) { this.message = message; }
    
    public static class ChatMessage {
        private String content;
        
        public ChatMessage() {}
        
        public ChatMessage(String content) {
            this.content = content;
        }
        
        // Getters and Setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
