package top.knos.mySeek.primary.vo;

import lombok.Data;

import java.util.List;

/** 返回给前端的 AI 接口信息（apiKey 用掩码显示，不泄露原文） */
@Data
public class AiProviderVo {
    private String id;
    private String name;
    private String displayName;
    private String baseUrl;
    /** 掩码后的 api-key，如 sk-****abcd */
    private String apiKeyMasked;
    private boolean apiKeyConfigured;
    private List<String> models;
    private boolean enabled;
    private boolean builtIn;
    private String createdAt;
    private String updatedAt;
}
