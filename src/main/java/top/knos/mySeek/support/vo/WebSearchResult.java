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
    /** 是否实际执行过搜索（用于前端提示"已联网搜索"） */
    private boolean performed = false;
    /** 实际使用的搜索引擎 */
    private String engine = "";

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

    /** 序列化为流式首帧元数据，供前端展示联网状态与来源 */
    public String getMetadataJson() {
        try {
            return MAPPER.writeValueAsString(new Metadata(performed, engine, sources));
        } catch (JsonProcessingException e) {
            return "{\"performed\":false,\"sources\":[]}";
        }
    }

    /** 兼容旧方法名 */
    public String getSourcesJson() {
        return getMetadataJson();
    }

    public boolean hasSources() {
        return sources != null && !sources.isEmpty();
    }

    private record Metadata(boolean performed, String engine, List<SearchSource> sources) {
    }
}
