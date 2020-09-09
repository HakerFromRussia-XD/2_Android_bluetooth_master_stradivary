package me.Romans.motorica.ui.chat.view.model;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

import me.Romans.motorica.ui.chat.view.ChartActivity;

public class Load3DModel  {
    private  static Context context;
    private static String[][] model = new String[19][];
    private static String[] text;
    private ChartActivity chatActivity;

    public Load3DModel(Context context) {
        this.context = context;
    }

    //////////////////////////////////////////////////////////////////////////////
    /**                             работа с 3D                                **/
    //////////////////////////////////////////////////////////////////////////////
    public String[] readData(String fileName) {

        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String line = new String(buffer);
            text = line.split("#");
        } catch (IOException e){
            e.printStackTrace();
        }
        return text;
    }
}
