package top.knos.mySeek.task.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.task.entity.AgentTask;
import top.knos.mySeek.task.entity.AgentTaskStep;
import top.knos.mySeek.task.repository.AgentTaskRepository;
import top.knos.mySeek.task.repository.AgentTaskStepRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * AI 任务运行时：plan 阶段产出可见步骤，build 阶段逐步执行。
 * 同一 parallel_group 且连续的步骤并发执行（轻量 sub-agent 风格），批次之间顺序执行。
 * 使用独立 Bean 承载 @Async，避免同类自调用导致代理失效。
 */
@Slf4j
@Service
public class TaskExecutionService {

    private final AgentTaskRepository taskRepository;
    private final AgentTaskStepRepository stepRepository;
    private final TaskEventService eventService;
    private final AgentToolRegistry toolRegistry;
    private final ModelProviderManager modelProviderManager;
    private final Executor stepExecutor;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final Set<String> activeTasks = ConcurrentHashMap.newKeySet();

    public TaskExecutionService(AgentTaskRepository taskRepository,
                                AgentTaskStepRepository stepRepository,
                                TaskEventService eventService,
                                AgentToolRegistry toolRegistry,
                                ModelProviderManager modelProviderManager,
                                @Qualifier("agentStepExecutor") Executor stepExecutor) {
        this.taskRepository = taskRepository;
        this.stepRepository = stepRepository;
        this.eventService = eventService;
        this.toolRegistry = toolRegistry;
        this.modelProviderManager = modelProviderManager;
        this.stepExecutor = stepExecutor;
    }

    @Async("agentTaskExecutor")
    public void planAsync(String taskId, Long userId) {
        AgentTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != AgentTask.Status.CREATED) return;

