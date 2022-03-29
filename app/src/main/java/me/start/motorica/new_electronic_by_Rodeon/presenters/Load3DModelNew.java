package me.start.motorica.new_electronic_by_Rodeon.presenters;

import android.annotation.SuppressLint;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

import me.start.motorica.old_electronic_by_Misha.ui.chat.view.ChartActivity;

import static me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager.MAX_NUMBER_DETAILS;

public class Load3DModelNew {
    @SuppressLint("StaticFieldLeak")
    private  static Context context;
    private static String[] text;
    private final float[][] coordinatesArray = new float[MAX_NUMBER_DETAILS][];
    private final float[][] texturesArray = new float[MAX_NUMBER_DETAILS][];
    private final float[][] normalsArray = new float[MAX_NUMBER_DETAILS][];
    public volatile static float[][] verticesArray = new float[MAX_NUMBER_DETAILS][1];
    public static String[][] model = new String[MAX_NUMBER_DETAILS][];
    public volatile static int[][] indicesArrayVertices = new int[MAX_NUMBER_DETAILS][1];

    public Load3DModelNew(Context context) {
        Load3DModelNew.context = context;
    }

    //////////////////////////////////////////////////////////////////////////////
    /**                           Загрузка модели                              **/
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
    public void loadSTR2(final int i) {
        System.out.println("Количество запуск парса ");
        parserDataVertices(i);
        parserDataTextures(i);
        parserDataNormals(i);
        parserDataFacets(i);
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                   Компановка необходимых массивов                      **/
    //////////////////////////////////////////////////////////////////////////////
    public void parserDataVertices(int number){
        String text = "";
        System.out.println("Количество  %   parserDataVertices получение буферов строк   %" + getStringBuffer1()[1]);
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
                    if (currentLine[2].contains("vertices")) {//\r
                        coordinatesNumber = Integer.parseInt(currentLine[1]);
                        coordinatesArray[number] = new float[coordinatesNumber * 3];
                        System.out.println("Количество вершин: " + coordinatesNumber);
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
    public void parserDataTextures(int number){
        String text = "";
        System.out.println("Количество  %   parserDataTextures получение буферов строк   % ");
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
                    if(currentLine[2].contains("texture")){
                        texturesNumber = Integer.parseInt(currentLine[1]);
                        texturesArray[number] = new float[texturesNumber*2];
                        System.out.println("Количество текстурных координат: " + texturesNumber);
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
    public void parserDataNormals(int number){
        String text = "";
        System.out.println("Количество  %   parserDataNormals получение буферов строк   % ");
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
                    if (currentLine[2].contains("vertex")) {
                        normalsNumber = Integer.parseInt(currentLine[1]);
                        normalsArray[number] = new float[normalsNumber * 3];
                        System.out.println("Количество координат нормалей: " + normalsNumber);
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

    public void parserDataFacets (int number){
        String text = "";
        System.out.println("Количество  %   parserDataFacets получение буферов строк   % ");
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
        float[] v1 = new float[3];
        float[] v2 = new float[3];
        float[] v3 = new float[3];
        float[] uv1 = new float[2];
        float[] uv2 = new float[2];
        float[] uv3 = new float[2];
        float[] deltaPos1 = new float[3];
        float[] deltaPos2 = new float[3];
        float[] deltaUV1 = new float[2];
        float[] deltaUV2 = new float[2];
        float r;
        float[] tangent = new float[3];
        float[] bitangent = new float[3];

        for (char msg : text.toCharArray()){
            line.append(msg);
            if (msg == 10){
                String[] currentLine = line.toString().split(" ");
                if(line.toString().startsWith("# ")){
                    if(currentLine[2].contains("facets")){//\r
                        indicesVertices = Integer.parseInt(currentLine[1]);
                        verticesArray[number] = new float[indicesVertices*18*3];
                        indicesArrayVertices[number] = new int [indicesVertices*3];
                        System.out.println("Количество треугольников: " + indicesVertices);
                        indicesVertices = 0;
                    }
                } else if (line.toString().startsWith("f ")){
                    //первая тройка
                    //координаты вершины
                    indicesCoordinateV = (Integer.parseInt(currentLine[1].split("/")[0]) - 1);
                    verticesArray[number][indicesVertices * 18] = coordinatesArray[number][indicesCoordinateV * 3];
                    verticesArray[number][indicesVertices * 18 + 1] = coordinatesArray[number][indicesCoordinateV * 3 + 1];
                    verticesArray[number][indicesVertices * 18 + 2] = coordinatesArray[number][indicesCoordinateV * 3 + 2];
                    v1[0] = coordinatesArray[number][indicesCoordinateV * 3];
                    v1[1] = coordinatesArray[number][indicesCoordinateV * 3 + 1];
                    v1[2] = coordinatesArray[number][indicesCoordinateV * 3 + 2];
                    //нормали
                    indicesNormalsV = (Integer.parseInt(currentLine[1].split("/")[2]) - 1);
                    verticesArray[number][indicesVertices * 18 + 3] = normalsArray[number][indicesNormalsV * 3];
                    verticesArray[number][indicesVertices * 18 + 4] = normalsArray[number][indicesNormalsV * 3 + 1];
                    verticesArray[number][indicesVertices * 18 + 5] = normalsArray[number][indicesNormalsV * 3 + 2];
                    //цвета
                    verticesArray[number][indicesVertices * 18 + 6] = 1.0f;
                    verticesArray[number][indicesVertices * 18 + 7] = 1.0f;
                    verticesArray[number][indicesVertices * 18 + 8] = 0.0f;
                    verticesArray[number][indicesVertices * 18 + 9] = 0.0f;
                    //текстурные координаты
                    indicesTextureV = (Integer.parseInt(currentLine[1].split("/")[1]) - 1);
                    verticesArray[number][indicesVertices * 18 + 10] = texturesArray[number][indicesTextureV * 2];
                    verticesArray[number][indicesVertices * 18 + 11] = texturesArray[number][indicesTextureV * 2 + 1];
                    uv1[0] = texturesArray[number][indicesTextureV * 2];
                    uv1[1] = texturesArray[number][indicesTextureV * 2 + 1];

                    indicesArrayVertices[number][indicesVertices] = indicesVertices++;

                    //вторая тройка
                    //координаты вершины
                    indicesCoordinateV = (Integer.parseInt(currentLine[2].split("/")[0]) - 1);
                    verticesArray[number][indicesVertices * 18] = coordinatesArray[number][indicesCoordinateV * 3];
                    verticesArray[number][indicesVertices * 18 + 1] = coordinatesArray[number][indicesCoordinateV * 3 + 1];
                    verticesArray[number][indicesVertices * 18 + 2] = coordinatesArray[number][indicesCoordinateV * 3 + 2];
                    v2[0] = coordinatesArray[number][indicesCoordinateV * 3];
                    v2[1] = coordinatesArray[number][indicesCoordinateV * 3 + 1];
                    v2[2] = coordinatesArray[number][indicesCoordinateV * 3 + 2];
                    //нормали
                    verticesArray[number][indicesVertices * 18 + 3] = normalsArray[number][(Integer.parseInt(currentLine[2].split("/")[2]) - 1) * 3];
                    verticesArray[number][indicesVertices * 18 + 4] = normalsArray[number][(Integer.parseInt(currentLine[2].split("/")[2]) - 1) * 3 + 1];
                    verticesArray[number][indicesVertices * 18 + 5] = normalsArray[number][(Integer.parseInt(currentLine[2].split("/")[2]) - 1) * 3 + 2];
                    //цвета
                    verticesArray[number][indicesVertices * 18 + 6] = 1.0f;
                    verticesArray[number][indicesVertices * 18 + 7] = 1.0f;
                    verticesArray[number][indicesVertices * 18 + 8] = 0.0f;
                    verticesArray[number][indicesVertices * 18 + 9] = 0.0f;
                    //текстурные координаты
                    indicesTextureV = (Integer.parseInt(currentLine[2].split("/")[1]) - 1);
                    verticesArray[number][indicesVertices * 18 + 10] = texturesArray[number][indicesTextureV * 2];
                    verticesArray[number][indicesVertices * 18 + 11] = texturesArray[number][indicesTextureV * 2 + 1];
                    uv2[0] = texturesArray[number][indicesTextureV * 2];
                    uv2[1] = texturesArray[number][indicesTextureV * 2 + 1];

                    indicesArrayVertices[number][indicesVertices] = indicesVertices++;

                    //третья тройка
                    //координаты вершины
                    indicesCoordinateV = (Integer.parseInt(currentLine[3].split("/")[0]) - 1);
                    verticesArray[number][indicesVertices * 18] = coordinatesArray[number][indicesCoordinateV * 3];
                    verticesArray[number][indicesVertices * 18 + 1] = coordinatesArray[number][indicesCoordinateV * 3 + 1];
                    verticesArray[number][indicesVertices * 18 + 2] = coordinatesArray[number][indicesCoordinateV * 3 + 2];
                    v3[0] = coordinatesArray[number][indicesCoordinateV * 3];
                    v3[1] = coordinatesArray[number][indicesCoordinateV * 3 + 1];
                    v3[2] = coordinatesArray[number][indicesCoordinateV * 3 + 2];
                    //нормали
                    if (currentLine[3].split("/")[2].contains("\n")) {
                        System.out.println("Количество % строка содержит \\n %");
                    }
                    if (currentLine[3].split("/")[2].contains("\r")) {
                        System.out.println("Количество % строка содержит \\r %");
                    }
                    indicesNormalsV = (Integer.parseInt(currentLine[3].split("/")[2].split("\n")[0]) - 1);//.split("\r")[0]
                    verticesArray[number][indicesVertices * 18 + 3] = normalsArray[number][indicesNormalsV * 3];
                    verticesArray[number][indicesVertices * 18 + 4] = normalsArray[number][indicesNormalsV * 3 + 1];
                    verticesArray[number][indicesVertices * 18 + 5] = normalsArray[number][indicesNormalsV * 3 + 2];
                    //цвета
                    verticesArray[number][indicesVertices * 18 + 6] = 1.0f;
                    verticesArray[number][indicesVertices * 18 + 7] = 1.0f;
                    verticesArray[number][indicesVertices * 18 + 8] = 0.0f;
                    verticesArray[number][indicesVertices * 18 + 9] = 0.0f;
                    //текстурные координаты
                    indicesTextureV = (Integer.parseInt(currentLine[3].split("/")[1]) - 1);
                    verticesArray[number][indicesVertices * 18 + 10] = texturesArray[number][indicesTextureV * 2];
                    verticesArray[number][indicesVertices * 18 + 11] = texturesArray[number][indicesTextureV * 2 + 1];
                    uv3[0] = texturesArray[number][indicesTextureV * 2];
                    uv3[1] = texturesArray[number][indicesTextureV * 2 + 1];

                    //расчёт тангента и битангента
                    deltaPos1[0] = v2[0]-v1[0];
                    deltaPos1[1] = v2[1]-v1[1];
                    deltaPos1[2] = v2[2]-v1[2];

                    deltaPos2[0] = v3[0]-v1[0];
                    deltaPos2[1] = v3[1]-v1[1];
                    deltaPos2[2] = v3[2]-v1[2];

                    deltaUV1[0] = uv2[0]-uv1[0];
                    deltaUV1[1] = uv2[1]-uv1[1];

                    deltaUV2[0] = uv3[0]-uv1[0];
                    deltaUV2[1] = uv3[1]-uv1[1];

                    r = 1.0f / (deltaUV1[0] * deltaUV2[1] - deltaUV1[1] * deltaUV2[0]);
                    tangent[0] = (deltaPos1[0] * deltaUV2[1] -deltaPos2[0] * deltaUV1[1]) * r;
                    tangent[1] = (deltaPos1[1] * deltaUV2[1] -deltaPos2[1] * deltaUV1[1]) * r;
                    tangent[2] = (deltaPos1[2] * deltaUV2[1] -deltaPos2[2] * deltaUV1[1]) * r;
                    bitangent[0] = (deltaPos2[0] * deltaUV1[0] -deltaPos1[0] * deltaUV2[0]) * r;
                    bitangent[1] = (deltaPos2[1] * deltaUV1[0] -deltaPos1[1] * deltaUV2[0]) * r;
                    bitangent[2] = (deltaPos2[2] * deltaUV1[0] -deltaPos1[2] * deltaUV2[0]) * r;

                    //присваиваем значения тангента и битангента каждой вершино по порядку
                    //тангент 1
                    verticesArray[number][(indicesVertices - 2) * 18 + 12] = tangent[0];
                    verticesArray[number][(indicesVertices - 2) * 18 + 13] = tangent[1];
                    verticesArray[number][(indicesVertices - 2) * 18 + 14] = tangent[2];
                    //битангент 1
                    verticesArray[number][(indicesVertices - 2) * 18 + 15] = bitangent[0];
                    verticesArray[number][(indicesVertices - 2) * 18 + 16] = bitangent[1];
                    verticesArray[number][(indicesVertices - 2) * 18 + 17] = bitangent[2];
                    //тангент 2
                    verticesArray[number][(indicesVertices - 1) * 18 + 12] = tangent[0];
                    verticesArray[number][(indicesVertices - 1) * 18 + 13] = tangent[1];
                    verticesArray[number][(indicesVertices - 1) * 18 + 14] = tangent[2];
                    //битангент 2
                    verticesArray[number][(indicesVertices - 1) * 18 + 15] = bitangent[0];
                    verticesArray[number][(indicesVertices - 1) * 18 + 16] = bitangent[1];
                    verticesArray[number][(indicesVertices - 1) * 18 + 17] = bitangent[2];
                    //тангент 3
                    verticesArray[number][indicesVertices * 18 + 12] = tangent[0];
                    verticesArray[number][indicesVertices * 18 + 13] = tangent[1];
                    verticesArray[number][indicesVertices * 18 + 14] = tangent[2];
                    //битангент 3
                    verticesArray[number][indicesVertices * 18 + 15] = bitangent[0];
                    verticesArray[number][indicesVertices * 18 + 16] = bitangent[1];
                    verticesArray[number][indicesVertices * 18 + 17] = bitangent[2];

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


    public static void transferFinger1Static (int angleFinger1){ ChartActivity.intValueFinger1Angle = angleFinger1; }
    public static void transferFinger2Static (int angleFinger2){ ChartActivity.intValueFinger2Angle = angleFinger2; }
    public static void transferFinger3Static (int angleFinger3){ ChartActivity.intValueFinger3Angle = angleFinger3; }
    public static void transferFinger4Static (int angleFinger4){ ChartActivity.intValueFinger4Angle = angleFinger4; }
    public static void transferFinger5Static (int angleFinger5){ ChartActivity.intValueFinger5Angle = angleFinger5; }
    public static void transferFinger6Static (int angleFinger6){ ChartActivity.intValueFinger6Angle = angleFinger6; }
}
