package top.knos.mySeek.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.knos.mySeek.task.entity.AgentTask;
import top.knos.mySeek.task.entity.AgentTaskStep;
import top.knos.mySeek.task.repository.AgentTaskEventRepository;
import top.knos.mySeek.task.repository.AgentTaskRepository;
import top.knos.mySeek.task.repository.AgentTaskStepRepository;
import top.knos.mySeek.task.runtime.TaskEventService;
import top.knos.mySeek.task.runtime.TaskExecutionService;
import top.knos.mySeek.task.vo.TaskApprovalReq;
import top.knos.mySeek.task.vo.TaskCreateReq;
import top.knos.mySeek.task.vo.TaskVo;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private AgentTaskRepository taskRepository;
    private AgentTaskStepRepository stepRepository;
    private AgentTaskEventRepository eventRepository;
    private TaskEventService eventService;
    private TaskExecutionService executionService;
    private TaskService service;

    @BeforeEach
    void setUp() {
        taskRepository = mock(AgentTaskRepository.class);
        stepRepository = mock(AgentTaskStepRepository.class);
        eventRepository = mock(AgentTaskEventRepository.class);
        eventService = mock(TaskEventService.class);
        executionService = mock(TaskExecutionService.class);
        service = new TaskService(taskRepository, stepRepository, eventRepository, eventService, executionService);
        when(taskRepository.save(any(AgentTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepository.save(any(AgentTaskStep.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepository.findByTaskIdOrderBySeqAsc(any())).thenReturn(List.of());
        when(eventRepository.findByTaskIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
    }

    @Test
    void createPersistsTaskAndSchedulesPlanning() {
        TaskCreateReq req = new TaskCreateReq();
        req.setTitle("整理设定");
        req.setGoal("整理世界观并输出文档");
        req.setModelCode("ollama:deepseek-r1:1.5b");
        final AgentTask[] saved = new AgentTask[1];
        when(taskRepository.save(any(AgentTask.class))).thenAnswer(invocation -> {
            saved[0] = invocation.getArgument(0);
            return saved[0];
        });
        when(taskRepository.findByIdAndCreatedBy(any(), eq(1L)))
                .thenAnswer(invocation -> Optional.ofNullable(saved[0]));

        TaskVo vo = service.create(1L, req);

        assertEquals("整理设定", vo.getTitle());
        assertEquals(AgentTask.Status.CREATED, vo.getStatus());
        verify(executionService).planAsync(any(), eq(1L));
        verify(eventService).record(any(), isNull(), eq("task_created"), any(), eq(1L));
    }

    @Test
    void confirmPlanStartsExecution() {
        AgentTask task = task("task-1", AgentTask.Status.PLAN_REVIEW);
        when(taskRepository.findByIdAndCreatedBy("task-1", 1L)).thenReturn(Optional.of(task));

        TaskVo vo = service.confirmPlan(1L, "task-1");

        assertEquals(AgentTask.Status.RUNNING, vo.getStatus());
        verify(executionService).executeAsync("task-1", 1L);
    }

    @Test
    void retryFailedStepResetsStepAndTask() {
        AgentTask task = task("task-1", AgentTask.Status.FAILED);
        AgentTaskStep step = new AgentTaskStep();
        step.setId("step-1");
        step.setTaskId("task-1");
        step.setStatus(AgentTaskStep.Status.FAILED);
        step.setErrorMessage("boom");
        when(taskRepository.findByIdAndCreatedBy("task-1", 1L)).thenReturn(Optional.of(task));
        when(stepRepository.findById("step-1")).thenReturn(Optional.of(step));

        TaskVo vo = service.retryStep(1L, "task-1", "step-1");

        assertEquals(AgentTask.Status.RUNNING, vo.getStatus());
        assertEquals(AgentTaskStep.Status.PENDING, step.getStatus());
        assertNull(step.getErrorMessage());
        verify(executionService).executeAsync("task-1", 1L);
    }

    @Test
    void approveSensitiveStepResumesExecution() {
        AgentTask task = task("task-1", AgentTask.Status.WAIT_APPROVAL);
        task.setCurrentStep(1);
        AgentTaskStep step = sensitiveStep();
        when(taskRepository.findByIdAndCreatedBy("task-1", 1L)).thenReturn(Optional.of(task));
        when(stepRepository.findByTaskIdOrderBySeqAsc("task-1")).thenReturn(List.of(step));
        TaskApprovalReq req = new TaskApprovalReq();
        req.setApproved(true);

        TaskVo vo = service.approve(1L, "task-1", req);

        assertEquals(AgentTask.Status.RUNNING, vo.getStatus());
        assertTrue(step.getApproved());
        verify(executionService).executeAsync("task-1", 1L);
    }

    @Test
    void rejectSensitiveStepStopsTask() {
        AgentTask task = task("task-1", AgentTask.Status.WAIT_APPROVAL);
        task.setCurrentStep(1);
        AgentTaskStep step = sensitiveStep();
        when(taskRepository.findByIdAndCreatedBy("task-1", 1L)).thenReturn(Optional.of(task));
        when(stepRepository.findByTaskIdOrderBySeqAsc("task-1")).thenReturn(List.of(step));
        TaskApprovalReq req = new TaskApprovalReq();
        req.setApproved(false);
        req.setReason("不允许写作品");

        TaskVo vo = service.approve(1L, "task-1", req);

        assertEquals(AgentTask.Status.REJECTED, vo.getStatus());
        assertEquals(AgentTaskStep.Status.SKIPPED, step.getStatus());
        verify(executionService, never()).executeAsync(any(), any());
    }

    @Test
    void detailRejectsForeignTask() {
        when(taskRepository.findByIdAndCreatedBy("task-1", 2L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.detail(2L, "task-1"));
    }

    private AgentTaskStep sensitiveStep() {
        AgentTaskStep step = new AgentTaskStep();
        step.setId("step-1");
        step.setTaskId("task-1");
        step.setSeq(1);
        step.setTitle("保存草稿");
        step.setTool("novel_write_draft");
        step.setStatus(AgentTaskStep.Status.PENDING);
        step.setRequiresApproval(true);
        step.setApproved(false);
        return step;
    }

    private AgentTask task(String id, AgentTask.Status status) {
        AgentTask task = new AgentTask();
        task.setId(id);
        task.setTitle("任务");
        task.setGoal("目标");
        task.setStatus(status);
        task.setCreatedBy(1L);
        return task;
    }
}
