package com.bailout.stick.scan.data;

public class ScanItem {

    private String title;
    private String address;
    private int position;
    private int rssi;

    public ScanItem(String title, String address, int position, int rssi) {
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
