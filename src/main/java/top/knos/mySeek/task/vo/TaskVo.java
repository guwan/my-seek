package top.knos.mySeek.task.vo;

import lombok.Data;
import top.knos.mySeek.task.entity.AgentTask;
import top.knos.mySeek.task.entity.AgentTaskEvent;
import top.knos.mySeek.task.entity.AgentTaskStep;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskVo {
    private String id;
    private String title;
    private String goal;
    private String constraints;
    private String modelCode;
    private AgentTask.Status status;
    private Integer currentStep;
    private Integer maxSteps;
    private String finalSummary;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StepVo> steps;
    private List<EventVo> events;

    @Data
    public static class StepVo {
        private String id;
        private Integer seq;
        private String title;
        private String tool;
        private String inputJson;
        private String parallelGroup;
        private Boolean requiresApproval;
        private Boolean approved;
        private AgentTaskStep.Status status;
        private String resultText;
        private String errorMessage;

        public static StepVo from(AgentTaskStep step) {
            StepVo vo = new StepVo();
            vo.setId(step.getId());
            vo.setSeq(step.getSeq());
            vo.setTitle(step.getTitle());
            vo.setTool(step.getTool());
            vo.setInputJson(step.getInputJson());
            vo.setParallelGroup(step.getParallelGroup());
            vo.setRequiresApproval(step.getRequiresApproval());
            vo.setApproved(step.getApproved());
            vo.setStatus(step.getStatus());
            vo.setResultText(step.getResultText());
            vo.setErrorMessage(step.getErrorMessage());
            return vo;
        }
    }

    @Data
    public static class EventVo {
        private String id;
        private Long seq;
        private String stepId;
        private String eventType;
        private String payload;
        private LocalDateTime createdAt;

        public static EventVo from(AgentTaskEvent event) {
            EventVo vo = new EventVo();
            vo.setId(event.getId());
            vo.setSeq(event.getSeq());
            vo.setStepId(event.getStepId());
            vo.setEventType(event.getEventType());
            vo.setPayload(event.getPayload());
            vo.setCreatedAt(event.getCreatedAt());
            return vo;
        }
    }

    public static TaskVo from(AgentTask task, List<AgentTaskStep> steps, List<AgentTaskEvent> events) {
        TaskVo vo = new TaskVo();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setGoal(task.getGoal());
        vo.setConstraints(task.getConstraints());
        vo.setModelCode(task.getModelCode());
        vo.setStatus(task.getStatus());
        vo.setCurrentStep(task.getCurrentStep());
        vo.setMaxSteps(task.getMaxSteps());
        vo.setFinalSummary(task.getFinalSummary());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        vo.setSteps(steps.stream().map(StepVo::from).toList());
        vo.setEvents(events.stream().map(EventVo::from).toList());
        return vo;
    }
}
