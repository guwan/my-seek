package top.knos.mySeek.agent.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import top.knos.mySeek.agent.service.CatalogService;
import top.knos.mySeek.agent.vo.CatalogVos;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.exception.CustomHttpException;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.support.SecurityUtils;

import java.util.List;

/** 专家 / 技能 / 连接器目录管理。 */
@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService service;

    @GetMapping("/agents")
    public Result<List<CatalogVos.AgentVo>> agents() {
        return Result.ok(service.listAgents(currentUserId()));
    }

    @GetMapping("/skills")
    public Result<List<CatalogVos.SkillVo>> skills() {
        return Result.ok(service.listSkills(currentUserId()));
    }

    @GetMapping("/connectors")
    public Result<List<CatalogVos.ConnectorVo>> connectors() {
        return Result.ok(service.listConnectors(currentUserId()));
    }

    @PutMapping("/agents")
    public Result<CatalogVos.AgentVo> saveAgent(@RequestBody CatalogVos.AgentSaveReq req) {
        return Result.ok(service.saveAgent(currentUserId(), req));
    }

    @PutMapping("/skills")
    public Result<CatalogVos.SkillVo> saveSkill(@RequestBody CatalogVos.SkillSaveReq req) {
        return Result.ok(service.saveSkill(currentUserId(), req));
    }

    @PutMapping("/connectors")
    public Result<CatalogVos.ConnectorVo> saveConnector(@RequestBody CatalogVos.ConnectorSaveReq req) {
        return Result.ok(service.saveConnector(currentUserId(), req));
    }

    @DeleteMapping("/agents/{id}")
    public Result<Void> deleteAgent(@PathVariable String id) {
        service.deleteAgent(currentUserId(), id);
        return Result.ok(null);
    }

    @DeleteMapping("/skills/{id}")
    public Result<Void> deleteSkill(@PathVariable String id) {
        service.deleteSkill(currentUserId(), id);
        return Result.ok(null);
    }

    @DeleteMapping("/connectors/{id}")
    public Result<Void> deleteConnector(@PathVariable String id) {
        service.deleteConnector(currentUserId(), id);
        return Result.ok(null);
    }

    private Long currentUserId() {
        User user = SecurityUtils.getUser();
        if (user == null || user.getId() == null) {
            throw new CustomHttpException("未登录或登录已过期", HttpStatus.UNAUTHORIZED);
        }
        return user.getId();
    }
}
