package top.knos.mySeek.primary.controller;

import tools.jackson.core.JacksonException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.primary.service.NovelGenerationService;
import top.knos.mySeek.support.vo.NovelReq;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/novel")
public class NovelController {
    private final NovelGenerationService generationService;

    @PostMapping("generate")
    public Result<String> generateNovel(@RequestBody NovelReq req) throws JacksonException {
        generationService.generateNovel(req.getMessage(), req.getTotalChapters());
        return Result.ok("小说生成已启动，请查看服务端配置的 novel.storage.path 目录");
    }
}
