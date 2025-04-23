package top.knos.mySeek.primary.vo;

import lombok.Data;
import org.springframework.ai.chat.messages.*;

import java.util.Map;

@Data
public class MessageVo {
    private String text;
    private String type;

    public static MessageVo of(Message message) {
        MessageVo vo = new MessageVo();
        vo.text = message.getText();
        vo.type = message.getMessageType().getValue();
        return vo;
    }
    public Message to() {
        Message message = null;
        if(this.type.equals(MessageType.USER.getValue())){
            message = new UserMessage(text);
        }else if(this.type.equals(MessageType.SYSTEM.getValue())){
            message = new SystemMessage(text);
        }else if(this.type.equals(MessageType.ASSISTANT.getValue())){
            message = new AssistantMessage(text);
        }else {
            message = new Message() {
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
            };
        }
        return message;
    }
}
