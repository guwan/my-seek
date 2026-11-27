package top.knos.mySeek.task.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MCP 工具桥接：把 Spring AI 自动发现的 MCP 服务器工具暴露给任务中心的工具白名单。
 * 未配置 MCP 服务器时 Spring 不创建 SyncMcpToolCallbackProvider，此处以空列表降级，不影响启动。
 */
@Slf4j
@Service
public class McpToolBridge {

    public static final String TOOL_PREFIX = "mcp_";

    private final SyncMcpToolCallbackProvider provider;

    public McpToolBridge(ObjectProvider<SyncMcpToolCallbackProvider> provider) {
        this.provider = provider.getIfAvailable();
        if (this.provider != null) {
            int count = tools().size();
            if (count > 0) {
                log.info("MCP 工具桥接已启用，发现 {} 个外部工具", count);
            } else {
                log.debug("MCP 客户端 Bean 存在但未发现工具，任务中心按内置工具运行");
            }
        }
    }

    public boolean enabled() {
        return provider != null && !tools().isEmpty();
    }

    public boolean isMcpTool(String tool) {
        return tool != null && tool.startsWith(TOOL_PREFIX);
    }

    /** 已发现的 MCP 工具默认视为敏感操作，执行前需用户批准。 */
    public boolean isSensitive(String tool) {
        return isMcpTool(tool) && find(tool) != null;
    }

    public String execute(String tool, String inputJson) {
        ToolCallback callback = find(tool);
        if (callback == null) {
            throw new IllegalArgumentException("MCP 工具不存在或不可用: " + tool);
        }
        String args = inputJson == null || inputJson.isBlank() ? "{}" : inputJson;
        return callback.call(args);
    }

    /** 供规划 prompt 使用：列出外部工具的名称、描述与参数 schema。 */
    public String describeTools() {
        if (!enabled()) return "";
        StringBuilder sb = new StringBuilder();
        for (ToolCallback callback : tools()) {
            var def = callback.getToolDefinition();
            sb.append("- ").append(def.name()).append("：").append(nullToEmpty(def.description()))
                    .append("；参数 schema=").append(trim(nullToEmpty(def.inputSchema()))).append("\n");
        }
        return sb.toString();
    }

    public List<String> toolNames() {
        return tools().stream().map(c -> c.getToolDefinition().name()).toList();
    }

    private ToolCallback find(String tool) {
        if (!isMcpTool(tool)) return null;
        for (ToolCallback callback : tools()) {
            if (tool.equals(callback.getToolDefinition().name())) return callback;
        }
        return null;
    }

    private List<ToolCallback> tools() {
        if (provider == null) return Collections.emptyList();
        ToolCallback[] callbacks;
        try {
            callbacks = provider.getToolCallbacks();
        } catch (Exception e) {
            log.warn("读取 MCP 工具列表失败，已按空列表处理: {}", e.getMessage());
            return Collections.emptyList();
        }
        if (callbacks == null) return Collections.emptyList();
        List<ToolCallback> result = new ArrayList<>();
        for (ToolCallback callback : callbacks) {
            if (callback != null && callback.getToolDefinition() != null
                    && callback.getToolDefinition().name() != null
                    && !callback.getToolDefinition().name().isBlank()) {
                result.add(callback);
            }
        }
        return result;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String trim(String value) {
        return value.length() > 1200 ? value.substring(0, 1200) + "…" : value;
    }
}
