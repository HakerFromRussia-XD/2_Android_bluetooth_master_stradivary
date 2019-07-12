package me.aflak.motorica.ui.scan.data;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.aflak.bluetooth.Bluetooth;
import me.aflak.motorica.ui.scan.interactor.ScanInteractor;
import me.aflak.motorica.ui.scan.interactor.ScanInteractorImpl;
import me.aflak.motorica.ui.scan.presenter.ScanPresenter;
import me.aflak.motorica.ui.scan.presenter.ScanPresenterImpl;
import me.aflak.motorica.ui.scan.view.ScanView;

/**
 * Created by Omar on 20/12/2017.
 */

@Module
public class ScanModule {
    private ScanView view;

    public ScanModule(ScanView view) {
        this.view = view;
    }

    @Provides @Singleton
    public ScanView provideScanView(){
        return view;
    }

    @Provides @Singleton
    public ScanInteractor provideScanInteractor(Bluetooth bluetooth){
        return new ScanInteractorImpl(bluetooth);
    }

    @Provides @Singleton
    public ScanPresenter provideScanPresenter(ScanView view, ScanInteractor interactor){
        return new ScanPresenterImpl(view, interactor);
    }
}
