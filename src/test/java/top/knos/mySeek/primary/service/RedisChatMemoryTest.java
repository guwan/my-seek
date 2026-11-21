package top.knos.mySeek.primary.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import top.knos.mySeek.primary.vo.MessageVo;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisChatMemoryTest {

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, MessageVo> messageTemplate() {
        return mock(RedisTemplate.class);
    }

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> objectTemplate() {
        return mock(RedisTemplate.class);
    }

    @SuppressWarnings("unchecked")
    private static Cursor<String> cursorWithTwoKeys() {
        Cursor<String> cursor = mock(Cursor.class);
        AtomicInteger index = new AtomicInteger();
        when(cursor.hasNext()).thenAnswer(invocation -> index.get() < 2);
        when(cursor.next()).thenAnswer(invocation -> "chat:7:" + (index.getAndIncrement() + 1));
        return cursor;
    }

    @Test
    void clearConversationsScansUserPrefixAndDeletesAllMatchingKeys() {
        RedisTemplate<String, MessageVo> messages = messageTemplate();
        RedisTemplate<String, Object> objects = objectTemplate();
        Cursor<String> cursor = cursorWithTwoKeys();
        when(messages.scan(any(ScanOptions.class))).thenReturn(cursor);

        new RedisChatMemory(messages, objects).clearConversations("7");

        verify(messages).delete(List.of("chat:7:1", "chat:7:2"));
        verify(cursor).close();
    }

    @Test
    void clearChatItemsDeletesOnlyCurrentUserHash() {
        RedisTemplate<String, MessageVo> messages = messageTemplate();
        RedisTemplate<String, Object> objects = objectTemplate();

        new RedisChatMemory(messages, objects).clearChatItems("7");

        verify(objects).delete("chat:items:7");
    }

    @Test
    void clearConversationsIgnoresBlankScope() {
        RedisTemplate<String, MessageVo> messages = messageTemplate();
        RedisTemplate<String, Object> objects = objectTemplate();

        new RedisChatMemory(messages, objects).clearConversations(" ");

        verify(messages, org.mockito.Mockito.never()).scan(any(ScanOptions.class));
    }
}
