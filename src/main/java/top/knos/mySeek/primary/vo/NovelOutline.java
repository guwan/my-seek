package top.knos.mySeek.primary.vo;

import java.util.List;


public record NovelOutline(
        String summary,
        List<String> characterSettings,
        List<PartOutline> parts,
        List<String> plotPoints
) {
}
