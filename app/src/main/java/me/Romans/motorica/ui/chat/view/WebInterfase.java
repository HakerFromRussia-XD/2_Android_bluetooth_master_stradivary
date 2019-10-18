package me.Romans.motorica.ui.chat.view;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebInterfase extends ChartActivity {
    private Context context;
    private int testInt = 0;

    public WebInterfase(Context context) {
        this.context = context;
    }

//    Vibrator vibrator = (Vibrator) mainActivity.getSystemService(Context.VIBRATOR_SERVICE);

    @JavascriptInterface
    public String myName () {
        System.err.println("WebInterfase ---------> my_integer=");
        return "Хаахахахахаззаахааз";
    }

    @JavascriptInterface
    public void showToast (String massege) {
        testInt = Integer.parseInt(massege);
        Toast.makeText(context, massege, Toast.LENGTH_SHORT).show();
        System.err.println("WebInterfase ---------> testInt="+testInt);
        setTestInt(massege);
    }

    @JavascriptInterface
    public void rotate (int my_int) {
//        if(mainActivity != null) {mainActivity = (MainActivity) getActivity();}
//        mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        System.err.println("MainActivity ---------> my_integer="+my_int);
    }

    @JavascriptInterface
    public void close () {

    }

    @JavascriptInterface
    public void vibrate () {
//        vibrator.vibrate(500);
    }
}
