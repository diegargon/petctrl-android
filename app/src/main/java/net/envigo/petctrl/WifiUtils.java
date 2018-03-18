package net.envigo.petctrl;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by diego on 18/02/18.
 *
 */


@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "unused"})
class WifiUtils {

    private Context context;
    private WifiManager wifi;
    private WifiConfiguration wifiConf;

    private StringBuilder sb = new StringBuilder();

    WifiUtils(Context context) {
        this.context = context;
    }


    void wifiInit() {
        if (wifi == null) wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }
    void enableWifi(){
        Log.d("Log:", "Wifi enabled");
        wifi.setWifiEnabled(true);
    }

    void disableWifi() {
        Log.d("Log:", "Wifi disabled");
        if (wifi.isWifiEnabled()) wifi.setWifiEnabled(false);
    }

    boolean enableAP() {
        try {
            Method method = wifi.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifi, wifiConf, true);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    boolean disableAP() {
        try {
            Method method = wifi.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifi, wifiConf, false);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    int getState() {
        return wifi.getWifiState();
    }

    boolean cfgDiscoveryAP() {
        Log.d("Log:", "cfgDiscoveryAP called");
        return cfgAP("PetControl", "esto-es-una-clave-larga");
    }

    boolean cfgAP(String SSID, String SharedKey) {

        wifiInit();
        disableWifi();

        wifiConf = new WifiConfiguration();
        wifiConf.allowedAuthAlgorithms.clear();
        wifiConf.allowedGroupCiphers.clear();
        wifiConf.allowedKeyManagement.clear();
        wifiConf.allowedPairwiseCiphers.clear();
        wifiConf.allowedProtocols.clear();
        wifiConf.SSID = SSID;
        wifiConf.preSharedKey  = SharedKey;
        wifiConf.status = WifiConfiguration.Status.ENABLED;
        wifiConf.hiddenSSID = false;
        wifiConf.allowedKeyManagement.set(4); //WPA2
        wifiConf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wifiConf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        int netId = wifi.addNetwork(wifiConf);

        if (netId != -1)  wifi.enableNetwork(netId, true);


        //wifi.saveConfiguration();

/*
        try {
            Method method = wifi.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifi, wifiConf, true);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
*/
        return enableAP();
    }

    StringBuilder getList() {
        return sb;
    }

    boolean isEnabled() {
        return wifi.isWifiEnabled();
    }


    @SuppressWarnings("SameParameterValue")
    void getClientList(final boolean onlyReachables, final int reachableTimeout, final iScanListener finishListener) {
        Runnable runnable = new Runnable() {
            public void run() {

                BufferedReader br = null;
                final ArrayList<PetClients> result = new ArrayList<>();

                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] splitted = line.split(" +");

                        if (splitted.length >= 4) { // Basic sanity check
                            String mac = splitted[3];

                            if (mac.matches("..:..:..:..:..:..")) {
                                boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(reachableTimeout);

                                if (!onlyReachables || isReachable) {
                                    result.add(new PetClients(splitted[0], splitted[3], splitted[5], isReachable));
                                }
                            }
                        }

                    }
                } catch (Exception e) {
                    Log.e(this.getClass().toString(), e.toString());
                } finally {
                    try {
                        if (br != null) br.close();
                    } catch (Exception e) {
                        Log.e(this.getClass().toString(), e.getMessage());
                    }
                }

                // Get a handler that can be used to post to the main thread
                Handler mainHandler = new Handler(context.getMainLooper());
                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        finishListener.onFinishScan(result);
                    }
                };
                mainHandler.post(myRunnable);
            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();
    }
}
