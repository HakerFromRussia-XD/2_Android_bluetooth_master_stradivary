package me.start.motorica.scan.data;

public class ScanItem {

    private String title;
    private String address;
    private int position;
    private boolean checkProgress;

    public ScanItem(String title, String address, int position, boolean checkProgress) {
        this.title = title;
        this.address = address;
        this.position = position;
        this.checkProgress = checkProgress;
    }

    public String getTitle() {
        return title;
    }

    public String getAddress() {
        return address;
    }

    public int getPosition() { return position; }

    public boolean getCheckProgress() { return checkProgress; }
}
