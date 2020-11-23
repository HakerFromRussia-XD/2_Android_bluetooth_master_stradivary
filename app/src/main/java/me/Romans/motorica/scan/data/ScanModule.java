package me.Romans.motorica.scan.data;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import me.Romans.motorica.scan.presenter.ScanPresenter;
import me.Romans.bluetooth.Bluetooth;
import me.Romans.motorica.scan.interactor.ScanInteractor;
import me.Romans.motorica.scan.interactor.ScanInteractorImpl;
import me.Romans.motorica.scan.presenter.ScanPresenterImpl;
import me.Romans.motorica.scan.view.ScanView;

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
