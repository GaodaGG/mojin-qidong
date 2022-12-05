package com.mojin.qidon.Keystore;

import android.app.Activity;
import android.app.Application;

public class SignatureVerifier {
    public static String V2_SHA256 = null;
    public static String V3_SHA256 = null;
    public static boolean isMySignature = false;

    public static void verify() {
        verify("com.mojin.qidon", 0x12, 0x78, 0x0a, 0x6d, 0xea, 0x80, 0xca, 0x23, 0xd5, 0xfd, 0x69, 0x60, 0x7c, 0x53, 0x09, 0x42, 0x91, 0x4e, 0xeb, 0xd8, 0x62, 0xa1, 0xc3, 0xff, 0xbb, 0xe3, 0xcf, 0x3f, 0xa2, 0xc2, 0xf4, 0xfe);
        }

    public static void verify(String packageName, int... mySignatureSHA256) {
        byte[][] signature = FuckYou.fuckYou(packageName);
        if (signature != null) {
            byte[] v2 = signature[0];
            byte[] v3 = signature[1];
            if (v2.length != 0) {
                byte[] v2sha256 = FuckYou.fuckMe(v2);
                V2_SHA256 = FuckYou.fuckAll(v2sha256);
                isMySignature = FuckYou.isFucking(v2sha256, mySignatureSHA256);
            }
            if (v3.length != 0) {
                byte[] v3sha256 = FuckYou.fuckMe(v3);
                V3_SHA256 = FuckYou.fuckAll(v3sha256);
                isMySignature = isMySignature && FuckYou.isFucking(v3sha256, mySignatureSHA256);
            }
        }
    }
    
    /**
     * 校验 application
     */
     public static boolean checkApplication(Activity context){
        Application nowApplication = context.getApplication();
        String trueApplicationName = "MainApplication";
        String nowApplicationName = nowApplication.getClass().getSimpleName();
        return trueApplicationName.equals(nowApplicationName);
    }
}
