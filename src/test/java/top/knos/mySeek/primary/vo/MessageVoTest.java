package top.knos.mySeek.primary.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MessageVoTest {

    @Test
    @DisplayName("User message survives the of/to conversion")
    void userMessageRoundTrip() {
        MessageVo vo = MessageVo.of(new UserMessage("hello"));

        assertEquals("user", vo.getRole());
        assertEquals("hello", vo.getContent());
        assertNotNull(vo.getId());

        Message back = vo.to();
        assertEquals(MessageType.USER, back.getMessageType());
        assertEquals("hello", back.getText());
    }

    @Test
    @DisplayName("Assistant and system messages survive the of/to conversion")
    void otherRolesRoundTrip() {
        MessageVo assistant = MessageVo.of(new AssistantMessage("hi"));
        assertEquals("assistant", assistant.getRole());
        assertEquals(MessageType.ASSISTANT, assistant.to().getMessageType());

        MessageVo system = MessageVo.of(new SystemMessage("be nice"));
        assertEquals("system", system.getRole());
        assertEquals(MessageType.SYSTEM, system.to().getMessageType());
    }
}
