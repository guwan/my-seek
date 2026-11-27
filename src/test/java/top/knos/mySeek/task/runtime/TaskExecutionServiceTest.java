package top.knos.mySeek.task.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.task.entity.AgentTask;
import top.knos.mySeek.task.entity.AgentTaskStep;
import top.knos.mySeek.task.repository.AgentTaskRepository;
import top.knos.mySeek.task.repository.AgentTaskStepRepository;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskExecutionServiceTest {

    @Test
    void groupIntoBatchesSplitsSequentialStepsAndParallelGroups() {
        List<AgentTaskStep> steps = List.of(
                step(1, null),
                step(2, "batch1"),
                step(3, "batch1"),
                step(4, ""),
                step(5, "batch2"),
                step(6, "batch2"),
                step(7, "batch2"));

        List<List<AgentTaskStep>> batches = TaskExecutionService.groupIntoBatches(steps);

        assertEquals(4, batches.size());
        assertEquals(List.of(1), seqs(batches.get(0)));
        assertEquals(List.of(2, 3), seqs(batches.get(1)));
        assertEquals(List.of(4), seqs(batches.get(2)));
        assertEquals(List.of(5, 6, 7), seqs(batches.get(3)));
    }

    @Test
    void groupIntoBatchesTreatsNonConsecutiveSameGroupAsSeparateBatches() {
        List<AgentTaskStep> steps = List.of(step(1, "batch1"), step(2, null), step(3, "batch1"));

        List<List<AgentTaskStep>> batches = TaskExecutionService.groupIntoBatches(steps);

        assertEquals(3, batches.size());
        assertEquals(List.of(1), seqs(batches.get(0)));
        assertEquals(List.of(2), seqs(batches.get(1)));
        assertEquals(List.of(3), seqs(batches.get(2)));
    }

    @Test
    void normalizeParallelGroupsRenamesNonConsecutiveGroups() {
        List<TaskExecutionService.PlannedStep> planned = new ArrayList<>();
        planned.add(new TaskExecutionService.PlannedStep("a", "model", "{}", "", false, "batch1"));
        planned.add(new TaskExecutionService.PlannedStep("b", "model", "{}", "", false, ""));
        planned.add(new TaskExecutionService.PlannedStep("c", "model", "{}", "", false, "batch1"));
        planned.add(new TaskExecutionService.PlannedStep("d", "model", "{}", "", false, "batch1"));

        TaskExecutionService.normalizeParallelGroups(planned);

        assertEquals("batch1", planned.get(0).parallelGroup());
        assertEquals("", planned.get(1).parallelGroup());
        assertEquals("batch1_2", planned.get(2).parallelGroup());
        assertEquals("batch1_2", planned.get(3).parallelGroup());
    }

    @Test
    void parsePlanReadsParallelGroupAndClearsItForApprovedOrSensitiveSteps() throws Exception {
        AgentToolRegistry registry = mock(AgentToolRegistry.class);
        when(registry.isSensitive("novel_write_draft")).thenReturn(true);
        TaskExecutionService service = new TaskExecutionService(
                null, null, null, registry, null, Runnable::run);

        String json = """
                {"steps":[
                  {"title":"检索","tool":"knowledge_search","input":{"q":"a"},"parallel_group":"batch1"},
                  {"title":"写作","tool":"novel_write_draft","input":{"text":"x"},"parallel_group":"batch1"},
                  {"title":"确认","tool":"model","input":{},"requires_approval":true,"parallel_group":"batch2"}
                ]}
                """;

        @SuppressWarnings("unchecked")
        List<TaskExecutionService.PlannedStep> planned =
                (List<TaskExecutionService.PlannedStep>) invokeParsePlan(service, json);

        assertEquals(3, planned.size());
        assertEquals("batch1", planned.get(0).parallelGroup());
        assertFalse(planned.get(0).requiresApproval());
        assertEquals("", planned.get(1).parallelGroup());
        assertTrue(planned.get(1).requiresApproval());
        assertEquals("", planned.get(2).parallelGroup());
    }

    @Test
    void parsePlanFallsBackToEmptyGroupWhenMissing() throws Exception {
        AgentToolRegistry registry = mock(AgentToolRegistry.class);
        TaskExecutionService service = new TaskExecutionService(
                null, null, null, registry, null, Runnable::run);

        @SuppressWarnings("unchecked")
        List<TaskExecutionService.PlannedStep> planned = (List<TaskExecutionService.PlannedStep>)
                invokeParsePlan(service, "{\"steps\":[{\"title\":\"单步\",\"tool\":\"model\"}]}");

        assertEquals(1, planned.size());
        assertEquals("", planned.get(0).parallelGroup());
    }

    @Test
    void executeAsyncRunsParallelStepsOnDifferentThreads() throws Exception {
        String taskId = "task-parallel-" + System.nanoTime();
        AgentTask task = new AgentTask();
        task.setId(taskId);
        task.setStatus(AgentTask.Status.RUNNING);
        task.setGoal("验证并行执行");
        task.setMaxSteps(8);
        task.setCreatedBy(1L);

        AgentTaskStep s1 = step(1, "batch1");
        s1.setTool("knowledge_search");
        s1.setInputJson("{\"q\":\"a\"}");
        AgentTaskStep s2 = step(2, "batch1");
        s2.setTool("knowledge_search");
        s2.setInputJson("{\"q\":\"b\"}");

        AgentTaskRepository taskRepo = mock(AgentTaskRepository.class);
        AgentTaskStepRepository stepRepo = mock(AgentTaskStepRepository.class);
        TaskEventService events = mock(TaskEventService.class);
        AgentToolRegistry registry = mock(AgentToolRegistry.class);
        ModelProviderManager models = mock(ModelProviderManager.class);
        when(taskRepo.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(AgentTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stepRepo.findByTaskIdOrderBySeqAsc(taskId)).thenReturn(List.of(s1, s2));
        when(stepRepo.save(any(AgentTaskStep.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<String> threads = ConcurrentHashMap.newKeySet();
        CountDownLatch bothStarted = new CountDownLatch(2);
        when(registry.execute(eq("knowledge_search"), eq(1L), anyString(), any()))
                .thenAnswer(inv -> {
                    threads.add(Thread.currentThread().getName());
                    bothStarted.countDown();
                    if (!bothStarted.await(3, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("步骤未并发执行");
                    }
                    return "检索结果";
                });
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(models.getDefaultChatClient()).thenReturn(chatClient);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content()).thenReturn("任务摘要");

        AtomicInteger threadSeq = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(4,
                r -> new Thread(r, "test-step-" + threadSeq.incrementAndGet()));
        try {
            TaskExecutionService service = new TaskExecutionService(
                    taskRepo, stepRepo, events, registry, models, pool);

            service.executeAsync(taskId, 1L);

            assertEquals(2, threads.size(), "并行组步骤应在不同线程执行");
            assertEquals(AgentTask.Status.COMPLETED, task.getStatus());
            assertEquals(AgentTaskStep.Status.SUCCEEDED, s1.getStatus());
            assertEquals(AgentTaskStep.Status.SUCCEEDED, s2.getStatus());
            verify(events).record(eq(taskId), isNull(), eq("task_completed"), any(), eq(1L));
        } finally {
            pool.shutdownNow();
        }
    }

    private static Object invokeParsePlan(TaskExecutionService service, String raw) throws Exception {
        Method method = TaskExecutionService.class.getDeclaredMethod("parsePlan", String.class);
        method.setAccessible(true);
        return method.invoke(service, raw);
    }

    private static AgentTaskStep step(int seq, String group) {
        AgentTaskStep step = new AgentTaskStep();
        step.setSeq(seq);
        step.setTitle("步骤" + seq);
        step.setParallelGroup(group);
        step.setStatus(AgentTaskStep.Status.PENDING);
        return step;
    }

    private static List<Integer> seqs(List<AgentTaskStep> steps) {
        return steps.stream().map(AgentTaskStep::getSeq).toList();
    }
}
