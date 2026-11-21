package top.knos.mySeek.primary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import top.knos.mySeek.primary.vo.MessageVo;
import top.knos.mySeek.support.vo.ChatInfoVo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的聊天记忆实现。
 * <p>会话 ID 已在上层（ChatService）按用户做过隔离，因此这里直接以 conversationId 作为 key 维度。</p>
 */
@Service
@Slf4j
public class RedisChatMemory implements ChatMemory {

    private static final String PREFIX = "chat:";
    /** 会话列表：key = chat:items:{userScope}，field = conversationId，value = ChatInfoVo */
    private static final String ITEM_PREFIX = "chat:items:";
    /** 送入模型的上下文消息条数上限 */
    private static final int HISTORY_LIMIT = 50;

    private final RedisTemplate<String, MessageVo> redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final long ttl;

    public RedisChatMemory(RedisTemplate<String, MessageVo> messageRedisTemplate,
                           RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = messageRedisTemplate;
        this.objectRedisTemplate = redisTemplate;
        this.ttl = 24 * 60 * 60 * 1000L;
    }

    @Override
    public void add(String conversationId, Message message) {
        add(conversationId, List.of(message));
    }

    @Override
    public void add(String conversationId, List<Message> messages) {

        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<MessageVo> vos = messages.stream().map(MessageVo::of).toList();

        String key = key(conversationId);
        BoundListOperations<String, MessageVo> ops = redisTemplate.boundListOps(key);
        vos.forEach(ops::rightPush);
        // 设置过期时间，防止无限增长
        redisTemplate.expire(key, ttl, TimeUnit.MILLISECONDS);
    }

    @Override
    public List<Message> get(String conversationId) {
        List<MessageVo> range = range(conversationId, HISTORY_LIMIT);
        if (range.isEmpty()) {
            return Collections.emptyList();
        }
        return range.stream().map(MessageVo::to).toList();
    }

    public List<MessageVo> getVo(String conversationId, int lastN) {
        return range(conversationId, lastN);
    }

    public MessageVo getFirstOneVo(String conversationId) {
        List<MessageVo> range = redisTemplate.opsForList().range(key(conversationId), 0, 0);
        if (range == null || range.isEmpty()) {
            return null;
        }
        return range.getFirst();
    }

    public Boolean chatExist(String conversationId) {
        return redisTemplate.hasKey(key(conversationId));
    }

    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(key(conversationId));
    }

    /**
     * 保存个人会话列表条目。
     */
    public void addChatItem(String userScope, ChatInfoVo chatInfoVo) {
        if (userScope == null || chatInfoVo == null || chatInfoVo.getId() == null) {
            return;
        }
        String key = ITEM_PREFIX + userScope;
        objectRedisTemplate.opsForHash().put(key, chatInfoVo.getId(), chatInfoVo);
        objectRedisTemplate.expire(key, ttl, TimeUnit.MILLISECONDS);
    }

    /**
     * 查询个人会话列表，按创建时间倒序。
     */
    public List<ChatInfoVo> getChatItems(String userScope) {
        if (userScope == null) {
            return Collections.emptyList();
        }
        Map<Object, Object> entries = objectRedisTemplate.opsForHash().entries(ITEM_PREFIX + userScope);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        return entries.values().stream()
                .filter(ChatInfoVo.class::isInstance)
                .map(ChatInfoVo.class::cast)
                .sorted(Comparator.comparing(ChatInfoVo::getCreateAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * 删除个人会话列表条目。
     */
    public void removeChatItem(String userScope, String conversationId) {
        if (userScope == null || conversationId == null) {
            return;
        }
        objectRedisTemplate.opsForHash().delete(ITEM_PREFIX + userScope, conversationId);
    }

    /**
     * 清空指定用户的全部会话记忆。按 userScope 前缀 SCAN，包含尚未登记到列表的孤立记忆。
     */
    public void clearConversations(String userScope) {
        if (userScope == null || userScope.isBlank()) {
            return;
        }
        String pattern = PREFIX + userScope + ":*";
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
                .match(pattern)
                .count(500)
                .build())) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } catch (Exception e) {
            log.warn("扫描用户会话记忆失败 userScope={} pattern={}", userScope, pattern, e);
            return;
        }
        if (keys.isEmpty()) {
            return;
        }
        for (int i = 0; i < keys.size(); i += 200) {
            redisTemplate.delete(new ArrayList<>(keys.subList(i, Math.min(i + 200, keys.size()))));
        }
        log.info("清空会话记忆 userScope={} count={}", userScope, keys.size());
    }

    /**
     * 清空指定用户的会话列表。
     */
    public void clearChatItems(String userScope) {
        if (userScope == null || userScope.isBlank()) {
            return;
        }
        objectRedisTemplate.delete(ITEM_PREFIX + userScope);
    }

    /**
     * 读取最近 N 条消息（不足则全量返回）。
     */
    private List<MessageVo> range(String conversationId, int lastN) {
        String key = key(conversationId);
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return Collections.emptyList();
        }
        long start = Math.max(0, size - lastN);
        long end = size - 1;
        List<MessageVo> range = redisTemplate.opsForList().range(key, start, end);
        if (range == null || range.isEmpty()) {
            return Collections.emptyList();
        }
        return range;
    }

    private String key(String conversationId) {
        return PREFIX + conversationId;
    }
}
