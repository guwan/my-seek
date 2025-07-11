package top.knos.mySeek.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.util.List;


public class JsonUtils {

    // 定义jackson对象
    public static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 将对象转换成json字符串。
     * <p>Title: pojoToJson</p>
     * <p>Description: </p>
     * @param data
     * @return
     */
    public static String objectToJson(Object data) {
    	try {
            return mapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
    	return null;
    }


    @SneakyThrows
    public static JsonNode strToJson(String data) {
        return mapper.readTree(data);
    }
    @SneakyThrows
    public static <T> T JsonNodeToObject(JsonNode jsonNode, Class<T> beanType) {
        return mapper.readValue(jsonNode.toString(),  beanType);
    }
    
    /**
     * 将json结果集转化为对象
     * 
     * @param jsonData json数据
     * @param beanType 对象中的object类型
     * @return
     */
    public static <T> T jsonToPojo(String jsonData, Class<T> beanType) {
        try {
            T t = mapper.readValue(jsonData, beanType);
            return t;
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 将json数据转换成pojo对象list
     * <p>Title: jsonToList</p>
     * <p>Description: </p>
     * @param jsonData
     * @param beanType
     * @return
     */
    public static <T>List<T> jsonToList(String jsonData, Class<T> beanType) {
    	JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, beanType);
    	try {
    		List<T> list = mapper.readValue(jsonData, javaType);
    		return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	return null;
    }

    /**
     * 对不合法的json转义
     * @param input
     * @return
     */
    public static String escapeJsonWithJackson(String input) {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder escaped = new StringBuilder();
        encoder.quoteAsString(input, escaped);
        return escaped.toString();
    }
    
}
