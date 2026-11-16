package top.knos.mySeek.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import top.knos.mySeek.primary.vo.MessageVo;

/**
 * redis配置类
 *
 * <p>Spring Boot 4 默认使用 Jackson 3（tools.jackson 包），
 * 序列化器已切换为 GenericJacksonJsonRedisSerializer / JacksonJsonRedisSerializer。
 * 默认类型（多态反序列化）必须使用白名单校验器，避免反序列化漏洞。</p>
 *
 * @author knos
 */
@Configuration
@EnableCaching //开启注解
public class RedisConfig {

    /**
     * 业务对象多态反序列化白名单：仅允许本项目包与 JDK 常用类型
     */
    private static PolymorphicTypeValidator typeValidator() {
        return BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("top.knos.mySeek.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.time.")
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator())
                .build();

        template.setValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisTemplate<String, MessageVo> messageRedisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, MessageVo> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        template.setKeySerializer(new StringRedisSerializer());

        JacksonJsonRedisSerializer<MessageVo> serializer = new JacksonJsonRedisSerializer<>(MessageVo.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();

        return template;
    }
}
