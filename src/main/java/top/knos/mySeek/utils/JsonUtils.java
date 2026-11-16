package top.knos.mySeek.utils;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.io.JsonStringEncoder;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * JSON 工具类（基于 Spring Boot 4 默认的 Jackson 3）
 */
@Slf4j
public class JsonUtils {

    /** 线程安全的 ObjectMapper 实例 */
    public static final ObjectMapper mapper = JsonMapper.builder().build();

    /**
     * 将对象转换成json字符串。
     */
    public static String objectToJson(Object data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (JacksonException e) {
            log.error("对象序列化为 JSON 失败", e);
            throw new IllegalStateException("对象序列化为 JSON 失败", e);
        }
    }

    /**
     * 解析 json 字符串为 JsonNode
     */
    public static JsonNode strToJson(String data) {
        try {
            return mapper.readTree(data);
        } catch (JacksonException e) {
            log.error("JSON 解析失败", e);
            throw new IllegalStateException("JSON 解析失败", e);
        }
    }

    /**
     * 将 JsonNode 转换为指定类型的对象
     */
    public static <T> T JsonNodeToObject(JsonNode jsonNode, Class<T> beanType) {
        try {
            return mapper.readValue(jsonNode.toString(), beanType);
        } catch (JacksonException e) {
            log.error("JsonNode 转换为对象失败: {}", beanType.getName(), e);
            throw new IllegalStateException("JsonNode 转换为对象失败", e);
        }
    }

    /**
     * 将json结果集转化为对象
     */
    public static <T> T jsonToPojo(String jsonData, Class<T> beanType) {
        try {
            return mapper.readValue(jsonData, beanType);
        } catch (JacksonException e) {
            log.error("JSON 反序列化失败: {}", beanType.getName(), e);
            return null;
        }
    }

    /**
     * 将json数据转换成pojo对象list
     */
    public static <T> List<T> jsonToList(String jsonData, Class<T> beanType) {
        JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, beanType);
        try {
            return mapper.readValue(jsonData, javaType);
        } catch (JacksonException e) {
            log.error("JSON 反序列化为列表失败: {}", beanType.getName(), e);
            return List.of();
        }
    }

    /**
     * 对不合法的json转义
     */
    public static String escapeJsonWithJackson(String input) {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder escaped = new StringBuilder();
        encoder.quoteAsString(input, escaped);
        return escaped.toString();
    }
}
