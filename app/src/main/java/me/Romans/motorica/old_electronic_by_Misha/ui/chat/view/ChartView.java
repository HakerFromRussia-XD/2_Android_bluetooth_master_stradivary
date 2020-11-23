package me.Romans.motorica.old_electronic_by_Misha.ui.chat.view;


public interface ChartView {
    void setStatus(String status);
    void setStatus(int resId);
    void setValueCH(int levelCH, int numberChannel);
    void setErrorReception (boolean incomeErrorReception);
    void enableInterface(boolean enabled);
    void showToast(String message);
    void showToastWithoutConnection();
    void onGestureClick(int position);
    void setGeneralValue(int receiveCurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension);
    void setStartParameters(Integer receiveCurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication, Byte receiveRoughnessOfSensors);
    void setStartParametersInChartActivity();
    boolean getFirstRead();
    void setFlagReceptionExpectation (Boolean flagReceptionExpectation);

    void setStartParametersTrigCH1(Integer receiveLevelTrigCH1);
    void setStartParametersTrigCH2(Integer receiveLevelTrigCH2);
    void setStartParametersCurrent(Integer receiveCurrent);
    void setStartParametersBlock(Byte receiveBlockIndication);
    void setStartParametersRoughness(Byte receiveRoughnessOfSensors);
    void setStartParametersBattery(Integer receiveBatteryTension);
}
