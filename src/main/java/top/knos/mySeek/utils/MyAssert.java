package top.knos.mySeek.utils;

import org.springframework.lang.Nullable;

/**
 * @author 卢文武(01380604)
 * @date 2018/11/15 11:20
 */
public class MyAssert {

    public static void hasLengthMin(@Nullable String text, int length, String message) {
        if (text == null || text.trim().length() < length) {
            throw new IllegalArgumentException(message);
        }
    }
    public static void hasLengthMax(@Nullable String text, int length, String message) {
        if (text != null && text.trim().length() >= length) {
            throw new IllegalArgumentException(message);
        }
    }
    public static void hasLength(@Nullable String text, int min,int max, String message) {
        if (text == null || text.trim().length() < min|| text.trim().length() >= max) {
            throw new IllegalArgumentException(message);
        }
    }
}
