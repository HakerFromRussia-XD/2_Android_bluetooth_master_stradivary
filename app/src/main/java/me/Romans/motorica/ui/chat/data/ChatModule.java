package me.Romans.motorica.ui.chat.data;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.Romans.bluetooth.Bluetooth;
import me.Romans.motorica.ui.chat.interactor.ChatInteractor;
import me.Romans.motorica.ui.chat.interactor.ChatInteractorImpl;
import me.Romans.motorica.ui.chat.presenter.ChatPresenter;
import me.Romans.motorica.ui.chat.presenter.ChatPresenterImpl;
import me.Romans.motorica.ui.chat.view.ChatView;

@Module
public class ChatModule {
    private ChatView view;

    public ChatModule(ChatView view) {
        this.view = view;
    }

    @Provides @Singleton
    public ChatView provideChatView(){
        return view;
    }

    @Provides @Singleton
    public ChatInteractor provideChatInteractor(Bluetooth bluetooth){
        return new ChatInteractorImpl(bluetooth);
    }

    @Provides @Singleton
    public ChatPresenter provideChatPresenter(ChatView view, ChatInteractor interactor){
        return new ChatPresenterImpl(view, interactor);
    }
}
