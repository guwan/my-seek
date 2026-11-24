package top.knos.mySeek.task.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.knos.mySeek.task.entity.AgentTaskEvent;
import top.knos.mySeek.task.repository.AgentTaskEventRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskEventService {

    private final AgentTaskEventRepository eventRepository;

    public void record(String taskId, String stepId, String type, String payload, Long userId) {
        AgentTaskEvent event = new AgentTaskEvent();
        event.setId(UUID.randomUUID().toString());
        event.setTaskId(taskId);
        event.setStepId(stepId);
        event.setEventType(type);
        event.setPayload(payload == null ? null : payload.substring(0, Math.min(payload.length(), 4000)));
        event.setCreatedBy(userId);
        eventRepository.save(event);
    }
}
