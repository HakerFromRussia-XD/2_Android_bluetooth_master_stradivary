package me.Romans.motorica.ui.chat.view.model;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import me.Romans.motorica.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.ui.chat.view.ChartActivity;
import me.Romans.motorica.ui.chat.view.massage_to_send.Massages;

import static me.Romans.motorica.ui.chat.view.ChartActivity.NUMBER_CELL;
import static me.Romans.motorica.ui.chat.view.ChartActivity.delay;
import static me.Romans.motorica.ui.chat.view.ChartActivity.deviceName;
import static me.Romans.motorica.ui.chat.view.ChartActivity.flagUseHDLCProtocol;
import static me.Romans.motorica.ui.chat.view.ChartActivity.selectStation;

public class Load3DModel extends ChartActivity {
    private  static Context context;
    private static String[] text;
    private static volatile float[][] coordinatesArray = new float[MAX_NUMBER_DETAILS][];
    private static volatile float[][] texturesArray = new float[MAX_NUMBER_DETAILS][];
    private static volatile float[][] normalsArray = new float[MAX_NUMBER_DETAILS][];
    private ChartActivity chatActivity;
    public static int MAX_NUMBER_DETAILS = 19;
    public volatile static float[][] verticesArray = new float[MAX_NUMBER_DETAILS][1];
    public static String[][] model = new String[19][];
    public volatile static int[][] indicesArrayVertices = new int[MAX_NUMBER_DETAILS][1];
    Massages mMassages = new Massages();
//    @Inject
//    public ChatPresenter presenter;


//    public Load3DModel(Context context) {
//        this.context = context;
//    }

