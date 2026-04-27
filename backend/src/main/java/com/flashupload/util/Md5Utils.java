package com.flashupload.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 第二阶段先抽出 MD5 工具类，后续秒传和分片校验都可以复用。
 */
public final class Md5Utils {

    private static final int BUFFER_SIZE = 8 * 1024;

    private Md5Utils() {
    }

    public static String md5Hex(InputStream inputStream) throws IOException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[BUFFER_SIZE];
            int readLength;

            while ((readLength = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, readLength);
            }

            return toHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 algorithm is not available", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
