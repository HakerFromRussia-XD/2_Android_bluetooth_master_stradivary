package com.bailout.stickk.scan.data;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.migration.DisableInstallInCheck;

import com.bailout.stickk.scan.presenter.ScanPresenter;
import com.bailout.bluetooth.Bluetooth;
import com.bailout.stickk.scan.interactor.ScanInteractor;
import com.bailout.stickk.scan.interactor.ScanInteractorImpl;
import com.bailout.stickk.scan.presenter.ScanPresenterImpl;
import com.bailout.stickk.scan.view.ScanView;


@Module
@DisableInstallInCheck
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
