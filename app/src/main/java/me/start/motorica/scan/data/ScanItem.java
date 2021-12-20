package me.start.motorica.scan.data;

public class ScanItem {
    private int image;
    private String title;
    private String address;
    private boolean checkProgress;

    public ScanItem(int image, String title, String address,boolean checkProgress) {
        this.image = image;
        this.title = title;
        this.address = address;
        this.checkProgress = checkProgress;
    }

    public int getImage() {
        return image;
    }

    public String getTitle() {
        return title;
    }

    public String getAddress() {
        return address;
    }

    public boolean getCheckProgress() { return checkProgress; }
}
