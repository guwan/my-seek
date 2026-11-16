package top.knos.mySeek.primary.vo;

import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 聊天消息传输对象。
 * <p>字段与前端契约保持一致：{@code id / role / content}。</p>
 */
@Data
public class MessageVo {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(System.currentTimeMillis());

    private Long id;
    /** user / assistant / system / tool */
    private String role;
    private String content;
    private Date createAt;

    public static MessageVo of(Message message) {
        MessageVo vo = new MessageVo();
        vo.id = ID_GENERATOR.incrementAndGet();
        vo.content = message.getText();
        vo.role = message.getMessageType().getValue();
        vo.setCreateAt(new Date());
        return vo;
    }

    public Message to() {
        String type = role == null ? MessageType.USER.getValue() : role;
        if (MessageType.SYSTEM.getValue().equals(type)) {
            return new SystemMessage(content);
        }
        if (MessageType.ASSISTANT.getValue().equals(type)) {
            return new AssistantMessage(content);
        }
        if (MessageType.TOOL.getValue().equals(type)) {
            return new ToolMessageStub(content);
        }
        return new UserMessage(content);
    }

    /**
     * 工具消息：Spring AI 2.0 起 ToolResponseMessage 需要 ToolResponse 上下文，这里用最小实现兜底。
     */
    private static final class ToolMessageStub implements Message {

        private final String text;

        private ToolMessageStub(String text) {
            this.text = text;
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.TOOL;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return Map.of();
        }
    }
}
