package top.knos.mySeek.primary.vo;

import java.util.List;
import java.util.stream.Collectors;


public record NovelOutline(
        String summary,
        List<String> characterSettings,
        List<PartOutline> parts,
        List<String> plotPoints
) {
}
