//package top.knos.mySeek.primary.support;
//
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.model.ModelOptionsUtils;
//import org.springframework.stereotype.Component;
//import top.knos.mySeek.primary.domain.ChatHistory;
//import top.knos.mySeek.primary.service.ChatHistoryService;
//import top.knos.mySeek.primary.vo.ChatRequest;
//
//@Component
//public class HistoryAdvisor {
//
//    private final ChatHistoryService chatHistoryService;
//
//    public HistoryAdvisor(ChatHistoryService chatHistoryService) {
//        this.chatHistoryService = chatHistoryService;
//    }
//
//    public void beforeRequest(ChatClient.AdvisorSpec advisorSpec) {
//        // 获取历史记录并追加到当前 Prompt
//        String history = chatHistoryService.getHistory(advisorSpec.getSessionId());
//        request.getPrompt().addMessage(new SystemPrompt(history));
//    }
//}