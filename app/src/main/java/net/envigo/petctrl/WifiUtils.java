package net.envigo.petctrl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by diego on 18/02/18.
 */

public class WifiUtils {

    private Context context;
    private WifiManager wifi;
    private WifiConfiguration wifiConf;

    StringBuilder sb = new StringBuilder();

    public WifiUtils(Context context) {
        this.context = context;
    }

    public void wifiInit() {
        if (wifi == null) {
            wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
    }
    public void enableWifi(){
        Log.d("Log:", "Wifi enabled");
        wifi.setWifiEnabled(true);
    }

    public void disableWifi() {
        Log.d("Log:", "Wifi disabled");
        if (wifi.isWifiEnabled()) {
            wifi.setWifiEnabled(false);
        }

    }

    public boolean enableAP() {
        try {
            Method method = wifi.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifi, wifiConf, true);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean disableAP() {
        try {
            Method method = wifi.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifi, wifiConf, false);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getState() {
        return wifi.getWifiState();
    }

    public boolean cfgDiscoveryAP() {
        Log.d("Log:", "cfgDiscoveryAP called");
        return cfgAP("PetControl", "esto-es-una-clave-larga");
    }




    public boolean cfgAP(String SSID, String SharedKey) {

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

        if (netId != -1) {
            wifi.enableNetwork(netId, true);
        }


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

    public StringBuilder getList() {
        return sb;
    }

    public boolean isEnabled() {
        return wifi.isWifiEnabled();
    }


    /*
    public void getClientList() {
        int macCount = 0;
        BufferedReader br = null;
        boolean onlyReachables = true;

        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");

                if (splitted != null ) {
                    Log.d("Log:","ENTRO");
                    String mac = splitted[3];

                    if (mac.matches("..:..:..:..:..:..")) {
                        Log.d("Log:","Matches");
                        System.out.println("Mac : "+ mac + " IP Address : "+splitted[0] + " - " + splitted[2] );
                        boolean isReachable = InetAddress.getByName(splitted[0]).isReachable(300);
                        Log.d("Log:","punto 1");
                        Log.d("Log:", splitted[0]);
                       // if (!onlyReachables || isReachable) {
                            //result.add(new ClientScanResult(splitted[0], splitted[3], splitted[5], isReachable));
                            Log.d("Log:", splitted[0] + splitted[3] + splitted[5]);
                        //}
                        Log.d("Log:","punto 2");
                    } else {
                        Log.d("Log:","Not matches");
                    }
                    Log.d("Log:","punto 3");

                    String mac = splitted[3];
                    System.out.println("Mac : Outside If "+ mac );
                    if (mac.matches("..:..:..:..:..:..")) {
                        macCount++;
                        System.out.println("Mac : "+ mac + " IP Address : "+splitted[0] );
                        System.out.println("Mac_Count  " + macCount + " MAC_ADDRESS  "+ mac);
                        Toast.makeText(
                                context,
                                "Mac_Count  " + macCount + "   MAC_ADDRESS  "
                                        + mac, Toast.LENGTH_SHORT).show();

                    }

                }
            }
        } catch(Exception e) {

        }
    }
    */
    public void getClientList(final boolean onlyReachables, final int reachableTimeout, final iScanListener finishListener) {
        Runnable runnable = new Runnable() {
            public void run() {

                BufferedReader br = null;
                final ArrayList<PetClients> result = new ArrayList<>();

                try {
                    br = new BufferedReader(new FileReader("/proc/net/arp"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] splitted = line.split(" +");

                        if ((splitted != null) && (splitted.length >= 4)) {
                            // Basic sanity check
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
                        br.close();
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
