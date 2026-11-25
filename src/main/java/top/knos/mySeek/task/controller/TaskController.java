package top.knos.mySeek.task.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.exception.CustomHttpException;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.support.SecurityUtils;
import top.knos.mySeek.task.runtime.TaskEventStreamService;
import top.knos.mySeek.task.service.TaskService;
import top.knos.mySeek.task.vo.TaskApprovalReq;
import top.knos.mySeek.task.vo.TaskCreateReq;
import top.knos.mySeek.task.vo.TaskVo;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskEventStreamService eventStreamService;

    @PostMapping
    public Result<TaskVo> create(@RequestBody TaskCreateReq req) {
        return Result.ok(taskService.create(currentUserId(), req));
    }

    @GetMapping
    public Result<Page<TaskVo>> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return Result.ok(taskService.list(currentUserId(), page, size));
    }

    @GetMapping("/{taskId}")
    public Result<TaskVo> detail(@PathVariable String taskId) {
        return Result.ok(taskService.detail(currentUserId(), taskId));
    }

    @GetMapping(value = "/{taskId}/events", produces = "text/event-stream")
    public SseEmitter events(@PathVariable String taskId,
                             @RequestParam(defaultValue = "0") long after) {
        // 先做归属校验，避免通过 SSE 泄漏他人任务事件
        taskService.detail(currentUserId(), taskId);
        return eventStreamService.subscribe(taskId, after);
    }

    @PostMapping("/{taskId}/confirm")
    public Result<TaskVo> confirm(@PathVariable String taskId) {
        return Result.ok(taskService.confirmPlan(currentUserId(), taskId));
    }

    @PostMapping("/{taskId}/approve")
    public Result<TaskVo> approve(@PathVariable String taskId, @RequestBody TaskApprovalReq req) {
        return Result.ok(taskService.approve(currentUserId(), taskId, req));
    }

    @PostMapping("/{taskId}/cancel")
    public Result<TaskVo> cancel(@PathVariable String taskId) {
        return Result.ok(taskService.cancel(currentUserId(), taskId));
    }

    @PostMapping("/{taskId}/steps/{stepId}/retry")
    public Result<TaskVo> retryStep(@PathVariable String taskId, @PathVariable String stepId) {
        return Result.ok(taskService.retryStep(currentUserId(), taskId, stepId));
    }

    private Long currentUserId() {
        User user = SecurityUtils.getUser();
        if (user == null || user.getId() == null) {
            throw new CustomHttpException("未登录或登录已过期", HttpStatus.UNAUTHORIZED);
        }
        return user.getId();
    }
}
