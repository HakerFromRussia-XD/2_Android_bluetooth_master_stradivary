package com.bailout.stickk.ubi4.rx;

import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle;
import com.bailout.stickk.ubi4.models.GestureInfo;
import com.bailout.stickk.ubi4.models.GestureWithAddress;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class RxUpdateMainEventUbi4 {

  private static RxUpdateMainEventUbi4 instance;
  private final PublishSubject<FingerAngle> fingerAngle;
  private final PublishSubject<GestureWithAddress> gestureStateWithEncoders;
  private final PublishSubject<GestureInfo> readCharacteristicBLE;

  // ui
  private final PublishSubject<Integer> allFragmentUi;
  private final PublishSubject<Integer> uiGestureSettings;
  private final PublishSubject<Integer> uiRotationGroup;


  private RxUpdateMainEventUbi4() {
    fingerAngle = PublishSubject.create();
    gestureStateWithEncoders = PublishSubject.create();
    readCharacteristicBLE = PublishSubject.create();
    uiGestureSettings = PublishSubject.create();
    uiRotationGroup = PublishSubject.create();
    allFragmentUi = PublishSubject.create();
  }
  public static RxUpdateMainEventUbi4 getInstance() {
    if (instance == null) {
      instance = new RxUpdateMainEventUbi4();
    }
    return instance;
  }

  public void updateFingerAngle(FingerAngle parameters) { fingerAngle.onNext(parameters); }
  public void updateGestureWithEncodersState(GestureWithAddress parameters) { gestureStateWithEncoders.onNext(parameters); }
  public void updateReadCharacteristicBLE(GestureInfo parameters) { readCharacteristicBLE.onNext(parameters); }
  public void updateUiGestureSettings(Integer parameters) { uiGestureSettings.onNext(parameters); }
  public void updateUiRotationGroup(Integer parameters) { uiRotationGroup.onNext(parameters); }
  public void updateAllFragmentUi(Integer parameters) { allFragmentUi.onNext(parameters); }




  public Observable<FingerAngle> getFingerAngleObservable() { return fingerAngle; }
  public Observable<GestureWithAddress> getGestureStateWithEncodersObservable() { return gestureStateWithEncoders; }
  public Observable<GestureInfo> getReadCharacteristicBLE() { return readCharacteristicBLE; }
  public Observable<Integer> getUiGestureSettingsObservable() { return uiGestureSettings; }
  public Observable<Integer> getUiRotationGroupObservable() { return uiRotationGroup; }
  public Observable<Integer> getAllFragmentUiObservable() { return allFragmentUi; }
}
