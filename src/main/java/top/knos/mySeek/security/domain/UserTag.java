package top.knos.mySeek.security.domain;

import jakarta.persistence.*;

/**
 * TODO
 *
 * @Author 技术古玩
 * @Date 2021/4/27 16:39
 */
@Entity(name = "sso_user_tag")
public class UserTag {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    private String label;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
