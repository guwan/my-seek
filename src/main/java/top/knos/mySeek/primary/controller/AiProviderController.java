package top.knos.mySeek.primary.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.primary.service.AiProviderConfigService;
import top.knos.mySeek.primary.vo.AiProviderRequest;
import top.knos.mySeek.primary.vo.AiProviderVo;

import java.util.List;

/** 第三方 AI 接口管理（需登录，/api/** 已在 SecurityConfig 中受保护） */
@RestController
@RequestMapping("/api/ai/providers")
@Slf4j
public class AiProviderController {

    private final AiProviderConfigService service;

    public AiProviderController(AiProviderConfigService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<AiProviderVo>> list() {
        try {
            return Result.ok(service.list());
        } catch (Exception e) {
            log.error("获取 AI 接口列表失败", e);
            return new Result<>(false, "获取列表失败：" + e.getMessage());
        }
    }

    @PostMapping
    public Result<AiProviderVo> create(@Valid @RequestBody AiProviderRequest req) {
        try {
            return Result.ok(service.create(req));
        } catch (Exception e) {
            log.warn("新增 AI 接口失败: {}", e.getMessage());
            return new Result<>(false, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<AiProviderVo> update(@PathVariable String id, @Valid @RequestBody AiProviderRequest req) {
        try {
            return Result.ok(service.update(id, req));
        } catch (Exception e) {
            log.warn("更新 AI 接口失败: {}", e.getMessage());
            return new Result<>(false, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        try {
            service.delete(id);
            return new Result<Void>(true);
        } catch (Exception e) {
            log.warn("删除 AI 接口失败: {}", e.getMessage());
            return new Result<>(false, e.getMessage());
        }
    }

    @PatchMapping("/{id}/toggle")
    public Result<AiProviderVo> toggle(@PathVariable String id, @RequestParam boolean enabled) {
        try {
            return Result.ok(service.toggle(id, enabled));
        } catch (Exception e) {
            log.warn("切换 AI 接口状态失败: {}", e.getMessage());
            return new Result<>(false, e.getMessage());
        }
    }

    /** 测试接口连通性 */
    @PostMapping("/test")
    public Result<Void> test(@Valid @RequestBody AiProviderRequest req) {
        try {
            service.test(req);
            return new Result<Void>(true);
        } catch (Exception e) {
            log.warn("测试 AI 接口失败: {}", e.getMessage());
            return new Result<>(false, "连接失败：" + e.getMessage());
        }
    }
}
