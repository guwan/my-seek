package top.knos.mySeek.security.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptUtilTest {

    private final EncryptUtil encryptUtil = EncryptUtil.getInstance();

    @Test
    @DisplayName("AES encrypt then decrypt returns the original text")
    void aesRoundTrip() {
        String plain = "user@example.com_register_token_1712345678901";
        String key = "a-very-secret-key-for-testing";

        String cipher = encryptUtil.AESencode(plain, key);
        assertNotEquals(plain, cipher);
        assertEquals(plain, encryptUtil.AESdecode(cipher, key));
    }

    @Test
    @DisplayName("Same plaintext produces different ciphertext (random IV)")
    void aesUsesRandomIv() {
        String plain = "hello world";
        String key = "key";

        String first = encryptUtil.AESencode(plain, key);
        String second = encryptUtil.AESencode(plain, key);

        assertNotEquals(first, second);
        assertEquals(plain, encryptUtil.AESdecode(first, key));
        assertEquals(plain, encryptUtil.AESdecode(second, key));
    }

    @Test
    @DisplayName("Wrong key cannot decrypt")
    void aesWrongKeyFails() {
        String cipher = encryptUtil.AESencode("secret", "key-a");

        assertThrows(Exception.class, () -> encryptUtil.AESdecode(cipher, "key-b"));
    }

    @Test
    @DisplayName("Hex and binary conversion is reversible")
    void hexConversionRoundTrip() {
        byte[] data = {0x00, 0x0F, (byte) 0xFF, 0x10};

        String hex = EncryptUtil.parseByte2HexStr(data);

        assertEquals("000FFF10", hex);
        assertTrue(Arrays.equals(data, EncryptUtil.parseHexStr2Byte(hex)));
    }
}
