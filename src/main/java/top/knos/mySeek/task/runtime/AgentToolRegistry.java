package top.knos.mySeek.task.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import top.knos.mySeek.primary.service.WebSearchService;
import top.knos.mySeek.rag.entity.KnowledgeDocument;
import top.knos.mySeek.rag.service.KnowledgeBaseService;
import top.knos.mySeek.writing.service.WritingService;

import java.util.List;
import java.util.UUID;

/**
 * AI 任务中心一期工具白名单。所有工具都强制携带 userId，禁止 Shell 与任意文件访问。
 */
@Slf4j
@Component
public class AgentToolRegistry {

    public static final List<String> ALLOWED_TOOLS = List.of(
            "knowledge_search", "knowledge_list", "novel_read", "novel_write_draft", "web_search");

    private final KnowledgeBaseService knowledgeBaseService;
    private final WritingService writingService;
    private final WebSearchService webSearchService;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public AgentToolRegistry(KnowledgeBaseService knowledgeBaseService,
                             WritingService writingService,
                             WebSearchService webSearchService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.writingService = writingService;
        this.webSearchService = webSearchService;
    }

    /** 写操作默认需要用户批准，查询类工具自动执行。 */
    public boolean isSensitive(String tool) {
        return "novel_write_draft".equals(tool);
    }

    public String execute(String tool, Long userId, String inputJson, String model) {
        if (tool == null || tool.isBlank() || tool.equals("model")) {
            throw new IllegalArgumentException("该步骤不需要工具");
        }
        if (!ALLOWED_TOOLS.contains(tool)) {
            throw new IllegalArgumentException("工具不在白名单中: " + tool);
        }
        JsonNode args = parseObject(inputJson);
        return switch (tool) {
            case "knowledge_search" -> knowledgeSearch(userId, args, model);
            case "knowledge_list" -> knowledgeList(userId);
            case "novel_read" -> novelRead(userId, args);
            case "novel_write_draft" -> novelWriteDraft(userId, args);
            case "web_search" -> webSearch(args);
            default -> throw new IllegalArgumentException("未实现的工具: " + tool);
        };
    }

    private String knowledgeSearch(Long userId, JsonNode args, String model) {
        String query = text(args, "query", "");
        if (query.isBlank()) throw new IllegalArgumentException("knowledge_search 缺少 query");
        int topK = Math.min(10, Math.max(1, intValue(args, "topK", 5)));
        String answer = knowledgeBaseService.answerBasedOnKnowledge(userId, query, topK, model);
        return answer == null ? "未检索到与问题相关的知识库资料" : answer;
    }

    private String knowledgeList(Long userId) {
        List<KnowledgeDocument> docs = knowledgeBaseService.getCompletedDocuments(userId);
        if (docs.isEmpty()) return "当前用户没有已处理的知识库文档";
        StringBuilder sb = new StringBuilder("已完成文档：\n");
        for (KnowledgeDocument doc : docs) {
            sb.append("- ").append(doc.getOriginalName()).append(" (").append(doc.getId()).append(")\n");
        }
        return sb.toString();
    }

    private String novelRead(Long userId, JsonNode args) {
        String keyword = text(args, "keyword", "");
        List<JsonNode> novels = writingService.listNovels(userId);
        List<JsonNode> matched = novels.stream()
                .filter(n -> keyword.isBlank() || n.path("title").asString("").contains(keyword))
                .limit(3)
                .toList();
        if (matched.isEmpty()) return "没有匹配的写作作品";
        try {
            return truncate(objectMapper.writeValueAsString(matched));
        } catch (Exception e) {
            throw new IllegalStateException("作品内容读取失败: " + e.getMessage(), e);
        }
    }

    private String novelWriteDraft(Long userId, JsonNode args) {
        String id = text(args, "id", UUID.randomUUID().toString());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("id", id);
        payload.put("title", text(args, "title", "AI 任务草稿"));
        payload.put("content", text(args, "content", ""));
        payload.put("source", "ai-task");
        payload.put("draft", true);
        writingService.saveNovel(userId, payload);
        return "草稿已保存，作品 id=" + id;
    }

    private String webSearch(JsonNode args) {
        String query = text(args, "query", "");
        if (query.isBlank()) throw new IllegalArgumentException("web_search 缺少 query");
        String context = webSearchService.search(query).getContext();
        return context == null || context.isBlank() ? "联网搜索未找到可用资料" : truncate(context);
    }

    private JsonNode parseObject(String inputJson) {
        if (inputJson == null || inputJson.isBlank()) return objectMapper.createObjectNode();
        try {
            JsonNode node = objectMapper.readTree(inputJson);
            return node != null && node.isObject() ? node : objectMapper.createObjectNode();
        } catch (Exception e) {
            throw new IllegalArgumentException("工具参数不是合法 JSON: " + e.getMessage(), e);
        }
    }

    private static String text(JsonNode node, String key, String fallback) {
        JsonNode value = node == null ? null : node.get(key);
        if (value == null || value.isNull() || !value.isValueNode()) return fallback;
        String s = value.asString();
        return s == null || s.isBlank() ? fallback : s;
    }

    private static int intValue(JsonNode node, String key, int fallback) {
        JsonNode value = node == null ? null : node.get(key);
        if (value == null || !value.isNumber()) return fallback;
        return value.intValue();
    }

    private static String truncate(String text) {
        return text.length() > 8000 ? text.substring(0, 8000) + "…（已截断）" : text;
    }
}
