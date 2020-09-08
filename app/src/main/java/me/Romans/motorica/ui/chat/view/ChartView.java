package me.Romans.motorica.ui.chat.view;


public interface ChatView {
    void setStatus(String status);
    void setStatus(int resId);
    void setValueCH(int levelCH, int numberChannel);
    void setErrorReception (boolean incomeErrorReception);
    void appendMessage(String message);
    void enableHWButton(boolean enabled);
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
