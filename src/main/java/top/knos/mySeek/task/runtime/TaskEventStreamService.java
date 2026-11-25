package top.knos.mySeek.task.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.knos.mySeek.task.entity.AgentTaskEvent;
import top.knos.mySeek.task.repository.AgentTaskEventRepository;
import top.knos.mySeek.task.vo.TaskVo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 任务事件的 SSE 订阅与分发。每个任务可有多个订阅者，事件写入后立即推送。
 */
@Slf4j
@Service
public class TaskEventStreamService {

    private static final long TIMEOUT_MS = 10 * 60 * 1000L;

    private final AgentTaskEventRepository eventRepository;
    private final Map<String, List<EmitterHandle>> subscribers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "task-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public TaskEventStreamService(AgentTaskEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public SseEmitter subscribe(String taskId, long after) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        EmitterHandle handle = new EmitterHandle(emitter, taskId);
        subscribers.computeIfAbsent(taskId, key -> new CopyOnWriteArrayList<>()).add(handle);
        Runnable cleanup = () -> remove(taskId, handle);
        emitter.onCompletion(cleanup);
        emitter.onError(error -> cleanup.run());
        emitter.onTimeout(cleanup);
        handle.heartbeat = heartbeat.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("ping").data("keepalive"));
            } catch (Exception e) {
                cleanup.run();
            }
        }, 15, 15, TimeUnit.SECONDS);

        // 先补发订阅前产生的事件；前端按 seq 去重，重复补发是安全的
        List<AgentTaskEvent> missed = eventRepository
                .findByTaskIdAndSeqGreaterThanOrderBySeqAsc(taskId, after);
        for (AgentTaskEvent event : missed) {
            send(handle, event);
        }
        return emitter;
    }

    public void push(AgentTaskEvent event) {
        List<EmitterHandle> handles = subscribers.get(event.getTaskId());
        if (handles == null || handles.isEmpty()) return;
        for (EmitterHandle handle : handles) {
            send(handle, event);
        }
    }

    private void send(EmitterHandle handle, AgentTaskEvent event) {
        try {
            handle.emitter.send(SseEmitter.event()
                    .name("task-event")
                    .data(TaskVo.EventVo.from(event)));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 推送失败，移除订阅: {}", e.getMessage());
            remove(handle.taskId, handle);
        }
    }

    private void remove(String taskId, EmitterHandle handle) {
        List<EmitterHandle> handles = subscribers.get(taskId);
        if (handles != null) handles.remove(handle);
        if (handle.heartbeat != null) handle.heartbeat.cancel(false);
        if (handles != null && handles.isEmpty()) subscribers.remove(taskId, handles);
    }

    private static class EmitterHandle {
        private final SseEmitter emitter;
        private final String taskId;
        private ScheduledFuture<?> heartbeat;

        private EmitterHandle(SseEmitter emitter, String taskId) {
            this.emitter = emitter;
            this.taskId = taskId;
        }
    }
}
