package top.knos.mySeek.primary.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.primary.service.OllamaConfigService;
import top.knos.mySeek.primary.vo.OllamaConfigRequest;
import top.knos.mySeek.primary.vo.OllamaConfigVo;

import java.util.List;

/** 内置 Ollama 运行时配置（需登录） */
@Slf4j
@RestController
@RequestMapping("/api/ai/ollama")
public class OllamaController {

    private final OllamaConfigService service;

    public OllamaController(OllamaConfigService service) {
        this.service = service;
    }

    @GetMapping("/config")
    public Result<OllamaConfigVo> getConfig() {
        return Result.ok(service.get());
    }

    @PutMapping("/config")
    public Result<OllamaConfigVo> updateConfig(@Valid @RequestBody OllamaConfigRequest req) {
        try {
            return Result.ok(service.update(req));
        } catch (Exception e) {
            log.warn("更新 Ollama 配置失败: {}", e.getMessage());
            return new Result<>(false, e.getMessage());
        }
    }

    /** 测试地址并返回在线模型列表 */
    @PostMapping("/test")
    public Result<List<String>> test(@Valid @RequestBody OllamaConfigRequest req) {
        try {
            return Result.ok(service.test(req));
        } catch (Exception e) {
            log.warn("测试 Ollama 连接失败: {}", e.getMessage());
            return new Result<>(false, "连接失败：" + e.getMessage());
        }
    }
}
