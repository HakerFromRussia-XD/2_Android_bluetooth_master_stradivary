package com.bailout.stick.scan.data;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import com.bailout.stick.scan.presenter.ScanPresenter;
import com.bailout.bluetooth.Bluetooth;
import com.bailout.stick.scan.interactor.ScanInteractor;
import com.bailout.stick.scan.interactor.ScanInteractorImpl;
import com.bailout.stick.scan.presenter.ScanPresenterImpl;
import com.bailout.stick.scan.view.ScanView;


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
