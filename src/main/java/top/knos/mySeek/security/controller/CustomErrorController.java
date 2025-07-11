package top.knos.mySeek.security.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import top.knos.mySeek.common.Result;

import java.util.Map;
import java.util.Objects;

/**
 * spring security 异常处理
 * UniformReponseHandler 无法拦截pring security的异常
 */
@Slf4j
@RestController
public class CustomErrorController implements ErrorController {
    private static final String PATH = "/error";

    @Autowired
    private ErrorAttributes errorAttributes;

    @RequestMapping(value = PATH)
    Result<Object> error(HttpServletRequest request, HttpServletResponse response) {
        // Appropriate HTTP response code (e.g. 404 or 500) is automatically set by Spring.
        // Here we just define response body.
        //response.setContentType("application/json");
        Map<String, Object> errorMap = getErrorAttributes(request);
        //定义返回格式
        Result<Object> result = new Result<>();
        result.setMsg(Objects.requireNonNull(errorMap.get("message")).toString());
        result.setCode(Integer.parseInt(Objects.requireNonNull(errorMap.get("status")).toString()));
        response.setStatus(HttpServletResponse.SC_OK);
        return result;
    }


    /**
     * 获取请求参数
     *
     * @param request
     * @return
     */
    private Map<String, Object> getErrorAttributes(HttpServletRequest request) {
        WebRequest requestAttributes = new ServletWebRequest(request);
        return errorAttributes.getErrorAttributes(requestAttributes, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE));
    }

}