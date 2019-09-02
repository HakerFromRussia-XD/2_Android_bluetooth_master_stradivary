package me.Romans.bluetooth;

public interface ParserCallback {
    Integer givsLenhgt (int lenght);
//    void lol (Integer intege, setText text);
    void givsRequest (Boolean request);
    void givsChannel (int channel);
    void givsLevelCH (int levelCH, int channel);
    void givsGeneralParcel (int current, int levelCH1, int levelCH2, byte indicationState, int batteryTension);
    void givsStartParameters (int current, int levelTrigCH1, int levelTrigCH2, byte indicationInvertMode, byte blockIndication);
    void givsRegister (Integer register);
    void givsCorrectAcceptance (Boolean correct_acceptence);
    void givsErrorReception(Boolean givsErrorReception);
}
