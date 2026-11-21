package top.knos.mySeek.writing.controller;

import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.exception.CustomHttpException;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.support.SecurityUtils;
import top.knos.mySeek.writing.service.WritingService;

import java.util.List;

/**
 * 写作作品云端同步接口。所有接口均按当前登录用户隔离。
 */
@Slf4j
@RestController
@RequestMapping("/api/writing")
@RequiredArgsConstructor
public class WritingController {

    private final WritingService writingService;

    @GetMapping("/novels")
    public Result<List<JsonNode>> listNovels() {
        return Result.ok(writingService.listNovels(currentUserId()));
    }

    @PutMapping("/novels")
    public Result<JsonNode> saveNovel(@RequestBody JsonNode payload) {
        return Result.ok(writingService.saveNovel(currentUserId(), payload));
    }

    @DeleteMapping("/novels/{novelId}")
    public Result<Object> deleteNovel(@PathVariable String novelId) {
        writingService.deleteNovel(currentUserId(), novelId);
        return Result.ok(null);
    }

    private Long currentUserId() {
        User user = SecurityUtils.getUser();
        if (user == null || user.getId() == null) {
            throw new CustomHttpException("未登录或登录已过期", HttpStatus.UNAUTHORIZED);
        }
        return WritingService.requireUserId(user.getId());
    }
}
