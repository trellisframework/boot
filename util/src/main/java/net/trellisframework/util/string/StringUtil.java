package net.trellisframework.util.string;

import net.trellisframework.core.log.Logger;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class StringUtil {

    public static String nullIfBlank(String value) {
        return StringUtils.isBlank(value) ? null : value;
    }

    public static String defaultIfBlank(String value) {
        return StringUtils.isBlank(value) ? null : value;
    }

    public static String convertToUTF8(String msg) {
        if (StandardCharsets.ISO_8859_1.newEncoder().canEncode(msg)) {
            return new String(msg.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        }
        return msg;
    }

    public static byte[] decodeHex(String hex) {
        try {
            return Hex.decodeHex(hex);
        } catch (DecoderException e) {
            Logger.error("DecodeException", e.getMessage());
            return null;
        }
    }

    public static String encodeHexString(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }

    public static String encodeHexString(byte[] data, boolean toLowerCase) {
        return Hex.encodeHexString(data, toLowerCase);
    }

    public static String getRandom(int characterLength) {
        return UUID.randomUUID().toString().substring(0, characterLength);
    }

}
