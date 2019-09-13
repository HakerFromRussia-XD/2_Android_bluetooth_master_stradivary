package me.Romans.motorica.ui.chat.view;


public interface ChatView {
    void setStatus(String status);
    void setStatus(int resId);
    void setValueCH(int levelCH, int numberChannel);

    void setErrorReception (boolean incomeErrorReception);
    void appendMessage(String message);
    void enableHWButton(boolean enabled);
    void showToast(String message);

    void onGestureClick(int position);
    void setGeneralValue(int receiveСurrent, int receiveLevelCH1, int receiveLevelCH2, byte receiveIndicationState, int receiveBatteryTension);
    void setStartParameters(Integer receiveСurrent, Integer receiveLevelTrigCH1, Integer receiveLevelTrigCH2, Byte receiveIndicationInvertMode, Byte receiveBlockIndication);
    void setStartParametersInGraphActivity();

}
