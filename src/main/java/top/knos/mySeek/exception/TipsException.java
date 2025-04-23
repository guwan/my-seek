package top.knos.mySeek.exception;

import org.springframework.http.HttpStatus;

public class TipsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String message;
  private final Integer code;

  public TipsException(String message) {
    this(message,HttpStatus.INTERNAL_SERVER_ERROR.value());
  }
  public TipsException(String message, Integer code ) {
    this.message = message;
    this.code  = code;
  }

  @Override
  public String getMessage() {
    return message;
  }

  public Integer getCode() {
    return code;
  }
}
