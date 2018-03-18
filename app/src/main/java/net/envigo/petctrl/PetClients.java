package net.envigo.petctrl;

/**
 * Created by diego on 19/02/18.
 *
 */

@SuppressWarnings("unused")
class PetClients {

    int lightState = 0;
    int soundState = 0;
    int vibrationState = 0;

    private String Name = null;
    private String ChipID = null;
    private String PhoneNumber = null;
    private int RSSI = 1;
    private String IpAddr;
    private String HWAddr;
    private String Device;
    private boolean isReachable = false;

    PetClients(String ipAddr, String hWAddr, String device, boolean isReachable) {
        super();
        this.IpAddr = ipAddr;
        this.HWAddr = hWAddr;
        this.Device = device;
        this.isReachable = isReachable;
    }


    String getIpAddr() {
        return IpAddr;
    }

    void setIpAddr(String ipAddr) {
        IpAddr = ipAddr;
    }

    String getHWAddr() {
        return HWAddr;
    }

    void setHWAddr(String hWAddr) {
        HWAddr = hWAddr;
    }

    String getDevice() {
        return Device;
    }

    void setDevice(String device) {
        Device = device;
    }

    boolean isReachable() {
        return isReachable;
    }

    void setReachable(boolean isReachable) {
        this.isReachable = isReachable;
    }

    void setName(String name) { this.Name = name;}
    String getName() { return this.Name;}
    String getChip() { return this.ChipID;}
    void setChip(String chip) { this.ChipID = chip;}
    String getPhoneNumber() { return this.PhoneNumber;}
    void setPhoneNumber(String phone) { this.PhoneNumber = phone;}
    int getRSSI() { return this.RSSI;}
    void setRSSI(int rssi) { this.RSSI = rssi;}

}
