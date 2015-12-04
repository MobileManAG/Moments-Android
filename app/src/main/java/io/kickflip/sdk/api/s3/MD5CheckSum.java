package io.kickflip.sdk.api.s3;

import android.util.Base64;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Created by admin on 26/04/15.
 */
public class MD5CheckSum {

    public static byte[] createChecksum(String pFilepath) throws Exception {
        InputStream lFis = new FileInputStream(pFilepath);

        byte[] lBuffer = new byte[1024];
        MessageDigest lMessageDigest = MessageDigest.getInstance("MD5");

        int lNumRead;

        do {
            lNumRead = lFis.read(lBuffer);
            if (lNumRead > 0) {
                lMessageDigest.update(lBuffer, 0, lNumRead);
            }
        } while (lNumRead != -1);

        lFis.close();
        return lMessageDigest.digest();

    }

    public static String getMD5Checksum(String pFilepath) throws Exception {
        byte[] lBytes = createChecksum(pFilepath);
        return Base64.encodeToString(lBytes, Base64.NO_WRAP);
    }
}
