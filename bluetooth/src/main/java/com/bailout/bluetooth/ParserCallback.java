package com.bailout.bluetooth;

public interface ParserCallback {
    Integer givesLenhgt (int lenght);
//    void lol (Integer intege, setText text);
    void givesRequest (Boolean request);
    void givesChannel (int channel);
    void givesLevelCH (int levelCH, int channel);
    void givesGeneralParcel (int current, int levelCH1, int levelCH2, byte indicationState, int batteryTension);
    void givesStartParameters (int current, int levelTrigCH1, int levelTrigCH2, byte indicationInvertMode, byte blockIndication, byte roughnessOfSensors);
    void givesStartParametersTrigCH1 (int levelTrigCH1);
    void givesStartParametersTrigCH2(int levelTrigCH2);
    void givesStartParametersCurrent(int current);
    void givesStartParametersBlock(byte blockIndication);
    void givesStartParametersRoughness(byte roughnessOfSensors);
    void givesStartParametersBattery(int msgBatteryTensionf);

    void givesRegister (Integer register);
    void givesCorrectAcceptance (Boolean correct_acceptence);
    void givesErrorReception(Boolean givesErrorReception);
    void setStartParametersInNemoStandActivity();
    boolean getFlagUseHDLCProtocol();
    boolean getFlagReceptionExpectation();
    void  setFlagReceptionExpectation(Boolean flagReceptionExpectation);


}
