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
    private final TaskEventStreamService streamService;

    public void record(String taskId, String stepId, String type, String payload, Long userId) {
        AgentTaskEvent event = new AgentTaskEvent();
        event.setId(UUID.randomUUID().toString());
        Long lastSeq = eventRepository.findTopByTaskIdOrderBySeqDesc(taskId)
                .map(AgentTaskEvent::getSeq).orElse(0L);
        event.setSeq((lastSeq == null ? 0L : lastSeq) + 1);
        event.setTaskId(taskId);
        event.setStepId(stepId);
        event.setEventType(type);
        event.setPayload(payload == null ? null : payload.substring(0, Math.min(payload.length(), 4000)));
        event.setCreatedBy(userId);
        eventRepository.save(event);
        streamService.push(event);
    }
}
