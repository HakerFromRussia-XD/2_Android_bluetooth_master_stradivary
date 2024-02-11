package com.bailout.stickk.scan.data;

public class ScanItem {

    private String typeProtocol;
    private String title;
    private String address;
    private int position;
    private int rssi;

    public ScanItem(String typeProtocol, String title, String address, int position, int rssi) {
        this.typeProtocol = typeProtocol;
        this.title = title;
        this.address = address;
        this.position = position;
        this.rssi = rssi;
    }

    public String getTitle() {return title;}

    public String getAddress() {return address;}

    public int getPosition() { return position; }

    public int getRssi() { return rssi; }
}
