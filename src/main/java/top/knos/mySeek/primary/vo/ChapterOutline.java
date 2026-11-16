package top.knos.mySeek.primary.vo;

import java.util.List;

public record ChapterOutline(
        String title,
        List<String> plotPoints, // 本章关键情节点
        List<String> foreshadowings // 本章埋设的伏笔
) {}
