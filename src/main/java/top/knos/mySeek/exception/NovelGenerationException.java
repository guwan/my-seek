package top.knos.mySeek.exception;

// 自定义异常类
public class NovelGenerationException extends RuntimeException {
    public NovelGenerationException(String message) {
        super(message);
    }
}