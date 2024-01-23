package com.bailout.stickk.old_electronic_by_Misha.ui.chat.view;


public interface NemoStandView {
    void setStatus(String status);
    void setStatus(int resId);
    void setErrorReception (boolean incomeErrorReception);
    void enableInterface(boolean enabled);
    void showToast(String message);
    void showToastWithoutConnection();
    void setStartParametersInNemoStandActivity();
    void setFlagReceptionExpectation (Boolean flagReceptionExpectation);
}
