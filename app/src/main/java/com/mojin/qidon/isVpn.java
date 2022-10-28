package com.mojin.qidon;

import android.content.Context;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class isVpn {
    public static boolean isVPNConnected(Context context) {
        ArrayList arrayList = new ArrayList();
        try {
            Iterator it = Collections.list(NetworkInterface.getNetworkInterfaces()).iterator();
            while (it.hasNext()) {
                NetworkInterface networkInterface = (NetworkInterface) it.next();
                if (networkInterface.isUp()) {
                    arrayList.add(networkInterface.getName());
                }
            }
        } catch (Exception e) {
        }
        return arrayList.contains("tun0") || arrayList.contains("ppp0");
    }
}
