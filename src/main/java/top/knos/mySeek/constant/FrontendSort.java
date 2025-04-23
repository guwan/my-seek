package top.knos.mySeek.constant;

import lombok.Getter;
import org.springframework.data.domain.Sort;

import java.util.Map;

@Getter
public enum FrontendSort {
    /**
     * 升序
     */
    ASC("asc", "ascend"),
    /**
     * 降序
     */
    DESC("desc", "descend");

    private final String value;
    private final String frontend;

    FrontendSort(String value, String frontend) {
        this.value = value;
        this.frontend = frontend;
    }

    /**
     * 根据前端传来的排序字段，获取对应的排序方式
     * 只接受一个sort
     *
     * @param sorter
     * @return
     */
    public static Sort getSort(Map<String, String> sorter) {
        for (Map.Entry<String, String> stringStringEntry : sorter.entrySet()) {
            String key = stringStringEntry.getKey();
            String value = stringStringEntry.getValue();
            if (value.equals(ASC.getFrontend())) {
                return Sort.by(Sort.Direction.ASC, key);
            } else {
                return Sort.by(Sort.Direction.DESC, key);
            }
        }
        return Sort.by(Sort.Direction.DESC, "id");
    }

}
