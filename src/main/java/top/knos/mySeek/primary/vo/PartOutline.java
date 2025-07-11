package top.knos.mySeek.primary.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// 新增PartOutline.java
public record PartOutline(
        @JsonProperty("partTitle") String partTitle,
        @JsonProperty("partSummary") String partSummary, // 明确JSON字段映射
        @JsonProperty("chapters") List<ChapterOutline> chapters
) {}