package top.knos.mySeek.config;

import lombok.SneakyThrows;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.utils.JsonUtils;

/**
 * @author 卢文武
 * @date 2021/7/8 10:10 上午
 */
@RestControllerAdvice(basePackages = "top.knos.mySeek")
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> converterType) {
        //return true;
        return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @SneakyThrows
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter methodParameter,
                                  MediaType mediaType,
                                  Class<? extends HttpMessageConverter<?>> aClass,
                                  ServerHttpRequest serverHttpRequest,
                                  ServerHttpResponse serverHttpResponse) {
        // 这里需要过滤掉swagger的相关返回
        /*if (body instanceof Result
                || body instanceof Json
                || body instanceof UiConfiguration
                || (body instanceof ArrayList && ((ArrayList) body).get(0) instanceof SwaggerResource))
            return body;*/
        // TODO: 文件上传下载要做处理，不能封装，直接返回
        if (body instanceof Resource) {
            return body;
        } else if (body instanceof String) {
            return JsonUtils.objectToJson(Result.ok(body));
        } else if (body instanceof Result) {
            return body;
        }
        return Result.ok(body);
    }
}
