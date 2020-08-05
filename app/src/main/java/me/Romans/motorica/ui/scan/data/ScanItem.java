package me.Romans.motorica.ui.scan.data;

import android.view.View;
import android.widget.ProgressBar;

import butterknife.BindView;
import me.Romans.motorica.R;

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
