package top.knos.mySeek.primary.service;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import top.knos.mySeek.primary.vo.MessageVo;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisChatMemory implements ChatMemory {

    private static final String PREFIX = "chat:";
    private final RedisTemplate<String, MessageVo> redisTemplate;
    private final long ttl ;

    public RedisChatMemory(RedisTemplate<String, MessageVo> messageRedisTemplate) {
        this.redisTemplate = messageRedisTemplate;
        this.ttl = 24*60*60*1000;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {

        if(messages==null|| messages.isEmpty()){
            return;
        }
        List<MessageVo> vos = messages.stream().map(MessageVo::of).toList();

        String key = PREFIX + conversationId;
        BoundListOperations<String, MessageVo> ops = redisTemplate.boundListOps(key);
        vos.forEach(ops::rightPush);
        // 设置过期时间，防止无限增长
        redisTemplate.expire(key, ttl, TimeUnit.MILLISECONDS);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        String key = PREFIX + conversationId;
        Long size = redisTemplate.opsForList().size(key);
        long start = Math.max(0, size - lastN);
        long end = size - 1;
        List<MessageVo> range = redisTemplate.opsForList().range(key, start, end);
        if(range==null||range.isEmpty()){
            return Collections.emptyList();
        }
        return range.stream().map(MessageVo::to).toList();
    }

    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(PREFIX + conversationId);
    }
}
