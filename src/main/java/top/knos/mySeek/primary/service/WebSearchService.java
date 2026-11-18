package top.knos.mySeek.primary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import top.knos.mySeek.support.vo.WebSearchResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 联网搜索服务：为对话提供实时外部信息。
 *
 * <p>搜索源按优先级自动降级，均使用 Spring Web 自带的 {@link RestClient}，无需新增依赖：</p>
 * <ol>
 *   <li>tavily：AI 场景专用搜索，质量高，需配置 API Key（有免费额度）；</li>
 *   <li>Wikipedia：免 Key 兜底，会根据查询语言自动选择中文/英文站点。</li>
 * </ol>
 * <p>任何异常都会被吞掉，但会保留 performed 标记，让前端能明确提示"已尝试联网搜索"。</p>
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
     * 搜索并返回结构化结果：context 注入 prompt，sources 供前端展示，performed 供前端提示。
     */
    public WebSearchResult search(String query) {
        WebSearchResult result = new WebSearchResult();
        if (query == null || query.isBlank()) {
            return result;
        }
        String trimmed = query.trim();
        result.setPerformed(true);
        try {
            List<WebSearchResult.SearchSource> sources = new ArrayList<>();
            if ("tavily".equalsIgnoreCase(provider) && tavilyApiKey != null && !tavilyApiKey.isBlank()) {
                sources = searchTavily(trimmed);
                if (!sources.isEmpty()) result.setEngine("Tavily");
            }
            // Tavily 未配置或无结果时降级到 Wikipedia，按查询语言选择中文/英文站
            if (sources.isEmpty()) {
                sources = searchWikipedia(trimmed, containsChinese(trimmed));
                if (!sources.isEmpty()) {
                    result.setEngine(containsChinese(trimmed) ? "Wikipedia 中文" : "Wikipedia");
                }
            }
            result.setSources(sources);
            result.setContext(toContext(sources));
            if (result.getEngine().isEmpty()) {
                result.setEngine("无可用结果");
            }
        } catch (Exception e) {
            log.warn("联网搜索失败，本次对话不使用外部资料: {}", e.getMessage());
            result.setEngine("搜索失败");
        }
        return result;
    }

    /** Tavily：POST JSON，返回 results[].title/content/url */
    private List<WebSearchResult.SearchSource> searchTavily(String query) {
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
        List<WebSearchResult.SearchSource> out = new ArrayList<>();
        JsonNode results = root.path("results");
        if (results.isArray()) {
            for (JsonNode item : results) {
                String title = item.path("title").asText("");
                String url = item.path("url").asText("");
                String content = item.path("content").asText("");
                if (!title.isBlank() || !url.isBlank()) {
                    out.add(new WebSearchResult.SearchSource(
                            title.isBlank() ? url : title + (content.isBlank() ? "" : "：" + abbreviate(content)),
                            url));
                }
            }
        }
        return out;
    }

    /** Wikipedia：GET JSON，免 API Key，根据语言选择中文或英文站点 */
    private List<WebSearchResult.SearchSource> searchWikipedia(String query, boolean chinese) {
        String host = chinese ? "https://zh.wikipedia.org" : "https://en.wikipedia.org";
        String url = host + "/w/api.php"
                + "?action=query&list=search&format=json&srlimit=" + maxResults
                + "&srsearch=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        String json = restClient.get().uri(url).retrieve().body(String.class);
        if (json == null) return List.of();

        JsonNode root = readJson(json);
        if (root == null) return List.of();
        List<WebSearchResult.SearchSource> out = new ArrayList<>();
        JsonNode results = root.path("query").path("search");
        if (results.isArray()) {
            for (JsonNode item : results) {
                String title = item.path("title").asText("");
                String snippet = item.path("snippet").asText("").replaceAll("<[^>]+>", "");
                String pageUrl = host + "/wiki/" + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8);
                out.add(new WebSearchResult.SearchSource(
                        title + (snippet.isBlank() ? "" : "：" + abbreviate(snippet)),
                        pageUrl));
            }
        }
        return out;
    }

    /** 拼接并截断，避免占用过多上下文窗口 */
    private String toContext(List<WebSearchResult.SearchSource> sources) {
        if (sources.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("以下是联网搜索到的实时资料，仅供参考，回答时请甄别其准确性：\n");
        for (WebSearchResult.SearchSource s : sources) {
            String line = String.format("- %s（来源：%s）", s.getTitle(), s.getUrl());
            if (sb.length() + line.length() > CONTEXT_MAX_CHARS) break;
            sb.append(line).append('\n');
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

    private static boolean containsChinese(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x4e00 && c <= 0x9fff) return true;
        }
        return false;
    }

    private static String abbreviate(String s) {
        String clean = s.replaceAll("\\s+", " ").trim();
        return clean.length() > 120 ? clean.substring(0, 120) + "…" : clean;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
