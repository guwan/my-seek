package top.knos.mySeek.writing.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.knos.mySeek.exception.CustomHttpException;
import top.knos.mySeek.exception.TipsException;
import top.knos.mySeek.writing.entity.WritingNovel;
import top.knos.mySeek.writing.repository.WritingNovelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 写作作品持久化：按用户隔离的作品 JSON 存取。
 */
@Service
@RequiredArgsConstructor
public class WritingService {

    private final WritingNovelRepository repository;
    /** Spring Boot 4 使用 Jackson 3，HTTP 转换器与 JSON 类型必须保持一致 */
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Transactional(readOnly = true)
    public List<JsonNode> listNovels(Long userId) {
        List<JsonNode> novels = new ArrayList<>();
        for (WritingNovel entity : repository.findByUserIdOrderByCreatedAtDesc(userId)) {
            try {
                novels.add(objectMapper.readTree(entity.getContentJson()));
            } catch (JacksonException e) {
                // 单条脏数据不应导致整个作品列表不可用
                novels.add(objectMapper.getNodeFactory().objectNode()
                        .put("id", entity.getId())
                        .put("title", entity.getTitle()));
            }
        }
        return novels;
    }

    @Transactional
    public JsonNode saveNovel(Long userId, JsonNode payload) {
        String id = requireId(payload);
        WritingNovel entity = repository.findById(id).orElse(null);
        if (entity != null && !Objects.equals(entity.getUserId(), userId)) {
            throw new AccessDeniedException("无权修改该作品");
        }

        String json = serialize(payload);
        JsonNode titleNode = payload.get("title");
        String title = titleNode != null && titleNode.isTextual() && !titleNode.asText().isBlank()
                ? titleNode.asText()
                : "未命名作品";

        if (entity == null) {
            entity = new WritingNovel();
            entity.setId(id);
            entity.setUserId(userId);
        }
        entity.setTitle(title);
        entity.setContentJson(json);
        repository.save(entity);
        return payload;
    }

    @Transactional
    public void deleteNovel(Long userId, String novelId) {
        WritingNovel entity = repository.findById(novelId).orElse(null);
        if (entity == null) {
            return;
        }
        if (!Objects.equals(entity.getUserId(), userId)) {
            throw new AccessDeniedException("无权删除该作品");
        }
        repository.delete(entity);
    }

    private String requireId(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new TipsException("作品数据格式错误", HttpStatus.BAD_REQUEST.value());
        }
        JsonNode idNode = payload.get("id");
        if (idNode == null || !idNode.isTextual() || idNode.asText().isBlank()) {
            throw new TipsException("作品缺少有效 id", HttpStatus.BAD_REQUEST.value());
        }
        return idNode.asText();
    }

    private String serialize(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new TipsException("作品数据无法保存", HttpStatus.BAD_REQUEST.value());
        }
    }

    /** 未登录时给出明确业务码，避免写出 user_id 为 null 的脏数据。 */
    public static Long requireUserId(Long userId) {
        if (userId == null) {
            throw new CustomHttpException("未登录或登录已过期", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }
}
