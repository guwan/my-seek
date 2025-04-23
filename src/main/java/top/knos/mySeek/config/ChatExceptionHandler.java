package top.knos.mySeek.config;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ChatExceptionHandler {
    
    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<ErrorResponse> handleAiError(NonTransientAiException ex) {
        return ResponseEntity.status(503)
                .body(new ErrorResponse("AI服务暂不可用"));
    }
    
    record ErrorResponse(String error) {}
}