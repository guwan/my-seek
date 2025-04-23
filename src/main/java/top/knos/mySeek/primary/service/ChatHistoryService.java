//package top.knos.mySeek.primary.service;
//
//import org.springframework.ai.chat.messages.Message;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//import top.knos.mySeek.primary.domain.ChatHistory;
//
//
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
//@Service
//public class ChatHistoryService {
//
//    private static final String CHAT_HISTORY_KEY_PREFIX = "chat:history:";
//    private static final long CHAT_HISTORY_TTL = 24; // 历史记录保存24小时
//
//    @Autowired
//    private RedisTemplate<String, ChatHistory> redisTemplate;
//
//    public String createNewSession() {
//        String sessionId = UUID.randomUUID().toString();
//        ChatHistory history = new ChatHistory(sessionId);
//        saveHistory(history);
//        return sessionId;
//    }
//
//    public void addMessage(String sessionId, Message message) {
//        ChatHistory history = getHistory(sessionId);
//        if (history == null) {
//            history = new ChatHistory(sessionId);
//        }
//        history.addMessage(message);
//        saveHistory(history);
//    }
//
//    public List<Message> getMessages(String sessionId) {
//        ChatHistory history = getHistory(sessionId);
//        if (history == null) {
//            return List.of();
//        }
//        return history.getMessagesForModel();
//    }
//
//    public ChatHistory getHistory(String sessionId) {
//        return redisTemplate.opsForValue().get(CHAT_HISTORY_KEY_PREFIX + sessionId);
//    }
//
//    private void saveHistory(ChatHistory history) {
//        redisTemplate.opsForValue().set(
//            CHAT_HISTORY_KEY_PREFIX + history.getSessionId(),
//            history,
//            CHAT_HISTORY_TTL,
//            TimeUnit.HOURS
//        );
//    }
//
//    public void clearHistory(String sessionId) {
//        redisTemplate.delete(CHAT_HISTORY_KEY_PREFIX + sessionId);
//    }
//}