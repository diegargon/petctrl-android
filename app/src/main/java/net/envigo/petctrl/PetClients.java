package net.envigo.petctrl;

/**
 * Created by diego on 19/02/18.
 */

public class PetClients {
    private String Name = null;
    private String ChipID = null;
    private String PhoneNumber = null;
    private int RSSI = 1;
    private String IpAddr;
    private String HWAddr;
    private String Device;


    private boolean isReachable;

    public PetClients(String ipAddr, String hWAddr, String device, boolean isReachable) {
        super();
        this.IpAddr = ipAddr;
        this.HWAddr = hWAddr;
        this.Device = device;
        this.isReachable = isReachable;
    }


    public String getIpAddr() {
        return IpAddr;
    }

    public void setIpAddr(String ipAddr) {
        IpAddr = ipAddr;
    }

    public String getHWAddr() {
        return HWAddr;
    }

    public void setHWAddr(String hWAddr) {
        HWAddr = hWAddr;
    }

    public String getDevice() {
        return Device;
    }

    public void setDevice(String device) {
        Device = device;
    }

    public boolean isReachable() {
        return isReachable;
    }

    public void setReachable(boolean isReachable) {
        this.isReachable = isReachable;
    }

    public void setName(String name) { this.Name = name;}
    public String getName() { return this.Name;}
    public String getChip() { return this.ChipID;}
    public void setChip(String chip) { this.ChipID = chip;}
    public String getPhoneNumber() { return this.PhoneNumber;}
    public void setPhoneNumber(String phone) { this.PhoneNumber = phone;}
    public int getRSSI() { return this.RSSI;}
    public void setRSSI(int rssi) { this.RSSI = rssi;}

}
