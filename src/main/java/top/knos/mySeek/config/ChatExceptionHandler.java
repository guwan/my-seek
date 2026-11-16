package top.knos.mySeek.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.knos.mySeek.common.Result;
import top.knos.mySeek.exception.CustomHttpException;
import top.knos.mySeek.exception.InvalidOutlineException;
import top.knos.mySeek.exception.NovelGenerationException;
import top.knos.mySeek.exception.TipsException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：统一异常到 HTTP 状态码与 {@link Result} 的映射，
 * 保证前端始终能拿到 {@code success=false} 与正确的业务码。
 */
@Slf4j
@RestControllerAdvice(basePackages = "top.knos.mySeek")
public class ChatExceptionHandler {

    /**
     * 业务提示异常（如验证码错误、参数不合法等）
     */
    @ExceptionHandler(TipsException.class)
    public ResponseEntity<Result<Object>> handleTips(TipsException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(Result.fail(status.value(), ex.getMessage()));
    }

    /**
     * 自定义 HTTP 异常
     */
    @ExceptionHandler(CustomHttpException.class)
    public ResponseEntity<Result<Object>> handleCustomHttp(CustomHttpException ex) {
        HttpStatus status = ex.getHttpStatus() == null ? HttpStatus.INTERNAL_SERVER_ERROR : ex.getHttpStatus();
        return ResponseEntity.status(status).body(Result.fail(status.value(), ex.getMessage()));
    }

    /**
     * 小说生成相关异常
     */
    @ExceptionHandler({InvalidOutlineException.class, NovelGenerationException.class})
    public ResponseEntity<Result<Object>> handleNovel(Exception ex) {
        log.warn("小说生成失败: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Result.fail(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage()));
    }

    /**
     * AI 服务不可用
     */
    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<Result<Object>> handleAiError(NonTransientAiException ex) {
        log.error("AI 服务暂不可用", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.fail(HttpStatus.SERVICE_UNAVAILABLE.value(), "AI服务暂不可用"));
    }

    /**
     * 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(HttpStatus.BAD_REQUEST.value(), msg.isEmpty() ? "参数校验失败" : msg));
    }

    /**
     * 权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(HttpStatus.FORBIDDEN.value(), "没有访问权限"));
    }

    /**
     * 兜底异常处理，避免堆栈直接暴露给前端
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception ex) {
        log.error("未预期的异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统繁忙，请稍后再试"));
    }
}

