package me.start.motorica.old_electronic_by_Misha.ui.chat.data;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.start.bluetooth.Bluetooth;
import me.start.motorica.old_electronic_by_Misha.ui.chat.interactor.NemoStandInteractor;
import me.start.motorica.old_electronic_by_Misha.ui.chat.interactor.NemoStandInteractorImpl;
import me.start.motorica.old_electronic_by_Misha.ui.chat.presenter.NemoStandPresenter;
import me.start.motorica.old_electronic_by_Misha.ui.chat.presenter.NemoStandPresenterImpl;
import me.start.motorica.old_electronic_by_Misha.ui.chat.view.NemoStandView;

@Module
public class NemoStandModule {
    private NemoStandView view;

    public NemoStandModule(NemoStandView view) {
        this.view = view;
    }

    @Provides @Singleton
    public NemoStandView provideNemoStandView(){
        return view;
    }

    @Provides @Singleton
    public NemoStandInteractor provideNemoStandInteractor(Bluetooth bluetooth){
        return new NemoStandInteractorImpl(bluetooth);
    }

    @Provides @Singleton
    public NemoStandPresenter provideNemoStandPresenter(NemoStandView view, NemoStandInteractor interactor){
        return new NemoStandPresenterImpl(view, interactor);
    }
}
