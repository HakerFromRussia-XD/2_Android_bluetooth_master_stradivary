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
    public void showToast (String massage) {
        testInt = Integer.parseInt(massage);
        Toast.makeText(context, massage, Toast.LENGTH_SHORT).show();
        System.err.println("WebInterface ---------> testInt="+testInt);
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