    //////////////////////////////////////////////////////////////////////////////
    /**                           Загрузка модели                              **/
    //////////////////////////////////////////////////////////////////////////////
    public static String[] readData(String fileName) {
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
    public static void loadSTR2(final int i) {
        parserDataVertices(i);
        parserDataTextures(i);
        parserDataNormals(i);
        parserDataFacets(i);
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                   Компановка необходимых массивов                      **/
    //////////////////////////////////////////////////////////////////////////////
    public static void parserDataVertices(int number){
        String text = "";
        if      (number ==  0) {text = "#" + getStringBuffer1() [1];}
        else if (number ==  1) {text = "#" + getStringBuffer2() [1];}
        else if (number ==  2) {text = "#" + getStringBuffer3() [1];}
        else if (number ==  3) {text = "#" + getStringBuffer4() [1];}
        else if (number ==  4) {text = "#" + getStringBuffer5() [1];}
        else if (number ==  5) {text = "#" + getStringBuffer6() [1];}
        else if (number ==  6) {text = "#" + getStringBuffer7() [1];}
        else if (number ==  7) {text = "#" + getStringBuffer8() [1];}
        else if (number ==  8) {text = "#" + getStringBuffer9() [1];}
        else if (number ==  9) {text = "#" + getStringBuffer10()[1];}
        else if (number == 10) {text = "#" + getStringBuffer11()[1];}
        else if (number == 11) {text = "#" + getStringBuffer12()[1];}
        else if (number == 12) {text = "#" + getStringBuffer13()[1];}
        else if (number == 13) {text = "#" + getStringBuffer14()[1];}
        else if (number == 14) {text = "#" + getStringBuffer15()[1];}
        else if (number == 15) {text = "#" + getStringBuffer16()[1];}
        else if (number == 16) {text = "#" + getStringBuffer17()[1];}
        else if (number == 17) {text = "#" + getStringBuffer18()[1];}
        else if (number == 18) {text = "#" + getStringBuffer19()[1];}
        StringBuilder line = new StringBuilder();
        int coordinatesNumber = 0;
        for (char msg : text.toCharArray()) {
            line.append(msg);
            if (msg == 10) {
                String[] currentLine = line.toString().split(" ");
                if (line.toString().startsWith("# ")) {
                    if (currentLine[2].equals("vertices\r\n")) {//\r
                        coordinatesNumber = Integer.parseInt(currentLine[1]);
                        coordinatesArray[number] = new float[coordinatesNumber * 3];
//                        System.out.println("Количество вершин: " + coordinatesNumber);
                        coordinatesNumber = 0;
                    }
                } else if (line.toString().startsWith("v ")){
                    coordinatesArray[number][coordinatesNumber++] = Float.parseFloat(currentLine[1]);
                    coordinatesArray[number][coordinatesNumber++] = Float.parseFloat(currentLine[2]);
                    coordinatesArray[number][coordinatesNumber++] = Float.parseFloat(currentLine[3]);
                }
                line = new StringBuilder();
            }
        }
    }
    public static void parserDataTextures(int number){
        String text = "";
        if      (number ==  0) {text = "#" + getStringBuffer1() [2];}
        else if (number ==  1) {text = "#" + getStringBuffer2() [2];}
        else if (number ==  2) {text = "#" + getStringBuffer3() [2];}
        else if (number ==  3) {text = "#" + getStringBuffer4() [2];}
        else if (number ==  4) {text = "#" + getStringBuffer5() [2];}
        else if (number ==  5) {text = "#" + getStringBuffer6() [2];}
        else if (number ==  6) {text = "#" + getStringBuffer7() [2];}
        else if (number ==  7) {text = "#" + getStringBuffer8() [2];}
        else if (number ==  8) {text = "#" + getStringBuffer9() [2];}
        else if (number ==  9) {text = "#" + getStringBuffer10()[2];}
        else if (number == 10) {text = "#" + getStringBuffer11()[2];}
        else if (number == 11) {text = "#" + getStringBuffer12()[2];}
        else if (number == 12) {text = "#" + getStringBuffer13()[2];}
        else if (number == 13) {text = "#" + getStringBuffer14()[2];}
        else if (number == 14) {text = "#" + getStringBuffer15()[2];}
        else if (number == 15) {text = "#" + getStringBuffer16()[2];}
        else if (number == 16) {text = "#" + getStringBuffer17()[2];}
        else if (number == 17) {text = "#" + getStringBuffer18()[2];}
        else if (number == 18) {text = "#" + getStringBuffer19()[2];}
        StringBuilder line = new StringBuilder();

        int texturesNumber = 0;
        for (char msg : text.toCharArray()){
            line.append(msg);
            if (msg == 10){
                String[] currentLine = line.toString().split(" ");
                if(line.toString().startsWith("# ")){
                    if(currentLine[2].equals("texture")){
                        texturesNumber = Integer.parseInt(currentLine[1]);
                        texturesArray[number] = new float[texturesNumber*2];
//                        System.out.println("Количество текстурных координат: " + texturesNumber);
                        texturesNumber = 0;
                    }
                }else if (line.toString().startsWith("vt ")){
                    texturesArray[number][texturesNumber] = Float.parseFloat(currentLine[1]);
                    texturesArray[number][texturesNumber + 1] = Float.parseFloat(currentLine[2]);
                    texturesNumber += 2;
                }
                line = new StringBuilder();
            }
        }
    }
    public static void parserDataNormals(int number){
        String text = "";
        if      (number ==  0) {text = "#" + getStringBuffer1() [3];}
        else if (number ==  1) {text = "#" + getStringBuffer2() [3];}
        else if (number ==  2) {text = "#" + getStringBuffer3() [3];}
        else if (number ==  3) {text = "#" + getStringBuffer4() [3];}
        else if (number ==  4) {text = "#" + getStringBuffer5() [3];}
        else if (number ==  5) {text = "#" + getStringBuffer6() [3];}
        else if (number ==  6) {text = "#" + getStringBuffer7() [3];}
        else if (number ==  7) {text = "#" + getStringBuffer8() [3];}
        else if (number ==  8) {text = "#" + getStringBuffer9() [3];}
        else if (number ==  9) {text = "#" + getStringBuffer10()[3];}
        else if (number == 10) {text = "#" + getStringBuffer11()[3];}
        else if (number == 11) {text = "#" + getStringBuffer12()[3];}
        else if (number == 12) {text = "#" + getStringBuffer13()[3];}
        else if (number == 13) {text = "#" + getStringBuffer14()[3];}
        else if (number == 14) {text = "#" + getStringBuffer15()[3];}
        else if (number == 15) {text = "#" + getStringBuffer16()[3];}
        else if (number == 16) {text = "#" + getStringBuffer17()[3];}
        else if (number == 17) {text = "#" + getStringBuffer18()[3];}
        else if (number == 18) {text = "#" + getStringBuffer19()[3];}
        StringBuilder line = new StringBuilder();

        int normalsNumber = 0;

        for (char msg : text.toCharArray()){
            line.append(msg);
            if (msg == 10) {
                String[] currentLine = line.toString().split(" ");
                if (line.toString().startsWith("# ")) {
                    if (currentLine[2].equals("vertex")) {
                        normalsNumber = Integer.parseInt(currentLine[1]);
                        normalsArray[number] = new float[normalsNumber * 3];
//                        System.out.println("Количество координат нормалей: " + normalsNumber);
                        normalsNumber = 0;
                    }
                } else if (line.toString().startsWith("vn ")) {
                    normalsArray[number][normalsNumber] = Float.parseFloat(currentLine[1]);
                    normalsArray[number][normalsNumber + 1] = Float.parseFloat(currentLine[2]);
                    normalsArray[number][normalsNumber + 2] = Float.parseFloat(currentLine[3]);
                    normalsNumber += 3;
                }
                line = new StringBuilder();
            }
        }
    }
    public static void parserDataFacets (int number){
        String text = "";
        if      (number ==  0) {text = "#" + getStringBuffer1() [4];}
        else if (number ==  1) {text = "#" + getStringBuffer2() [4];}
        else if (number ==  2) {text = "#" + getStringBuffer3() [4];}
        else if (number ==  3) {text = "#" + getStringBuffer4() [4];}
        else if (number ==  4) {text = "#" + getStringBuffer5() [4];}
        else if (number ==  5) {text = "#" + getStringBuffer6() [4];}
        else if (number ==  6) {text = "#" + getStringBuffer7() [4];}
        else if (number ==  7) {text = "#" + getStringBuffer8() [4];}
        else if (number ==  8) {text = "#" + getStringBuffer9() [4];}
        else if (number ==  9) {text = "#" + getStringBuffer10()[4];}
        else if (number == 10) {text = "#" + getStringBuffer11()[4];}
        else if (number == 11) {text = "#" + getStringBuffer12()[4];}
        else if (number == 12) {text = "#" + getStringBuffer13()[4];}
        else if (number == 13) {text = "#" + getStringBuffer14()[4];}
        else if (number == 14) {text = "#" + getStringBuffer15()[4];}
        else if (number == 15) {text = "#" + getStringBuffer16()[4];}
        else if (number == 16) {text = "#" + getStringBuffer17()[4];}
        else if (number == 17) {text = "#" + getStringBuffer18()[4];}
        else if (number == 18) {text = "#" + getStringBuffer19()[4];}
        StringBuilder line = new StringBuilder();

        int indicesVertices = 0;
        int indicesCoordinateV;
        int indicesNormalsV;
        int indicesTextureV;

        for (char msg : text.toCharArray()){
            line.append(msg);
            if (msg == 10){
                String[] currentLine = line.toString().split(" ");
                if(line.toString().startsWith("# ")){
                    if(currentLine[2].equals("facets\r\n")){//\r
                        indicesVertices = Integer.parseInt(currentLine[1]);
                        verticesArray[number] = new float[indicesVertices*12*3];
                        indicesArrayVertices[number] = new int [indicesVertices*3];
//                        System.out.println("Количество треугольников: " + indicesVertices);
                        indicesVertices = 0;
                    }
                } else if (line.toString().startsWith("f ")){
                    //первая тройка
                    //координаты вершины
                    indicesCoordinateV = (Integer.parseInt(currentLine[1].split("/")[0]) - 1);
                    verticesArray[number][indicesVertices * 12] = coordinatesArray[number][indicesCoordinateV * 3];
                    verticesArray[number][indicesVertices * 12 + 1] = coordinatesArray[number][indicesCoordinateV * 3 + 1];
                    verticesArray[number][indicesVertices * 12 + 2] = coordinatesArray[number][indicesCoordinateV * 3 + 2];
                    //нормали
                    indicesNormalsV = (Integer.parseInt(currentLine[1].split("/")[2]) - 1);
                    verticesArray[number][indicesVertices * 12 + 3] = normalsArray[number][indicesNormalsV * 3];
                    verticesArray[number][indicesVertices * 12 + 4] = normalsArray[number][indicesNormalsV * 3 + 1];
                    verticesArray[number][indicesVertices * 12 + 5] = normalsArray[number][indicesNormalsV * 3 + 2];
                    //цвета
                    verticesArray[number][indicesVertices * 12 + 6] = 1.0f;
                    verticesArray[number][indicesVertices * 12 + 7] = 1.0f;
                    verticesArray[number][indicesVertices * 12 + 8] = 0.0f;
                    verticesArray[number][indicesVertices * 12 + 9] = 0.0f;
                    //текстурные координаты
                    indicesTextureV = (Integer.parseInt(currentLine[1].split("/")[1]) - 1);
                    verticesArray[number][indicesVertices * 12 + 10] = texturesArray[number][indicesTextureV * 2];
                    verticesArray[number][indicesVertices * 12 + 11] = texturesArray[number][indicesTextureV * 2 + 1];

                    indicesArrayVertices[number][indicesVertices] = indicesVertices++;

                    //вторая тройка
                    //координаты вершины
                    indicesCoordinateV = (Integer.parseInt(currentLine[2].split("/")[0]) - 1);
                    verticesArray[number][indicesVertices * 12] = coordinatesArray[number][indicesCoordinateV * 3];
                    verticesArray[number][indicesVertices * 12 + 1] = coordinatesArray[number][indicesCoordinateV * 3 + 1];
                    verticesArray[number][indicesVertices * 12 + 2] = coordinatesArray[number][indicesCoordinateV * 3 + 2];
                    //нормали
                    verticesArray[number][indicesVertices * 12 + 3] = normalsArray[number][(Integer.parseInt(currentLine[2].split("/")[2]) - 1) * 3];
                    verticesArray[number][indicesVertices * 12 + 4] = normalsArray[number][(Integer.parseInt(currentLine[2].split("/")[2]) - 1) * 3 + 1];
                    verticesArray[number][indicesVertices * 12 + 5] = normalsArray[number][(Integer.parseInt(currentLine[2].split("/")[2]) - 1) * 3 + 2];
                    //цвета
                    verticesArray[number][indicesVertices * 12 + 6] = 1.0f;
                    verticesArray[number][indicesVertices * 12 + 7] = 1.0f;
                    verticesArray[number][indicesVertices * 12 + 8] = 0.0f;
                    verticesArray[number][indicesVertices * 12 + 9] = 0.0f;
                    //текстурные координаты
                    indicesTextureV = (Integer.parseInt(currentLine[2].split("/")[1]) - 1);
                    verticesArray[number][indicesVertices * 12 + 10] = texturesArray[number][indicesTextureV * 2];
                    verticesArray[number][indicesVertices * 12 + 11] = texturesArray[number][indicesTextureV * 2 + 1];

                    indicesArrayVertices[number][indicesVertices] = indicesVertices++;

                    //третья тройка
                    //координаты вершины
                    indicesCoordinateV = (Integer.parseInt(currentLine[3].split("/")[0]) - 1);
                    verticesArray[number][indicesVertices * 12] = coordinatesArray[number][indicesCoordinateV * 3];
                    verticesArray[number][indicesVertices * 12 + 1] = coordinatesArray[number][indicesCoordinateV * 3 + 1];
                    verticesArray[number][indicesVertices * 12 + 2] = coordinatesArray[number][indicesCoordinateV * 3 + 2];
                    //нормали
                    indicesNormalsV = (Integer.parseInt(currentLine[3].split("/")[2].split("\r")[0]) - 1);//.split("\r")[0]
                    verticesArray[number][indicesVertices * 12 + 3] = normalsArray[number][indicesNormalsV * 3];
                    verticesArray[number][indicesVertices * 12 + 4] = normalsArray[number][indicesNormalsV * 3 + 1];
                    verticesArray[number][indicesVertices * 12 + 5] = normalsArray[number][indicesNormalsV * 3 + 2];
                    //цвета
                    verticesArray[number][indicesVertices * 12 + 6] = 1.0f;
                    verticesArray[number][indicesVertices * 12 + 7] = 1.0f;
                    verticesArray[number][indicesVertices * 12 + 8] = 0.0f;
                    verticesArray[number][indicesVertices * 12 + 9] = 0.0f;
                    //текстурные координаты
                    indicesTextureV = (Integer.parseInt(currentLine[3].split("/")[1]) - 1);
                    verticesArray[number][indicesVertices * 12 + 10] = texturesArray[number][indicesTextureV * 2];
                    verticesArray[number][indicesVertices * 12 + 11] = texturesArray[number][indicesTextureV * 2 + 1];

                    indicesArrayVertices[number][indicesVertices] = indicesVertices++;
                }
                line = new StringBuilder();
            }
        }
    }

    public static String[] getStringBuffer1()  { return model[0];  }
    public static String[] getStringBuffer2()  { return model[1];  }
    public static String[] getStringBuffer3()  { return model[2];  }
    public static String[] getStringBuffer4()  { return model[3];  }
    public static String[] getStringBuffer5()  { return model[4];  }
    public static String[] getStringBuffer6()  { return model[5];  }
    public static String[] getStringBuffer7()  { return model[6];  }
    public static String[] getStringBuffer8()  { return model[7];  }
    public static String[] getStringBuffer9()  { return model[8];  }
    public static String[] getStringBuffer10() { return model[9];  }
    public static String[] getStringBuffer11() { return model[10]; }
    public static String[] getStringBuffer12() { return model[11]; }
    public static String[] getStringBuffer13() { return model[12]; }
    public static String[] getStringBuffer14() { return model[13]; }
    public static String[] getStringBuffer15() { return model[14]; }
    public static String[] getStringBuffer16() { return model[15]; }
    public static String[] getStringBuffer17() { return model[16]; }
    public static String[] getStringBuffer18() { return model[17]; }
    public static String[] getStringBuffer19() { return model[18]; }


    //////////////////////////////////////////////////////////////////////////////
    /**                Передача готовых массивов на отрисовку                  **/
    //////////////////////////////////////////////////////////////////////////////
    public static float[] getVertexArray(int i){
        return verticesArray[i];
    }
    public static  int[] getIndicesArray(int i){
        return indicesArrayVertices[i];
    }


    public void startTransferThread () {
        // пальчики
        Thread transferThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (ChartActivity.transferThreadFlag) {
                    // пальчики
                    if (String.valueOf(selectStation).equals("SELECT_FINGER_1")) {
                        ChartActivity.fragmentGripperSettings.seekBarSpeedFinger.setProgress(ChartActivity.intValueFinger1Speed);
                    }
                    if (String.valueOf(selectStation).equals("SELECT_FINGER_2")) {
                        ChartActivity.fragmentGripperSettings.seekBarSpeedFinger.setProgress(ChartActivity.intValueFinger2Speed);
                    }
                    if (String.valueOf(selectStation).equals("SELECT_FINGER_3")) {
                        ChartActivity.fragmentGripperSettings.seekBarSpeedFinger.setProgress(ChartActivity.intValueFinger3Speed);
                    }
                    if (String.valueOf(selectStation).equals("SELECT_FINGER_4")) {
                        ChartActivity.fragmentGripperSettings.seekBarSpeedFinger.setProgress(ChartActivity.intValueFinger4Speed);
                    }
                    if (String.valueOf(selectStation).equals("SELECT_FINGER_5")) {
                        ChartActivity.fragmentGripperSettings.seekBarSpeedFinger.setProgress(ChartActivity.intValueFinger6Speed);
                    }
                    if (ChartActivity.lastSpeedFinger != ChartActivity.speedFinger && ChartActivity.isEnable) {
                        System.err.println("ChatActivity--------> speedFinger: " + ChartActivity.speedFinger);
                        String.valueOf(selectStation);
                        if (String.valueOf(selectStation).equals("SELECT_FINGER_1")) {
                            ChartActivity.intValueFinger1Speed = ChartActivity.speedFinger;
                        }
                        if (String.valueOf(selectStation).equals("SELECT_FINGER_2")) {
                            ChartActivity.intValueFinger2Speed = ChartActivity.speedFinger;
                        }
                        if (String.valueOf(selectStation).equals("SELECT_FINGER_3")) {
                            ChartActivity.intValueFinger3Speed = ChartActivity.speedFinger;
                        }
                        if (String.valueOf(selectStation).equals("SELECT_FINGER_4")) {
                            ChartActivity.intValueFinger4Speed = ChartActivity.speedFinger;
                        }
                        if (String.valueOf(selectStation).equals("SELECT_FINGER_5")) {
                            intValueFinger6Speed = ChartActivity.speedFinger;
                        }
                        ChartActivity.lastSpeedFinger = ChartActivity.speedFinger;
                    }
                    if (ChartActivity.intValueFinger1AngleLast != ChartActivity.intValueFinger1Angle && ChartActivity.isEnable) {
                        ChartActivity.numberFinger = 1;
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger1Angle, ChartActivity.intValueFinger1Speed));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(ChartActivity.numberFinger, ChartActivity.intValueFinger1Angle, ChartActivity.intValueFinger1Speed));
                        }
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger1Angle", ChartActivity.intValueFinger1Angle);
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger1Speed", ChartActivity.intValueFinger1Speed);

                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(ChartActivity.numberFinger));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(ChartActivity.numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)) {
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger1Angle, 99));
                        }
                        ChartActivity.intValueFinger1AngleLast = ChartActivity.intValueFinger1Angle;
                    }
                    if (ChartActivity.intValueFinger2AngleLast != ChartActivity.intValueFinger2Angle && ChartActivity.isEnable) {
                        ChartActivity.numberFinger = 2;
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger2Angle, ChartActivity.intValueFinger2Speed));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(ChartActivity.numberFinger, ChartActivity.intValueFinger2Angle, ChartActivity.intValueFinger2Speed));
                        }
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger2Angle", ChartActivity.intValueFinger2Angle);
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger2Speed", ChartActivity.intValueFinger2Speed);
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(ChartActivity.numberFinger));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(ChartActivity.numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)) {
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger2Angle, 99));
                        }
                        ChartActivity.intValueFinger2AngleLast = ChartActivity.intValueFinger2Angle;
                    }
                    if (ChartActivity.intValueFinger3AngleLast != ChartActivity.intValueFinger3Angle && ChartActivity.isEnable) {
                        ChartActivity.numberFinger = 3;
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger3Angle, ChartActivity.intValueFinger3Speed));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(ChartActivity.numberFinger, ChartActivity.intValueFinger3Angle, ChartActivity.intValueFinger3Speed));
                        }
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger3Angle", ChartActivity.intValueFinger3Angle);
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger3Speed", ChartActivity.intValueFinger3Speed);
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(ChartActivity.numberFinger));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(ChartActivity.numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)) {
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger3Angle, 99));
                        }
                        ChartActivity.intValueFinger3AngleLast = ChartActivity.intValueFinger3Angle;
                    }
                    if (ChartActivity.intValueFinger4AngleLast != ChartActivity.intValueFinger4Angle && ChartActivity.isEnable) {
                        ChartActivity.numberFinger = 4;
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger4Angle, ChartActivity.intValueFinger4Speed));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(ChartActivity.numberFinger, ChartActivity.intValueFinger4Angle, ChartActivity.intValueFinger4Speed));
                        }
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger4Angle", ChartActivity.intValueFinger4Angle);
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger4Speed", ChartActivity.intValueFinger4Speed);
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(ChartActivity.numberFinger));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(ChartActivity.numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)) {
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger4Angle, 99));
                        }
                        ChartActivity.intValueFinger4AngleLast = ChartActivity.intValueFinger4Angle;
                    }
                    if ((ChartActivity.intValueFinger5AngleLast != ChartActivity.intValueFinger5Angle && ChartActivity.isEnable) || (ChartActivity.intValueFinger6AngleLast != ChartActivity.intValueFinger6Angle && chatActivity.isEnable)) {
                        ChartActivity.numberFinger = 5;
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger5Angle, ChartActivity.intValueFinger5Speed));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(ChartActivity.numberFinger, ChartActivity.intValueFinger5Angle, ChartActivity.intValueFinger5Speed));
                        }
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger5Angle", ChartActivity.intValueFinger5Angle);
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger5Speed", ChartActivity.intValueFinger5Speed);
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(ChartActivity.numberFinger));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(ChartActivity.numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)) {
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger5Angle, 99));
                        }
                        ChartActivity.intValueFinger5AngleLast = ChartActivity.intValueFinger5Angle;
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        ChartActivity.numberFinger = 6;
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMessageSettingsHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger6Angle, ChartActivity.intValueFinger6Speed));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageSettings(ChartActivity.numberFinger, ChartActivity.intValueFinger6Angle, ChartActivity.intValueFinger6Speed));
                        }
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger6Angle", ChartActivity.intValueFinger6Angle);
                        chatActivity.saveVariable(deviceName + NUMBER_CELL + "intValueFinger6Speed", ChartActivity.intValueFinger6Speed);
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol) {
                            presenter.onHelloWorld(mMassages.CompileMassageControlHDLC(ChartActivity.numberFinger));
                        } else {
                            presenter.onHelloWorld(mMassages.CompileMassageControl(ChartActivity.numberFinger));
                        }
                        try {
                            Thread.sleep(delay);
                        } catch (Exception ignored) {
                        }
                        if (flagUseHDLCProtocol && (NUMBER_CELL == 0 || NUMBER_CELL == 2 || NUMBER_CELL == 4)) {
                            presenter.onHelloWorld(mMassages.CompileMassageSettingsDubbingHDLC(ChartActivity.numberFinger, ChartActivity.intValueFinger6Angle, 30));
                        }
                        ChartActivity.intValueFinger6AngleLast = ChartActivity.intValueFinger6Angle;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
        transferThread.start();
    }

    public static void transferFinger1Static (int angleFinger1){ ChartActivity.intValueFinger1Angle = angleFinger1; }
    public static void transferFinger2Static (int angleFinger2){ ChartActivity.intValueFinger2Angle = angleFinger2; }
    public static void transferFinger3Static (int angleFinger3){ ChartActivity.intValueFinger3Angle = angleFinger3; }
    public static void transferFinger4Static (int angleFinger4){ ChartActivity.intValueFinger4Angle = angleFinger4; }
    public static void transferFinger5Static (int angleFinger5){ ChartActivity.intValueFinger5Angle = angleFinger5; }
    public static void transferFinger6Static (int angleFinger6){ ChartActivity.intValueFinger6Angle = angleFinger6; }
}
