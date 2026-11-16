package top.knos.mySeek.security.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.knos.mySeek.common.Result;

/**
 * 统一错误页控制器：保持 HTTP 状态码不变，并以统一的 {@link Result} 结构返回错误信息。
 */
@Slf4j
@RestController
public class CustomErrorController implements ErrorController {

    private static final String PATH = "/error";

    @RequestMapping(value = PATH)
    public Result<Object> error(HttpServletRequest request, HttpServletResponse response) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttr instanceof Integer code ? code : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        Object messageAttr = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        Result<Object> result = new Result<>();
        result.setSuccess(false);
        result.setCode(status);
        result.setMsg(messageAttr instanceof String message && !message.isBlank() ? message : "系统错误");
        // 保持真实状态码，便于前端按 code 区分处理
        response.setStatus(status);
        return result;
    }
}
