package top.knos.mySeek.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.utils.JsonUtils;

/**
 * 统一包装 Controller 返回值为 {@link Result} 结构。
 * <p>注意：Spring Framework 7 的 Jackson 转换器基类为
 * {@link AbstractJacksonHttpMessageConverter}（同时兼容 Jackson 2 / Jackson 3）。</p>
 *
 * @author 卢文武
 * @date 2021/7/8 10:10 上午
 */
@Slf4j
@RestControllerAdvice(basePackages = "top.knos.mySeek")
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType)) {
            return false;
        }
        // 流式响应不参与统一包装
        Class<?> returnType = methodParameter.getParameterType();
        return !Flux.class.isAssignableFrom(returnType)
                && !StreamingResponseBody.class.isAssignableFrom(returnType)
                && !SseEmitter.class.isAssignableFrom(returnType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter methodParameter,
                                  MediaType mediaType,
                                  Class<? extends HttpMessageConverter<?>> aClass,
                                  ServerHttpRequest serverHttpRequest,
                                  ServerHttpResponse serverHttpResponse) {
        // TODO: 文件上传下载要做处理，不能封装，直接返回
        if (body == null) {
            return null;
        }
        if (body instanceof Resource) {
            return body;
        } else if (body instanceof Result) {
            return body;
        } else if (body instanceof String) {
            return JsonUtils.objectToJson(Result.ok(body));
        }
        return Result.ok(body);
    }
}
