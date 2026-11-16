package top.knos.mySeek.security.support;


import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 对称加密工具。
 * <p>原实现每次调用 {@link KeyGenerator} 随机生成密钥，导致加密后无法解密，
 * 这里改为：使用口令派生固定密钥（SHA-256），并采用 CBC + 随机 IV 的安全组合。</p>
 */
@Slf4j
public class EncryptUtil {
    public static final String MD5 = "MD5";
    public static final String SHA1 = "SHA1";
    public static final String HmacMD5 = "HmacMD5";
    public static final String HmacSHA1 = "HmacSHA1";
    public static final String DES = "DES";
    public static final String AES = "AES";

    /** AES/CBC/PKCS5Padding */
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    /**编码格式；默认使用uft-8*/
    public String charset = "utf-8";
    /**DES*/
    public int keysizeDES = 0;
    /**AES*/
    public int keysizeAES = 128;

    public static EncryptUtil me;

    private EncryptUtil(){
        //单例
    }
    //双重锁
    public static EncryptUtil getInstance(){
        if (me==null) {
           synchronized (EncryptUtil.class) {
               if(me == null){
                   me = new EncryptUtil();
               }
           }
        }
        return me;
    }


    /**
     * 使用KeyGenerator双向加密，DES/AES，注意这里转化为字符串的时候是将2进制转为16进制格式的字符串，不是直接转，因为会出错
     * @param res 加密的原文
     * @param algorithm 加密使用的算法名称
     * @param key  加密的秘钥
     * @param keysize
     * @param isEncode
     * @return
     */
    private String keyGeneratorES(String res,String algorithm,String key,int keysize,boolean isEncode){
        try {
            KeyGenerator kg = KeyGenerator.getInstance(algorithm);
            if (keysize == 0) {
                byte[] keyBytes = charset==null?key.getBytes():key.getBytes(charset);
                kg.init(new SecureRandom(keyBytes));
            }else if (key==null) {
                kg.init(keysize);
            }else {
                byte[] keyBytes = charset==null?key.getBytes():key.getBytes(charset);
                kg.init(keysize, new SecureRandom(keyBytes));
            }
            SecretKey sk = kg.generateKey();
            SecretKeySpec sks = new SecretKeySpec(sk.getEncoded(), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            if (isEncode) {
                cipher.init(Cipher.ENCRYPT_MODE, sks);
                byte[] resBytes = charset==null?res.getBytes():res.getBytes(charset);
                return parseByte2HexStr(cipher.doFinal(resBytes));
            }else {
                cipher.init(Cipher.DECRYPT_MODE, sks);
                return new String(cipher.doFinal(parseHexStr2Byte(res)));
            }
        } catch (Exception e) {
            log.error("对称加解密失败", e);
        }
        return null;
    }


    /**将二进制转换成16进制 */
    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }
    /**将16进制转换为二进制*/
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length()/2];
        for (int i = 0;i< hexStr.length()/2; i++) {
            int high = Integer.parseInt(hexStr.substring(i*2, i*2+1), 16);
            int low = Integer.parseInt(hexStr.substring(i*2+1, i*2+2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    /**
     * 使用DES加密算法进行加密（可逆）
     * @param res 需要加密的原文
     * @param key 秘钥
     * @return
     */
    public String DESencode(String res, String key) {
        return keyGeneratorES(res, DES, key, keysizeDES, true);
    }

    /**
     * 对使用DES加密算法的密文进行解密（可逆）
     * @param res 需要解密的密文
     * @param key 秘钥
     * @return
     */
    public String DESdecode(String res, String key) {
        return keyGeneratorES(res, DES, key, keysizeDES, false);
    }

    /**
     * 使用 AES/CBC/PKCS5Padding 加密（可逆）。
     * <p>密钥由口令经 SHA-256 派生，IV 随机生成并前置在密文中。</p>
     *
     * @param res 需要加密的原文
     * @param key 秘钥
     * @return 16 进制密文
     */
    public String AESencode(String res, String key) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(key), new IvParameterSpec(iv));
            byte[] resBytes = charset == null ? res.getBytes() : res.getBytes(charset);
            byte[] encrypted = cipher.doFinal(resBytes);
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return parseByte2HexStr(result);
        } catch (Exception e) {
            log.error("AES 加密失败", e);
            throw new IllegalStateException("AES 加密失败", e);
        }
    }

    /**
     * 对使用 AES 加密的密文进行解密。
     *
     * @param res 需要解密的密文
     * @param key 秘钥
     * @return 明文
     */
    public String AESdecode(String res, String key) {
        try {
            byte[] raw = parseHexStr2Byte(res);
            if (raw == null || raw.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文格式错误");
            }
            byte[] iv = Arrays.copyOfRange(raw, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(raw, IV_LENGTH, raw.length);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(key), new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            Charset cs = charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset);
            return new String(decrypted, cs);
        } catch (Exception e) {
            log.error("AES 解密失败", e);
            throw new IllegalStateException("AES 解密失败", e);
        }
    }

    /**
     * 由口令派生固定长度的 AES 密钥。
     */
    private SecretKeySpec deriveKey(String key) throws Exception {
        byte[] keyBytes = charset == null ? key.getBytes() : key.getBytes(charset);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(keyBytes);
        return new SecretKeySpec(digest, 0, keysizeAES / 8, AES);
    }

    /**
     * 使用异或进行加密
     * @param res 需要加密的密文
     * @param key 秘钥
     * @return
     */
    public String XORencode(String res, String key) {
        byte[] bs = res.getBytes();
        for (int i = 0; i < bs.length; i++) {
            bs[i] = (byte) ((bs[i]) ^ key.hashCode());
        }
        return parseByte2HexStr(bs);
    }

    /**
     * 使用异或进行解密
     * @param res 需要解密的密文
     * @param key 秘钥
     * @return
     */
    public String XORdecode(String res, String key) {
        byte[] bs = parseHexStr2Byte(res);
        for (int i = 0; i < bs.length; i++) {
            bs[i] = (byte) ((bs[i]) ^ key.hashCode());
        }
        return new String(bs);
    }

    /**
     * 直接使用异或（第一调用加密，第二次调用解密）
     * @param res 密文
     * @param key 秘钥
     * @return
     */
    public int XOR(int res, String key) {
        return res ^ key.hashCode();
    }


}
