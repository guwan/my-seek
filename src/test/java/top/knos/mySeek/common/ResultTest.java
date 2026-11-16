package top.knos.mySeek.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    @DisplayName("ok wraps the payload")
    void okWrapsPayload() {
        Result<String> result = Result.ok("payload");

        assertTrue(result.isSuccess());
        assertEquals("payload", result.getData());
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("fail is not successful and uses 400 by default")
    void failSemantics() {
        Result<Object> result = Result.fail();

        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("unauthorized is not successful and uses 401")
    void unauthorizedSemantics() {
        Result<Object> result = Result.unauthorized();

        assertFalse(result.isSuccess());
        assertEquals(401, result.getCode());
    }
}
