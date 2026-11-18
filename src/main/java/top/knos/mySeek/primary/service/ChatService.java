package top.knos.mySeek.primary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import top.knos.mySeek.primary.model.ModelProviderManager;
import top.knos.mySeek.primary.vo.ChatRequest;
import top.knos.mySeek.primary.vo.MessageVo;
import top.knos.mySeek.primary.vo.ModelInfo;
import top.knos.mySeek.security.domain.User;
import top.knos.mySeek.security.support.SecurityUtils;
import top.knos.mySeek.support.vo.ChatInfoVo;
import top.knos.mySeek.support.vo.WebSearchResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 聊天服务。
 * <p>Spring AI 2.0 起，聊天记忆统一由 {@link MessageChatMemoryAdvisor} 负责读写，
 * 会话 ID 通过请求上下文（{@link ChatMemory#CONVERSATION_ID}）传递。</p>
 */
@Service
@Slf4j
public class ChatService {

    /** 记忆 Advisor 的执行顺序 */
    private static final int MEMORY_ADVISOR_ORDER = 50;
    /** 会话标题最大长度 */
    private static final int TITLE_MAX_LENGTH = 100;
    /** 内存中保留的历史消息条数 */
    private static final int HISTORY_PAGE_SIZE = 50;
    /** 流式首帧中携带联网来源的标记，前端据此解析并剔除该帧 */
    public static final String SOURCES_FRAME_PREFIX = "\u0000MYSEEK_SOURCES:";
    public static final String SOURCES_FRAME_SUFFIX = "\u0000\n";

    private final ChatClient chatClient;
    private final RedisChatMemory redisChatMemory;
    private final ModelProviderManager modelProviderManager;
    private final WebSearchService webSearchService;

    public ChatService(ChatClient chatClient, RedisChatMemory redisChatMemory,
                       ModelProviderManager modelProviderManager, WebSearchService webSearchService) {
        this.chatClient = chatClient;
        this.redisChatMemory = redisChatMemory;
        this.modelProviderManager = modelProviderManager;
        this.webSearchService = webSearchService;
    }

    private ChatClient getChatClient(String modelIdentifier, boolean enableThinking) {
        if (modelIdentifier == null || modelIdentifier.trim().isEmpty()) {
            return chatClient;
        }

        try {
            return modelProviderManager.createChatClient(modelIdentifier, enableThinking);
        } catch (Exception e) {
            log.warn("无法创建指定模型的 ChatClient: {}, 使用默认客户端", modelIdentifier, e);
            return chatClient;
        }
    }

    /**
     * 构建带记忆 Advisor 的请求。会话 ID 已在请求上下文中声明，Advisor 会自动完成
     * 历史读取、用户消息写入与 AI 回复写入，避免重复写入。
     */
    private ChatClient.ChatClientRequestSpec promptWithMemory(ChatClient client, String scopedConversationId,
                                                              String userInput, String searchContext) {
        var spec = client.prompt()
                .advisors(advisor -> advisor
                        .advisors(MessageChatMemoryAdvisor.builder(redisChatMemory)
                                .order(MEMORY_ADVISOR_ORDER)
                                .build())
                        .param(ChatMemory.CONVERSATION_ID, scopedConversationId));

        if (searchContext != null && !searchContext.isBlank()) {
            spec = spec.system(searchContext);
        }
        return spec.user(userInput);
    }

    public String chat(ChatRequest request) {
        String userScope = userScope();
        String scopedConversationId = scopedConversationId(userScope, request.getConversationId());
        String userInput = request.getMessage();

        ChatClient clientToUse = getChatClient(request.getModel(), request.isEnableThinking());
        WebSearchResult searchResult = request.isWebSearch()
                ? webSearchService.search(userInput) : new WebSearchResult();

        var response = promptWithMemory(clientToUse, scopedConversationId, userInput, searchResult.getContext()).call();

        String aiReply = response.content();
        saveChatItem(userScope, request.getConversationId(), userInput);
        return aiReply;
    }

    public Flux<String> chatStream(ChatRequest request) {
        // SecurityContext 是 ThreadLocal，流的完成/取消回调运行在 Reactor 线程上，
        // 必须在请求线程同步取出用户作用域并在闭包中使用。
        String userScope = userScope();
        String scopedConversationId = scopedConversationId(userScope, request.getConversationId());
        String userInput = request.getMessage();

        ChatClient clientToUse = getChatClient(request.getModel(), request.isEnableThinking());
        WebSearchResult searchResult = request.isWebSearch()
                ? webSearchService.search(userInput) : new WebSearchResult();

        var response = promptWithMemory(clientToUse, scopedConversationId, userInput, searchResult.getContext())
                .stream()
                .content();

        StringBuilder sb = new StringBuilder();

        Flux<String> contentFlux = response
                .doOnNext(sb::append)
                .doOnComplete(() -> {
                    log.debug("对话回复: {}", sb);
                    // AI 回复由记忆 Advisor 自动落库，此处只需登记会话条目
                    saveChatItem(userScope, request.getConversationId(), userInput);
                })
                // 客户端中途断开时 Advisor 不会落库，此处补写已生成的部分内容
                .doOnCancel(() -> persistPartialReply(scopedConversationId, sb))
                .doOnError(e -> log.warn("流式对话异常: {}", e.getMessage()));

        // 开启联网时，首帧始终下发元数据（含 performed/engine/sources），
        // 这样即使没有搜到结果，前端也能明确提示"已联网搜索"，而不是毫无反馈。
        if (request.isWebSearch()) {
            String frame = SOURCES_FRAME_PREFIX + searchResult.getMetadataJson() + SOURCES_FRAME_SUFFIX;
            return Flux.concat(Flux.just(frame), contentFlux);
        }
        return contentFlux;
    }

    /**
     * 客户端中断时保存已生成的部分回复。
     */
    private void persistPartialReply(String scopedConversationId, StringBuilder sb) {
        String reply = sb.toString();
        if (reply.isEmpty()) {
            return;
        }
        try {
            redisChatMemory.add(scopedConversationId, List.of(new AssistantMessage(reply)));
        } catch (Exception e) {
            log.warn("保存 AI 回复到会话记忆失败", e);
        }
    }

    public ChatInfoVo chatInfo(String conversationId) {
        String scopedConversationId = scopedConversationId(userScope(), conversationId);
        MessageVo message = redisChatMemory.getFirstOneVo(scopedConversationId);
        String text = message == null ? null : message.getContent();
        ChatInfoVo vo = new ChatInfoVo();
        vo.setId(conversationId);
        vo.setTitle(text == null ? "" : text.substring(0, Math.min(TITLE_MAX_LENGTH, text.length())));
        vo.setCreateAt(new Date());
        return vo;
    }

    public List<MessageVo> history(String conversationId) {
        return redisChatMemory.getVo(scopedConversationId(userScope(), conversationId), HISTORY_PAGE_SIZE);
    }

    public List<ChatInfoVo> chatItemList() {
        return redisChatMemory.getChatItems(userScope());
    }

    /**
     * 获取所有可用的模型列表
     */
    public List<ModelInfo> getAvailableModels() {
        List<ModelInfo> allModels = new ArrayList<>();

        Map<String, List<String>> providerModels = modelProviderManager.getAllAvailableModels();

        for (Map.Entry<String, List<String>> entry : providerModels.entrySet()) {
            String provider = entry.getKey();
            for (String modelName : entry.getValue()) {
                ModelInfo modelInfo = new ModelInfo();
                modelInfo.setName(provider + ":" + modelName);
                modelInfo.setProvider(provider);
                allModels.add(modelInfo);
            }
        }

        return allModels;
    }

    /**
     * 会话 ID 按当前用户隔离，避免不同用户之间串会话。
     */
    private String scopedConversationId(String userScope, String conversationId) {
        return userScope + ":" + conversationId;
    }

    private String userScope() {
        User user = SecurityUtils.getUser();
        return user == null || user.getId() == null ? "anonymous" : String.valueOf(user.getId());
    }

    private void saveChatItem(String userScope, String conversationId, String userInput) {
        ChatInfoVo chatInfoVo = new ChatInfoVo();
        chatInfoVo.setId(conversationId);
        chatInfoVo.setTitle(userInput == null ? ""
                : userInput.substring(0, Math.min(TITLE_MAX_LENGTH, userInput.length())));
        chatInfoVo.setCreateAt(new Date());
        redisChatMemory.addChatItem(userScope, chatInfoVo);
    }
}
