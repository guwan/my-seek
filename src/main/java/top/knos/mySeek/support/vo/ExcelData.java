package top.knos.mySeek.support.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExcelData {
    private Map<Integer,String>  headers;
    private List<Map<Integer,String>> datas;
}
