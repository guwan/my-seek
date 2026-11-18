package top.knos.mySeek.primary.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/** 新增 / 更新 AI 接口的请求体 */
@Data
public class AiProviderRequest {

    /** 更新时传入已有 id，新增时不传 */
    private String id;

    @NotBlank(message = "接口标识不能为空")
    @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,31}", message = "接口标识只能以字母开头，含字母数字下划线连字符，最长 32 位")
    private String name;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    @NotBlank(message = "base-url 不能为空")
    private String baseUrl;

    /** 新增必填；更新时留空表示沿用原 key */
    private String apiKey;

    private List<String> models;

    private boolean enabled = true;
}
