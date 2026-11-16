package top.knos.mySeek.primary.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.primary.service.ChatService;
import top.knos.mySeek.primary.vo.ChatRequest;
import top.knos.mySeek.primary.vo.MessageVo;
import top.knos.mySeek.primary.vo.ModelInfo;
import top.knos.mySeek.support.vo.ChatInfoVo;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("chat")
    public Result<String> chat(@Valid @RequestBody ChatRequest chatRequest) {
        return Result.ok(chatService.chat(chatRequest));
    }

    /**
     * 流式对话：返回纯文本增量，前端直接按 chunk 读取。
     */
    @PostMapping("chat/stream")
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest chatRequest) {
        return chatService.chatStream(chatRequest);
    }

    /**
     * 会话摘要（标题）。
     */
    @GetMapping("chat/info")
    public Result<ChatInfoVo> chatInfo(@RequestParam String conversationId) {
        return Result.ok(chatService.chatInfo(conversationId));
    }

    /**
     * 会话历史消息。
     */
    @GetMapping("chat/history")
    public Result<List<MessageVo>> chatHistory(@RequestParam String conversationId) {
        return Result.ok(chatService.history(conversationId));
    }

    /**
     * 当前用户的会话列表。
     */
    @GetMapping("chat/item/list")
    public Result<List<ChatInfoVo>> chatItemList() {
        return Result.ok(chatService.chatItemList());
    }

    /**
     * 可用模型列表。
     */
    @GetMapping("models")
    public Result<List<ModelInfo>> getAvailableModels() {
        return Result.ok(chatService.getAvailableModels());
    }
}
