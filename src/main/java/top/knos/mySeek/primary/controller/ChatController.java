package top.knos.mySeek.primary.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.primary.vo.ChatRequest;
import top.knos.mySeek.primary.service.ChatService;
import top.knos.mySeek.support.vo.ChatInfoVo;

@RestController
@RequestMapping("api/ai")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("chat")
    public Result<String> chat(@RequestBody ChatRequest chatRequest) {

        return Result.ok(chatService.chat(chatRequest));
    }

    @PostMapping("chat/stream")
    public Flux<String> generateStream(@RequestBody ChatRequest chatRequest) {
        return chatService.chatStream(chatRequest);
    }
    @GetMapping("chat/info")
    public Result<ChatInfoVo> generateStreamRes(@RequestParam String conversationId) {
        return Result.ok(chatService.chatInfo(conversationId));
    }

}
