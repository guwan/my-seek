package top.knos.mySeek.task.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import top.knos.mySeek.task.entity.AgentTask;
import top.knos.mySeek.task.entity.AgentTaskStep;
import top.knos.mySeek.task.repository.AgentTaskEventRepository;
import top.knos.mySeek.task.repository.AgentTaskRepository;
import top.knos.mySeek.task.repository.AgentTaskStepRepository;
import top.knos.mySeek.task.runtime.AgentToolRegistry;
import top.knos.mySeek.task.runtime.TaskEventService;
import top.knos.mySeek.task.runtime.TaskExecutionService;
import top.knos.mySeek.task.vo.TaskApprovalReq;
import top.knos.mySeek.task.vo.TaskCreateReq;
import top.knos.mySeek.task.vo.TaskVo;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final AgentTaskRepository taskRepository;
    private final AgentTaskStepRepository stepRepository;
    private final AgentTaskEventRepository eventRepository;
    private final TaskEventService eventService;
    private final TaskExecutionService executionService;
    private final AgentToolRegistry toolRegistry;

    public TaskVo create(Long userId, TaskCreateReq req) {
        if (req == null || req.getTitle() == null || req.getTitle().isBlank()
                || req.getGoal() == null || req.getGoal().isBlank()) {
            throw new IllegalArgumentException("任务标题和目标不能为空");
        }
        AgentTask task = new AgentTask();
        task.setId(UUID.randomUUID().toString());
        task.setTitle(req.getTitle().trim());
        task.setGoal(req.getGoal().trim());
        task.setConstraints(req.getConstraints());
        task.setModelCode(req.getModelCode());
        task.setAgentCode(req.getAgentCode());
        task.setStatus(AgentTask.Status.CREATED);
        task.setCurrentStep(0);
        task.setMaxSteps(8);
        task.setCreatedBy(userId);
        task = taskRepository.save(task);
        eventService.record(task.getId(), null, "task_created", "任务已创建", userId);
        executionService.planAsync(task.getId(), userId);
        return detail(userId, task.getId());
    }

    /** 任务中心可用工具列表，供前端展示规划能力边界（含外部 MCP 工具）。 */
    public List<String> listTools() {
        return toolRegistry.availableTools();
    }

    public Page<TaskVo> list(Long userId, int page, int size) {
        Page<AgentTask> tasks = taskRepository.findByCreatedByOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size))));
        return tasks.map(task -> TaskVo.from(task, List.of(), List.of()));
    }

    public TaskVo detail(Long userId, String taskId) {
        AgentTask task = owned(userId, taskId);
        List<AgentTaskStep> steps = stepRepository.findByTaskIdOrderBySeqAsc(taskId);
        return TaskVo.from(task, steps, eventRepository.findByTaskIdOrderByCreatedAtAsc(taskId));
    }

    public TaskVo confirmPlan(Long userId, String taskId) {
        AgentTask task = owned(userId, taskId);
        if (task.getStatus() != AgentTask.Status.PLAN_REVIEW) {
            throw new IllegalStateException("只有待确认计划的任务可以开始执行");
        }
        task.setStatus(AgentTask.Status.RUNNING);
        task.setErrorMessage(null);
        taskRepository.save(task);
        eventService.record(taskId, null, "plan_confirmed", "用户已确认计划", userId);
        executionService.executeAsync(taskId, userId);
        return detail(userId, taskId);
    }

    public TaskVo approve(Long userId, String taskId, TaskApprovalReq req) {
        AgentTask task = owned(userId, taskId);
        if (task.getStatus() != AgentTask.Status.WAIT_APPROVAL) {
            throw new IllegalStateException("当前任务不需要审批");
        }
        if (req == null) {
            throw new IllegalArgumentException("审批结果不能为空");
        }
        AgentTaskStep step = stepRepository.findByTaskIdOrderBySeqAsc(taskId).stream()
                .filter(s -> s.getSeq() != null && s.getSeq().equals(task.getCurrentStep()))
                .filter(s -> Boolean.TRUE.equals(s.getRequiresApproval()))
                .filter(s -> !Boolean.TRUE.equals(s.getApproved()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("找不到待审批的步骤"));
        String reason = req.getReason() == null ? "" : req.getReason().trim();

        if (req.isApproved()) {
            step.setApproved(true);
            stepRepository.save(step);
            task.setStatus(AgentTask.Status.RUNNING);
            task.setErrorMessage(null);
            taskRepository.save(task);
            eventService.record(taskId, step.getId(), "approval_granted",
                    reason.isEmpty() ? "用户批准执行：" + step.getTitle() : reason, userId);
            executionService.executeAsync(taskId, userId);
        } else {
            step.setStatus(AgentTaskStep.Status.SKIPPED);
            stepRepository.save(step);
            task.setStatus(AgentTask.Status.REJECTED);
            task.setErrorMessage("用户拒绝执行步骤：" + step.getTitle());
            taskRepository.save(task);
            eventService.record(taskId, step.getId(), "approval_rejected",
                    reason.isEmpty() ? "用户拒绝执行：" + step.getTitle() : reason, userId);
        }
        return detail(userId, taskId);
    }

    public TaskVo cancel(Long userId, String taskId) {
        AgentTask task = owned(userId, taskId);
        if (isTerminal(task.getStatus())) {
            throw new IllegalStateException("任务已结束，无法取消");
        }
        task.setStatus(AgentTask.Status.CANCELLED);
        taskRepository.save(task);
        eventService.record(taskId, null, "task_cancelled", "用户已取消任务", userId);
        return detail(userId, taskId);
    }

    public TaskVo retryStep(Long userId, String taskId, String stepId) {
        AgentTask task = owned(userId, taskId);
        AgentTaskStep step = stepRepository.findById(stepId)
                .filter(s -> s.getTaskId().equals(taskId))
                .orElseThrow(() -> new IllegalArgumentException("步骤不存在或不属于该任务"));
        if (step.getStatus() != AgentTaskStep.Status.FAILED) {
            throw new IllegalStateException("只有失败的步骤可以重试");
        }
        step.setStatus(AgentTaskStep.Status.PENDING);
        step.setErrorMessage(null);
        stepRepository.save(step);
        task.setStatus(AgentTask.Status.RUNNING);
        task.setErrorMessage(null);
        taskRepository.save(task);
        eventService.record(taskId, stepId, "step_retry", "重新执行步骤：" + step.getTitle(), userId);
        executionService.executeAsync(taskId, userId);
        return detail(userId, taskId);
    }

    public AgentTask owned(Long userId, String taskId) {
        return taskRepository.findByIdAndCreatedBy(taskId, userId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在或无权访问"));
    }

    public static boolean isTerminal(AgentTask.Status status) {
        return status == AgentTask.Status.COMPLETED || status == AgentTask.Status.FAILED
                || status == AgentTask.Status.CANCELLED || status == AgentTask.Status.REJECTED;
    }
}
