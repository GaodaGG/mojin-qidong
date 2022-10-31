package com.mojin.qidon.Keystore;

public class SignatureVerifier {
    public static String V2_SHA256 = null;
    public static String V3_SHA256 = null;
    public static boolean isMySignature = false;

    public static void verify() {
        verify("com.mojin.qidon", 0x6e, 0xcb, 0xda, 0x2c, 0x26, 0x3f, 0x3e, 0xa7, 0x5c, 0x7a, 0x1f, 0x33, 0x7d, 0x2f, 0x85, 0x11, 0x1a, 0x32, 0xe7, 0xa2, 0xbe, 0x6d, 0x0b, 0xd1, 0xdb, 0x0c, 0xb2, 0xcf, 0xc4, 0xc8, 0x28, 0x37);
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
}
