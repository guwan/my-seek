package top.knos.mySeek.primary.vo;

import lombok.Data;

@Data
public class ModelInfo {
    private String name;
    private String provider;
    private String size;
    private String modified_at;
    private String digest;
    
    public ModelInfo() {}
    
    public ModelInfo(String name) {
        this.name = name;
    }
}
