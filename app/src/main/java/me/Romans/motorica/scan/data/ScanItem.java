package me.Romans.motorica.scan.data;

public class ScanItem {
    private int image;
    private String title;
    private boolean checkProgress;

    public ScanItem(int image, String title, boolean checkProgress) {
        this.image = image;
        this.title = title;
        this.checkProgress = checkProgress;
    }

    public int getImage() {
        return image;
    }

    public String getTitle() {
        return title;
    }

    public boolean getCheckProgress() { return checkProgress; }
}
