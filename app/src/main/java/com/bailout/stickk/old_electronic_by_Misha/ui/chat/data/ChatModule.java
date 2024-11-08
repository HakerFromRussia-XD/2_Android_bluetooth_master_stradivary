package com.bailout.stickk.old_electronic_by_Misha.ui.chat.data;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.migration.DisableInstallInCheck;

import com.bailout.bluetooth.Bluetooth;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.interactor.ChatInteractor;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.interactor.ChartInteractorImpl;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.presenter.ChatPresenter;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.presenter.ChartPresenterImpl;
import com.bailout.stickk.old_electronic_by_Misha.ui.chat.view.ChartView;

@Module
@DisableInstallInCheck
public class ChatModule {
    private ChartView view;

    public ChatModule(ChartView view) {
        this.view = view;
    }

    @Provides @Singleton
    public ChartView provideChatView(){
        return view;
    }

    @Provides @Singleton
    public ChatInteractor provideChatInteractor(Bluetooth bluetooth){
        return new ChartInteractorImpl(bluetooth);
    }

    @Provides @Singleton
    public ChatPresenter provideChatPresenter(ChartView view, ChatInteractor interactor){
        return new ChartPresenterImpl(view, interactor);
    }
}
