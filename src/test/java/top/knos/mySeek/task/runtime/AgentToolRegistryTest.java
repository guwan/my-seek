package top.knos.mySeek.task.runtime;

import org.junit.jupiter.api.Test;
import top.knos.mySeek.primary.service.WebSearchService;
import top.knos.mySeek.rag.service.KnowledgeBaseService;
import top.knos.mySeek.writing.service.WritingService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentToolRegistryTest {

    @Test
    void mcpToolIsSensitiveAndDispatchedToBridge() {
        McpToolBridge bridge = mock(McpToolBridge.class);
        when(bridge.toolNames()).thenReturn(List.of("mcp_weather_get_weather"));
        when(bridge.isMcpTool("mcp_weather_get_weather")).thenReturn(true);
        when(bridge.isSensitive("mcp_weather_get_weather")).thenReturn(true);
        when(bridge.execute("mcp_weather_get_weather", "{\"city\":\"北京\"}")).thenReturn("晴 25℃");
        AgentToolRegistry registry = registry(bridge);

        assertTrue(registry.availableTools().contains("mcp_weather_get_weather"));
        assertTrue(registry.isSensitive("mcp_weather_get_weather"));
        assertEquals("晴 25℃", registry.execute("mcp_weather_get_weather", 1L, "{\"city\":\"北京\"}", null));
        verify(bridge).execute("mcp_weather_get_weather", "{\"city\":\"北京\"}");
        verify(bridge, never()).isSensitive("novel_write_draft");
    }

    @Test
    void builtInSensitiveToolStillRequiresApproval() {
        AgentToolRegistry registry = registry(mock(McpToolBridge.class));

        assertTrue(registry.isSensitive("novel_write_draft"));
        assertFalse(registry.isSensitive("knowledge_search"));
        assertTrue(registry.availableTools().containsAll(AgentToolRegistry.ALLOWED_TOOLS));
    }

    @Test
    void unknownToolIsRejected() {
        AgentToolRegistry registry = registry(mock(McpToolBridge.class));

        assertThrows(IllegalArgumentException.class,
                () -> registry.execute("shell", 1L, "{}", null));
    }

    private AgentToolRegistry registry(McpToolBridge bridge) {
        return new AgentToolRegistry(mock(KnowledgeBaseService.class),
                mock(WritingService.class), mock(WebSearchService.class), bridge);
    }
}
