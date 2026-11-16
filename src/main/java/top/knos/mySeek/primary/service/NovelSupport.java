package top.knos.mySeek.primary.service;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import top.knos.mySeek.exception.InvalidOutlineException;
import top.knos.mySeek.exception.NovelGenerationException;
import top.knos.mySeek.primary.vo.ChatRequest;
import top.knos.mySeek.primary.vo.NovelOutline;
import top.knos.mySeek.primary.vo.PartOutline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class NovelSupport {
    private final NovelStorageService storageService;
    private final ChatService chatService;// 新增配置参数

    public NovelSupport(NovelStorageService storageService, ChatService chatService) {
        this.storageService = storageService;
        this.chatService = chatService;
    }

    public String chat(String conversationId,String prompt ){
        ChatRequest request = new ChatRequest(conversationId,prompt);
        return chatService.chat(request);
    }


    // 预处理包含思考内容的多段式响应
    public String preprocessResponse(String rawResponse) {
        // 1. 移除XML格式的思考内容
        String cleaned = rawResponse
                .replaceAll("<think>.*?</think>", "")  // 移除整个思考段落
                .replaceAll("(?i)<!--.*?-->", "")     // 移除可能的注释
                .replaceAll("^[^\\{]*", "")           // 移除JSON前的非结构内容
                .trim();

        // 2. 处理多代码块情况
        if (cleaned.contains("```")) {
            // 取最后一个代码块（通常AI会把正确响应放在最后）
            String[] blocks = cleaned.split("```");
            if (blocks.length % 2 == 1) {
                cleaned = blocks[blocks.length - 1];
            }
        }
        // 新增字符替换（处理中文符号问题）
//        cleaned = cleaned
//                .replaceAll("[\"\"]", "\"")   // 替换中文双引号
//                .replaceAll("[\u2018\u2019]", "'")    // 替换中文单引号
//                .replaceAll("\\\\\\\"", "\"") // 处理转义引号
//                .replaceAll("\uff1a", ":")       // 替换中文冒号
//                .replaceAll("\uff0c", ",")      // 替换中文逗号
//                .replaceAll("\uff1b", ";");     // 替换中文分号

        return cleaned;
    }

    // 增强版JSON提取
    public String extractJsonFromMarkdown(String content) {
        // 处理带语言标识的代码块
        String json = content
                .replaceAll("(?i)```json\\s*", "")
                .replaceAll("```", "")
                .replaceAll("^\\s*", "")
                .replaceAll("\\s*$", "");

        // 智能定位JSON起始点
        int jsonStart = json.indexOf('{');
        int jsonEnd = json.lastIndexOf('}') + 1;

        if (jsonStart == -1 || jsonEnd == 0) {
            throw new NovelGenerationException("未找到有效JSON结构");
        }
        // 新增换行符修复逻辑
        json = fixJsonFormat(json);
        // 修复多余逗号（分步骤处理）
        json = fixTrailingCommas(json);
        jsonStart = json.indexOf('{');
        jsonEnd = json.lastIndexOf('}') + 1;
        return json.substring(jsonStart, jsonEnd);
    }
    /**
     * 修复JSON中的非法换行符（专门处理字符串值内的换行）
     */
    private String fixJsonFormat(String json) {
        // 匹配 "content": "..." 结构（包括可能的多行内容）
        Pattern pattern = Pattern.compile(
                "\"content\"\\s*:\\s*\"(.*?)\"",
                Pattern.DOTALL // 允许.匹配换行符
        );

        Matcher matcher = pattern.matcher(json);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            // 转义换行符、双引号等
            content = content.replace("\\", "\\\\")  // 先转义反斜杠
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
            matcher.appendReplacement(sb, "\"content\": \"" + content + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    /**
     * 专门处理结尾多余逗号
     */
    private String fixTrailingCommas(String json) {
        // 使用前瞻断言确保只处理结构逗号
        String regex = "(?<!\\\\)\"\\s*,\\s*(?=\\s*[]}])";
        json = json.replaceAll(regex, "\"");

        // 处理数组情况
        json = json.replaceAll(",(\\s*])", "$1");
        // 处理对象情况
        json = json.replaceAll(",(\\s*})", "$1");

        return json;
    }

    public NovelOutline parseValidatedJson(String json, int totalChapters)
            throws JacksonException, InvalidOutlineException {

        // Jackson 3 已内置 java.time 支持，无需额外注册 JavaTimeModule
        JsonMapper mapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();

        JsonNode root = mapper.readTree(json);

        // 验证基础结构
        validatePresence(root, "summary", "characterSettings", "parts");

        // 解析parts并合并章节
        List<PartOutline> parts = mapper.readValue(
                root.path("parts").toString(),
                new TypeReference<List<PartOutline>>(){}
        );
        // 自动提取全局plotPoints
        List<String> globalPlotPoints = new ArrayList<>();
        parts.forEach(part ->
                part.chapters().forEach(chapter ->
                        globalPlotPoints.addAll(chapter.plotPoints())
                )
        );
        // 验证章节总数
        int actualChapters = parts.stream()
                .mapToInt(p -> p.chapters().size())
                .sum();

        /*if (actualChapters != totalChapters) {
            throw new InvalidOutlineException(
                    "章节数不符 预期:%d 实际:%d".formatted(totalChapters, actualChapters)
            );
        }*/

        // 验证故事结构
        validateStoryStructure(parts, totalChapters);

        return new NovelOutline(
                root.path("summary").asText(),
                mapper.convertValue(
                        root.path("characterSettings"),
                        new TypeReference<List<String>>(){}),
                parts,
                globalPlotPoints // 合并所有章节情节点
        );
    }

    // 动态故事结构验证
    private void validateStoryStructure(List<PartOutline> parts, int total) {
        int[] structurePoints = {
                (int)(total * 0.25),  // 第一幕结束
                (int)(total * 0.5),   // 中点转折
                (int)(total * 0.75)   // 高潮铺垫
        };

//        List<String> allChapters = parts.stream()
//                .flatMap(p -> p.chapters().stream()) // 现在chapters是List<String>，可以正确调用stream()
//                .toList();

//        validateChapter(allChapters.get(structurePoints[0]), "第一幕应完成世界观构建");
//        validateChapter(allChapters.get(structurePoints[1]), "中点应出现重大转折");
//        validateChapter(allChapters.get(structurePoints[2]), "第三幕应开始高潮铺垫");
    }


    private void validatePresence(JsonNode node, String... fields) {
        List<String> missing = Arrays.stream(fields)
                .filter(f -> !node.has(f))
                .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            throw new NovelGenerationException("缺少必要字段: " + missing);
        }
    }
}
