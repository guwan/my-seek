package top.knos.mySeek.common;

import lombok.Data;
import org.springframework.http.HttpStatus;

/**
 * 通用返回类
 *
 * @Author andy
 * @Date 2022/3/5 15:32
 */
@Data
public class Result<T>  {
    private boolean success;
    private int code;
    private String msg;
    private T data;

    /**
     * 服务器响应时间
     */
    private long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    public Result(boolean success) {
        this.timestamp = System.currentTimeMillis() / 1000;
        this.success = success;
        this.code =HttpStatus.OK.value();
    }

    public Result(boolean success, T data) {
        this.timestamp = System.currentTimeMillis() / 1000;
        this.success = success;
        this.data = data;
        this.code =HttpStatus.OK.value();
    }

    public Result(boolean success, T data, int code) {
        this.timestamp = System.currentTimeMillis() / 1000;
        this.success = success;
        this.data = data;
        this.code = code;
    }

    public Result(boolean success, String msg) {
        this.timestamp = System.currentTimeMillis() / 1000;
        this.success = success;
        this.msg = msg;
        this.code = HttpStatus.OK.value();
    }

    public Result(boolean success, String msg, int code) {
        this.timestamp = System.currentTimeMillis() / 1000;
        this.success = success;
        this.msg = msg;
        this.code = code;
    }

    public static Result<Object> ok() {
        return new Result<Object>(true);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<T>(true, data);
    }

    public static Result<Object> ok(int code) {
        return new Result<Object>(true, null, code);
    }
    public static Result<Object> okMessage(String s) {
        return new Result<Object>(true, s);
    }

    public static <T> Result<T> ok(T data, int code) {
        return new Result<T>(true, data, code);
    }

    public static Result<Object> fail() {
        return new Result<Object>(false);
    }

    public static Result<Object> fail(String msg) {
        return new Result<Object>(false, msg,HttpStatus.BAD_REQUEST.value());
    }

    public static Result<Object> fail(int code) {
        return new Result<Object>(false, null, code);
    }

    public static Result<Object> fail(int code, String msg) {
        return new Result<Object>(false, msg, code);
    }

    public static Result<Object> unauthorized() {
        return new Result<Object>(true, "未登录或登录已过期", HttpStatus.UNAUTHORIZED.value());
    }

}