        try {
            task.setStatus(AgentTask.Status.PLANNING);
            taskRepository.save(task);
            eventService.record(taskId, null, "planning_started", "正在生成执行计划", userId);

            String raw = chatClient(task.getModelCode())
                    .prompt()
                    .system("""
                            你是任务规划助手。只输出标准 JSON，不要 Markdown 代码块，不要额外说明。
                             格式：{"steps":[{"title":"步骤标题","tool":"model|knowledge_search|knowledge_list|novel_read|novel_write_draft|web_search","input":"JSON 对象字符串","reason":"为什么做这步","requires_approval":false,"parallel_group":""}]}
                             tool 必须在白名单内；不需要工具时填 model。步骤数不超过 8，每步目标必须具体可执行。
                             写操作工具（novel_write_draft）必须设置 requires_approval=true，且不要设置 parallel_group。
                             parallel_group 仅用于彼此独立、无先后依赖、无写冲突的只读步骤：给它们填相同的组名（如 "batch1"），这些步骤会同时执行。
                             留空或省略 parallel_group 表示顺序执行；同一组的步骤请连续排列。
                             """)
                    .user("任务目标：" + task.getGoal() + "\n约束：" + nullToEmpty(task.getConstraints())
                            + "\n标题：" + task.getTitle())
                    .call()
                    .content();

            List<PlannedStep> planned = parsePlan(raw);
            if (planned.isEmpty()) {
                PlannedStep fallback = new PlannedStep("根据目标生成结果", "model",
                        "{\"prompt\":" + quote(task.getGoal()) + "}", "模型无法输出有效计划，直接执行目标", false, "");
                planned.add(fallback);
            }
            int max = task.getMaxSteps() == null ? 8 : task.getMaxSteps();
            if (planned.size() > max) planned = planned.subList(0, max);
            normalizeParallelGroups(planned);

            int seq = 1;
            for (PlannedStep item : planned) {
                AgentTaskStep step = new AgentTaskStep();
                step.setId(UUID.randomUUID().toString());
                step.setTaskId(taskId);
                step.setSeq(seq++);
                step.setTitle(item.title());
                step.setTool(item.tool());
                step.setInputJson(item.input());
                step.setParallelGroup(item.parallelGroup());
                step.setRequiresApproval(item.requiresApproval());
                step.setApproved(false);
                step.setStatus(AgentTaskStep.Status.PENDING);
                step.setCreatedBy(userId);
                stepRepository.save(step);
            }

            task.setStatus(AgentTask.Status.PLAN_REVIEW);
            task.setCurrentStep(0);
            taskRepository.save(task);
            eventService.record(taskId, null, "plan_generated", "已生成 " + planned.size() + " 个步骤，等待确认", userId);
            log.info("任务计划生成完成: taskId={}, steps={}", taskId, planned.size());
        } catch (Exception e) {
            log.error("任务计划生成失败: taskId={}", taskId, e);
            task.setStatus(AgentTask.Status.FAILED);
            task.setErrorMessage(trim(e.getMessage(), "计划生成失败"));
            taskRepository.save(task);
            eventService.record(taskId, null, "plan_failed", task.getErrorMessage(), userId);
        }
    }

    @Async("agentTaskExecutor")
    public void executeAsync(String taskId, Long userId) {
        if (!activeTasks.add(taskId)) {
            log.warn("任务已有执行线程，忽略重复投递: {}", taskId);
            return;
        }
        try {
            AgentTask task = taskRepository.findById(taskId).orElse(null);
            if (task == null || task.getStatus() != AgentTask.Status.RUNNING) return;

            List<AgentTaskStep> steps = stepRepository.findByTaskIdOrderBySeqAsc(taskId);
            List<List<AgentTaskStep>> batches = groupIntoBatches(steps);
            List<String> previousResults = new ArrayList<>();
            int executed = 0;
            int max = task.getMaxSteps() == null ? 8 : task.getMaxSteps();

            for (List<AgentTaskStep> batch : batches) {
                AgentTask current = taskRepository.findById(taskId).orElse(null);
                if (current == null || current.getStatus() != AgentTask.Status.RUNNING) return;

                List<AgentTaskStep> pending = new ArrayList<>();
                for (AgentTaskStep step : batch) {
                    if (step.getStatus() == AgentTaskStep.Status.SUCCEEDED) {
                        previousResults.add("[" + step.getTitle() + "] " + shortText(step.getResultText()));
                    } else if (step.getStatus() != AgentTaskStep.Status.SKIPPED) {
                        pending.add(step);
                    }
                }
                if (pending.isEmpty()) continue;

                AgentTaskStep approving = pending.stream()
                        .filter(s -> Boolean.TRUE.equals(s.getRequiresApproval())
                                && !Boolean.TRUE.equals(s.getApproved()))
                        .findFirst().orElse(null);
                if (approving != null) {
                    current.setStatus(AgentTask.Status.WAIT_APPROVAL);
                    current.setCurrentStep(approving.getSeq());
                    taskRepository.save(current);
                    approving.setStatus(AgentTaskStep.Status.PENDING);
                    stepRepository.save(approving);
                    eventService.record(taskId, approving.getId(), "approval_requested",
                            "等待批准：" + approving.getTitle() + "，工具=" + approving.getTool()
                                    + "，参数=" + shortText(approving.getInputJson()), userId);
                    log.info("任务等待工具审批: taskId={}, step={}, tool={}", taskId, approving.getId(), approving.getTool());
                    return;
                }

                executed += pending.size();
                if (executed > max) {
                    throw new IllegalStateException("执行步数超过上限 " + max);
                }

                List<StepOutcome> outcomes = executeBatch(current, pending, previousResults, userId);
                String failure = null;
                AgentTaskStep failedStep = null;
                for (StepOutcome outcome : outcomes) {
                    if (outcome.error() != null) {
                        failure = outcome.error();
                        failedStep = outcome.step();
                        break;
                    }
                }
                if (failure != null) {
                    AgentTask failedTask = taskRepository.findById(taskId).orElse(null);
                    if (failedTask != null) {
                        failedTask.setStatus(AgentTask.Status.FAILED);
                        failedTask.setErrorMessage(failure);
                        taskRepository.save(failedTask);
                    }
                    eventService.record(taskId, failedStep.getId(), "step_failed", failure, userId);
                    return;
                }

                for (StepOutcome outcome : outcomes) {
                    previousResults.add("[" + outcome.step().getTitle() + "] "
                            + shortText(outcome.step().getResultText()));
                    eventService.record(taskId, outcome.step().getId(), "step_finished",
                            shortText(outcome.step().getResultText()), userId);
                }
            }

            AgentTask finished = taskRepository.findById(taskId).orElse(null);
            if (finished == null || finished.getStatus() != AgentTask.Status.RUNNING) return;
            finished.setFinalSummary(generateSummary(finished, previousResults));
            finished.setStatus(AgentTask.Status.COMPLETED);
            finished.setErrorMessage(null);
            taskRepository.save(finished);
            eventService.record(taskId, null, "task_completed", "任务已完成", userId);
            log.info("任务执行完成: taskId={}", taskId);
        } catch (Exception e) {
            log.error("任务执行异常: taskId={}", taskId, e);
            AgentTask failed = taskRepository.findById(taskId).orElse(null);
            if (failed != null && failed.getStatus() == AgentTask.Status.RUNNING) {
                failed.setStatus(AgentTask.Status.FAILED);
                failed.setErrorMessage(trim(e.getMessage(), "任务执行失败"));
                taskRepository.save(failed);
                eventService.record(taskId, null, "task_failed", failed.getErrorMessage(), userId);
            }
        } finally {
            activeTasks.remove(taskId);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeUnfinishedTasks() {
        // 恢复是增强功能，绝不能因为历史库缺少任务表而阻止整个应用启动。
        try {
            List<AgentTask.Status> statuses = List.of(AgentTask.Status.PLANNING, AgentTask.Status.RUNNING);
            for (AgentTask task : taskRepository.findByStatusIn(statuses)) {
                try {
                    Long userId = task.getCreatedBy();
                    if (task.getStatus() == AgentTask.Status.PLANNING) {
                        task.setStatus(AgentTask.Status.CREATED);
                        taskRepository.save(task);
                        planAsync(task.getId(), userId);
                    } else {
                        executeAsync(task.getId(), userId);
                    }
                    log.info("已重新投递未完成任务: {}", task.getId());
                } catch (Exception e) {
                    log.warn("恢复单个任务失败，跳过: taskId={}, reason={}", task.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("扫描未完成任务失败，已跳过恢复；请确认 V6~V11 迁移已执行且 agent_tasks 表存在", e);
        }
    }

    /** 将一个批次（单步骤或并行组）投入执行，返回与入参同序的结果。 */
    private List<StepOutcome> executeBatch(AgentTask task, List<AgentTaskStep> batch,
                                           List<String> previousResults, Long userId) {
        List<String> sharedContext = List.copyOf(previousResults);
        boolean parallel = batch.size() > 1;

        for (AgentTaskStep step : batch) {
            step.setStatus(AgentTaskStep.Status.RUNNING);
            step.setErrorMessage(null);
            stepRepository.save(step);
            eventService.record(task.getId(), step.getId(), "step_started", step.getTitle(), userId);
        }
        task.setCurrentStep(batch.get(batch.size() - 1).getSeq());
        taskRepository.save(task);

        if (!parallel) {
            return List.of(executeOne(task, batch.get(0), sharedContext));
        }

        List<CompletableFuture<StepOutcome>> futures = new ArrayList<>();
        for (AgentTaskStep step : batch) {
            futures.add(CompletableFuture.supplyAsync(
                    () -> executeOne(task, step, sharedContext), stepExecutor));
        }
        List<StepOutcome> outcomes = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                outcomes.add(futures.get(i).join());
            } catch (Exception e) {
                AgentTaskStep step = batch.get(i);
                log.error("并行步骤执行异常: taskId={}, step={}", task.getId(), step.getId(), e);
                step.setStatus(AgentTaskStep.Status.FAILED);
                step.setErrorMessage(trim(e.getMessage(), "并行步骤执行失败"));
                stepRepository.save(step);
                outcomes.add(new StepOutcome(step, step.getErrorMessage()));
            }
        }
        return outcomes;
    }

    private StepOutcome executeOne(AgentTask task, AgentTaskStep step, List<String> previousResults) {
        try {
            String result = executeStep(task, step, previousResults);
            step.setResultText(result);
            step.setStatus(AgentTaskStep.Status.SUCCEEDED);
            stepRepository.save(step);
            return new StepOutcome(step, null);
        } catch (Exception e) {
            log.error("任务步骤失败: taskId={}, step={}", task.getId(), step.getId(), e);
            step.setStatus(AgentTaskStep.Status.FAILED);
            step.setErrorMessage(trim(e.getMessage(), "步骤执行失败"));
            stepRepository.save(step);
            return new StepOutcome(step, step.getErrorMessage());
        }
    }

    /** 按 parallel_group 把连续同组步骤聚成批次；组名为空的步骤各自成批。包可见便于单测。 */
    static List<List<AgentTaskStep>> groupIntoBatches(List<AgentTaskStep> steps) {
        List<List<AgentTaskStep>> batches = new ArrayList<>();
        List<AgentTaskStep> current = new ArrayList<>();
        String currentGroup = null;
        for (AgentTaskStep step : steps) {
            String group = groupOf(step);
            if (group != null && group.equals(currentGroup)) {
                current.add(step);
            } else {
                if (!current.isEmpty()) batches.add(current);
                current = new ArrayList<>();
                current.add(step);
                currentGroup = group;
            }
        }
        if (!current.isEmpty()) batches.add(current);
        return batches;
    }

    /** 同名但不连续的并行组重命名为 group_2、group_3，避免跨批次混淆。 */
    static void normalizeParallelGroups(List<PlannedStep> planned) {
        Map<String, Integer> occurrences = new HashMap<>();
        String lastOriginal = null;
        String effectiveGroup = null;
        for (int i = 0; i < planned.size(); i++) {
            PlannedStep item = planned.get(i);
            String original = item.parallelGroup();
            if (original == null || original.isBlank()) {
                lastOriginal = null;
                effectiveGroup = null;
                continue;
            }
            if (!original.equals(lastOriginal)) {
                int count = occurrences.merge(original, 1, Integer::sum);
                effectiveGroup = count > 1 ? original + "_" + count : original;
                lastOriginal = original;
            }
            if (!effectiveGroup.equals(original)) {
                planned.set(i, new PlannedStep(item.title(), item.tool(), item.input(),
                        item.reason(), item.requiresApproval(), effectiveGroup));
            }
        }
    }

    private static String groupOf(AgentTaskStep step) {
        String group = step.getParallelGroup();
        return group == null || group.isBlank() ? null : group.trim();
    }

    private String executeStep(AgentTask task, AgentTaskStep step, List<String> previousResults) {
        String tool = step.getTool();
        if (tool == null || tool.isBlank() || tool.equals("model")) {
            String prompt = "任务目标：" + task.getGoal()
                    + "\n约束：" + nullToEmpty(task.getConstraints())
                    + "\n当前步骤：" + step.getTitle()
                    + "\n此前结果：\n" + String.join("\n", previousResults)
                    + "\n补充提示：" + promptFromInput(step.getInputJson());
            return chatClient(task.getModelCode())
                    .prompt()
                    .system("你是任务执行助手。基于任务目标与已有结果完成当前步骤，输出可直接使用的文本，不要输出分析过程。")
                    .user(prompt)
                    .call()
                    .content();
        }
        return toolRegistry.execute(tool, task.getCreatedBy(), step.getInputJson(), task.getModelCode());
    }

    private String generateSummary(AgentTask task, List<String> results) {
        try {
            return chatClient(task.getModelCode())
                    .prompt()
                    .system("你是任务总结助手。基于各步骤结果给出简洁的最终交付物，不要输出分析过程。")
                    .user("任务目标：" + task.getGoal() + "\n步骤结果：\n" + String.join("\n", results))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("生成任务总结失败，回退为步骤结果拼接: taskId={}", task.getId(), e);
            return String.join("\n\n", results);
        }
    }

    private org.springframework.ai.chat.client.ChatClient chatClient(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) return modelProviderManager.getDefaultChatClient();
        return modelProviderManager.createChatClient(modelCode);
    }

    private List<PlannedStep> parsePlan(String raw) {
        List<PlannedStep> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        String json = raw.replaceAll("(?s)^.*?```json\\s*", "").replaceAll("(?s)```.*$", "").trim();
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) json = json.substring(start, end + 1);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode steps = root.isArray() ? root : root.path("steps");
            if (!steps.isArray()) return result;
            for (JsonNode node : steps) {
                String title = text(node, "title", "");
                if (title.isBlank()) continue;
                String tool = text(node, "tool", "model");
                if (!tool.equals("model") && !AgentToolRegistry.ALLOWED_TOOLS.contains(tool)) tool = "model";
                String input = inputJson(node.get("input"));
                String reason = text(node, "reason", "");
                boolean requiresApproval = bool(node, "requires_approval")
                        || bool(node, "requiresApproval") || toolRegistry.isSensitive(tool);
                String parallelGroup = text(node, "parallel_group", "");
                if (parallelGroup.isBlank()) parallelGroup = text(node, "parallelGroup", "");
                if (requiresApproval || toolRegistry.isSensitive(tool)) parallelGroup = "";
                result.add(new PlannedStep(title, tool, input, reason, requiresApproval, parallelGroup));
            }
        } catch (Exception e) {
            log.warn("解析任务计划 JSON 失败，将使用单步兜底: {}", e.getMessage());
        }
        return result;
    }

    private String inputJson(JsonNode input) {
        if (input == null || input.isNull()) return "{}";
        if (input.isObject()) {
            try { return objectMapper.writeValueAsString(input); } catch (Exception ignored) { return "{}"; }
        }
        String value = input.asString("");
        return value.isBlank() ? "{}" : value;
    }

    private String promptFromInput(String inputJson) {
        if (inputJson == null || inputJson.isBlank()) return "";
        try {
            JsonNode node = objectMapper.readTree(inputJson);
            return text(node, "prompt", "");
        } catch (Exception e) {
            return "";
        }
    }

    record PlannedStep(String title, String tool, String input, String reason,
                       boolean requiresApproval, String parallelGroup) {}

    private record StepOutcome(AgentTaskStep step, String error) {}

    private static boolean bool(JsonNode node, String key) {
        JsonNode value = node == null ? null : node.get(key);
        return value != null && value.isBoolean() && value.booleanValue();
    }

    private static String text(JsonNode node, String key, String fallback) {
        JsonNode value = node == null ? null : node.get(key);
        if (value == null || value.isNull() || !value.isValueNode()) return fallback;
        String s = value.asString();
        return s == null || s.isBlank() ? fallback : s;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String quote(String value) {
        return "\"" + nullToEmpty(value).replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"";
    }

    private static String shortText(String value) {
        String s = nullToEmpty(value);
        return s.length() > 2000 ? s.substring(0, 2000) + "…" : s;
    }

    private static String trim(String message, String fallback) {
        if (message == null || message.isBlank()) return fallback;
        return message.length() > 2048 ? message.substring(0, 2048) : message;
    }
}
