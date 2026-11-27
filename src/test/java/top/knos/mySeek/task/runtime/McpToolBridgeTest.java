package top.knos.mySeek.task.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpToolBridgeTest {

    @Test
    void noProviderDegradesToEmptyAndNotSensitive() {
        McpToolBridge bridge = new McpToolBridge(emptyProvider());

        assertFalse(bridge.enabled());
        assertTrue(bridge.toolNames().isEmpty());
        assertTrue(bridge.describeTools().isBlank());
        assertFalse(bridge.isSensitive("mcp_server_tool"));
        assertFalse(bridge.isSensitive("novel_write_draft"));
    }

    @Test
    void discoversMcpToolsAndRoutesExecution() {
        ToolCallback callback = callback("mcp_weather_get_weather", "查询天气", "{\"type\":\"object\"}");
        when(callback.call("{\"city\":\"北京\"}")).thenReturn("晴 25℃");
        McpToolBridge bridge = new McpToolBridge(providerWith(callback));

        assertTrue(bridge.enabled());
        assertEquals(List.of("mcp_weather_get_weather"), bridge.toolNames());
        assertTrue(bridge.isMcpTool("mcp_weather_get_weather"));
        assertTrue(bridge.isSensitive("mcp_weather_get_weather"));
        assertTrue(bridge.describeTools().contains("查询天气"));
        assertEquals("晴 25℃", bridge.execute("mcp_weather_get_weather", "{\"city\":\"北京\"}"));
        verify(callback).call("{\"city\":\"北京\"}");
    }

    @Test
    void executeUnknownMcpToolIsRejected() {
        McpToolBridge bridge = new McpToolBridge(providerWith(
                callback("mcp_weather_get_weather", "查询天气", "{}")));

        assertThrows(IllegalArgumentException.class,
                () -> bridge.execute("mcp_missing_tool", "{}"));
        assertFalse(bridge.isMcpTool("knowledge_search"));
    }

    @Test
    void emptyInputsDefaultToEmptyJsonObject() {
        ToolCallback callback = callback("mcp_calc_add", "加法", "{}");
        when(callback.call("{}")).thenReturn("3");
        McpToolBridge bridge = new McpToolBridge(providerWith(callback));

        assertEquals("3", bridge.execute("mcp_calc_add", null));
        assertEquals("3", bridge.execute("mcp_calc_add", "  "));
        verify(callback, times(2)).call("{}");
    }

    @Test
    void providerFailureDegradesGracefully() {
        SyncMcpToolCallbackProvider provider = mock(SyncMcpToolCallbackProvider.class);
        when(provider.getToolCallbacks()).thenThrow(new IllegalStateException("MCP 服务器断开"));
        ObjectProvider<SyncMcpToolCallbackProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        McpToolBridge bridge = new McpToolBridge(objectProvider);

        assertFalse(bridge.enabled());
        assertTrue(bridge.toolNames().isEmpty());
    }

    private ToolCallback callback(String name, String description, String schema) {
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn(name);
        when(definition.description()).thenReturn(description);
        when(definition.inputSchema()).thenReturn(schema);
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        return callback;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<SyncMcpToolCallbackProvider> emptyProvider() {
        ObjectProvider<SyncMcpToolCallbackProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(null);
        return objectProvider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<SyncMcpToolCallbackProvider> providerWith(ToolCallback... callbacks) {
        SyncMcpToolCallbackProvider provider = mock(SyncMcpToolCallbackProvider.class);
        when(provider.getToolCallbacks()).thenReturn(callbacks);
        ObjectProvider<SyncMcpToolCallbackProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        return objectProvider;
    }
}
