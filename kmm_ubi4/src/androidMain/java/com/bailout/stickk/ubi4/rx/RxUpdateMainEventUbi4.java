package com.bailout.stickk.ubi4.rx;

import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle;
import com.bailout.stickk.ubi4.models.ble.ParameterRef;
import com.bailout.stickk.ubi4.models.gestures.GestureInfo;
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class RxUpdateMainEventUbi4 {

  private static RxUpdateMainEventUbi4 instance;

  // существующие subject
  private final PublishSubject<FingerAngle> fingerAngle;
  private final PublishSubject<GestureWithAddress> gestureStateWithEncoders;
  private final PublishSubject<GestureInfo> readCharacteristicBLE;
  private final PublishSubject<Integer> uiGestureSettings;
  private final PublishSubject<ParameterRef> uiRotationGroup;
  private final PublishSubject<ParameterRef> uiOpticTraining;

  // новый subject для AccountMain
  private final PublishSubject<Boolean> uiAccountMain;

  private RxUpdateMainEventUbi4() {
    fingerAngle = PublishSubject.create();
    gestureStateWithEncoders = PublishSubject.create();
    readCharacteristicBLE = PublishSubject.create();
    uiGestureSettings = PublishSubject.create();
    uiRotationGroup = PublishSubject.create();
    uiOpticTraining = PublishSubject.create();
    uiAccountMain = PublishSubject.create();              // инициализация
  }

  public static RxUpdateMainEventUbi4 getInstance() {
    if (instance == null) {
      instance = new RxUpdateMainEventUbi4();
    }
    return instance;
  }

  // существующие методы
  public void updateFingerAngle(FingerAngle parameters) {
    fingerAngle.onNext(parameters);
  }

  public void updateGestureWithEncodersState(GestureWithAddress parameters) {
    gestureStateWithEncoders.onNext(parameters);
  }

  public void updateReadCharacteristicBLE(GestureInfo parameters) {
    readCharacteristicBLE.onNext(parameters);
  }

  public void updateUiGestureSettings(Integer parameters) {
    uiGestureSettings.onNext(parameters);
  }

  public void updateUiRotationGroup(ParameterRef parameters) {
    uiRotationGroup.onNext(parameters);
  }

  public void updateUiOpticTraining(ParameterRef parameters) {
    uiOpticTraining.onNext(parameters);
  }

  // новый метод для отправки состояния AccountMain
  public void updateUIAccountMain(Boolean state) {
    uiAccountMain.onNext(state);
  }

  // существующие геттеры
  public Observable<FingerAngle> getFingerAngleObservable() {
    return fingerAngle;
  }

  public Observable<GestureWithAddress> getGestureStateWithEncodersObservable() {
    return gestureStateWithEncoders;
  }

  public Observable<GestureInfo> getReadCharacteristicBLE() {
    return readCharacteristicBLE;
  }

  public Observable<Integer> getUiGestureSettingsObservable() {
    return uiGestureSettings;
  }

  public Observable<ParameterRef> getUiRotationGroupObservable() {
    return uiRotationGroup;
  }

  public Observable<ParameterRef> getUiOpticTrainingObservable() {
    return uiOpticTraining;
  }

  // новый геттер для подписки на AccountMain
  public Observable<Boolean> getUIAccountMain() {
    return uiAccountMain;
  }
}