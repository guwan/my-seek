package top.knos.mySeek.primary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 联网搜索服务：为对话提供实时外部信息。
 *
 * <p>支持两种提供方，按优先级自动降级，且均使用 Spring Web 自带的 {@link RestClient}，
 * 无需新增任何依赖：</p>
 * <ul>
 *   <li>tavily：AI 场景专用搜索，质量高，需配置 API Key（有免费额度）；</li>
 *   <li>wikipedia：免 Key 兜底，适合事实/百科类问题。</li>
 * </ul>
 * <p>任何异常都会被吞掉并返回空串，保证搜索失败时对话仍可正常进行。</p>
 */
@Service
@Slf4j
public class WebSearchService {

    private static final int CONTEXT_MAX_CHARS = 4000;

    private final RestClient restClient;
    /** 独立实例，避免依赖 Spring Boot 4 中不再默认提供的 ObjectMapper bean */
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String provider;
    private final String tavilyApiKey;
    private final String tavilyBaseUrl;
    private final int maxResults;

    public WebSearchService(
            @Value("${web-search.provider:tavily}") String provider,
            @Value("${web-search.tavily.api-key:}") String tavilyApiKey,
            @Value("${web-search.tavily.base-url:https://api.tavily.com}") String tavilyBaseUrl,
            @Value("${web-search.max-results:5}") int maxResults) {
        this.restClient = RestClient.create();
        this.provider = provider;
        this.tavilyApiKey = tavilyApiKey;
        this.tavilyBaseUrl = tavilyBaseUrl;
        this.maxResults = maxResults;
    }

    /**
     * 搜索并整理为可直接注入 prompt 的参考资料文本。
     *
     * @return 参考资料文本；无结果或失败时返回空串
     */
    public String search(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        String trimmed = query.trim();
        try {
            List<String> snippets = new ArrayList<>();
            if ("tavily".equalsIgnoreCase(provider) && tavilyApiKey != null && !tavilyApiKey.isBlank()) {
                snippets = searchTavily(trimmed);
            }
            if (snippets.isEmpty()) {
                snippets = searchWikipedia(trimmed);
            }
            return toContext(snippets);
        } catch (Exception e) {
            log.warn("联网搜索失败，本次对话不使用外部资料: {}", e.getMessage());
            return "";
        }
    }

    /** Tavily：POST JSON，返回 results[].title/content/url */
    private List<String> searchTavily(String query) {
        String body = """
                {"api_key":"%s","query":"%s","search_depth":"basic","max_results":%d,"include_answer":false}
                """.formatted(escape(tavilyApiKey), escape(query), maxResults);

        String json = restClient.post()
                .uri(tavilyBaseUrl + "/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        if (json == null) return List.of();

        JsonNode root = readJson(json);
        if (root == null) return List.of();
        List<String> out = new ArrayList<>();
        JsonNode results = root.path("results");
        if (results.isArray()) {
            for (JsonNode item : results) {
                String title = item.path("title").asText("");
                String content = item.path("content").asText("");
                String url = item.path("url").asText("");
                out.add(String.format("【%s】%s（来源：%s）", title, content, url));
            }
        }
        return out;
    }

    /** Wikipedia：GET JSON，免 API Key，返回 query.search[].title/snippet */
    private List<String> searchWikipedia(String query) {
        String url = "https://en.wikipedia.org/w/api.php"
                + "?action=query&list=search&format=json&srlimit=" + maxResults
                + "&srsearch=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        String json = restClient.get().uri(url).retrieve().body(String.class);
        if (json == null) return List.of();

        JsonNode root = readJson(json);
        if (root == null) return List.of();
        List<String> out = new ArrayList<>();
        JsonNode results = root.path("query").path("search");
        if (results.isArray()) {
            for (JsonNode item : results) {
                String title = item.path("title").asText("");
                String snippet = item.path("snippet").asText("").replaceAll("<[^>]+>", "");
                out.add(String.format("【%s】%s（来源：Wikipedia）", title, snippet));
            }
        }
        return out;
    }

    /** 拼接并截断，避免占用过多上下文窗口 */
    private String toContext(List<String> snippets) {
        if (snippets.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("以下是联网搜索到的实时资料，仅供参考，回答时请甄别其准确性：\n");
        for (String s : snippets) {
            if (sb.length() + s.length() > CONTEXT_MAX_CHARS) break;
            sb.append("- ").append(s).append('\n');
        }
        return sb.toString().trim();
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("联网搜索结果解析失败: {}", e.getMessage());
            return null;
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
