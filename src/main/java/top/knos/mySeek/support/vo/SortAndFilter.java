package top.knos.mySeek.support.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper=true)
public class SortAndFilter extends PageBaseReq {
    private Map<String,String> sorter;
    private Map<String, List<String>> filter;
}
