package top.knos.mySeek.agent.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** 连接器（Connector/工具）：工具来源，内置工具组或外部 MCP 服务器。 */
@Data
@Entity
@Table(name = "connector_definitions")
public class ConnectorDefinition {

    public enum Type {
        BUILTIN, MCP
    }

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "code", length = 64, nullable = false)
    private String code;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon", length = 64)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 32, nullable = false)
    private Type type = Type.BUILTIN;

    /** 该连接器提供的工具名 JSON 数组 */
    @Column(name = "tools_json", columnDefinition = "TEXT")
    private String toolsJson;

    /** 连接配置（MCP url/command 等）JSON，敏感信息不落此表 */
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "builtin")
    private Boolean builtin = false;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
