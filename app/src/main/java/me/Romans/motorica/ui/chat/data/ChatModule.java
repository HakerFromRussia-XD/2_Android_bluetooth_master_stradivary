package me.Romans.motorica.ui.chat.data;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.Romans.bluetooth.Bluetooth;
import me.Romans.motorica.ui.chat.interactor.ChatInteractor;
import me.Romans.motorica.ui.chat.interactor.ChatInteractorImpl;
import me.Romans.motorica.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.ui.chat.presenter.ChatPresenterImpl;
import me.Romans.motorica.ui.chat.view.ChartView;

@Module
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
        return new ChatInteractorImpl(bluetooth);
    }

    @Provides @Singleton
    public ChatPresenter provideChatPresenter(ChartView view, ChatInteractor interactor){
        return new ChatPresenterImpl(view, interactor);
    }
}
