package top.knos.mySeek.support.vo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.List;

/**
 * 联网搜索结果：context 用于注入模型 prompt，sources 用于前端展示参考来源。
 */
@Data
public class WebSearchResult {

    private String context = "";
    private List<SearchSource> sources = List.of();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Data
    public static class SearchSource {
        private String title;
        private String url;

        public SearchSource() {
        }

        public SearchSource(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }

    /** 序列化来源列表为 JSON，供 ChatService 组装流式首帧 */
    public String getSourcesJson() {
        try {
            return MAPPER.writeValueAsString(sources);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public boolean hasSources() {
        return sources != null && !sources.isEmpty();
    }
}
