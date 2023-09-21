package me.start.motorica.new_electronic_by_Rodeon.events.rx;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import me.start.motorica.new_electronic_by_Rodeon.models.FingerAngle;
import me.start.motorica.new_electronic_by_Rodeon.models.FingersEncoderValue;
import me.start.motorica.new_electronic_by_Rodeon.models.GestureState;
import me.start.motorica.new_electronic_by_Rodeon.models.GestureStateWithEncoders;

public class RxUpdateMainEvent {

  private static RxUpdateMainEvent instance;
  private final PublishSubject<Integer> selectedFinger;
  private final PublishSubject<FingerAngle> fingerAngle;
  private final PublishSubject<GestureState> gestureState;
  private final PublishSubject<GestureStateWithEncoders> gestureStateWithEncoders;
  private final PublishSubject<Integer> fingerSpeed;
  private final PublishSubject<Boolean> calibrating;
  private final PublishSubject<String> characteristics;
  private final PublishSubject<Boolean> updateUIAdvancedSettings;
  private final PublishSubject<Boolean> updateResetAdvancedSettings;
  private final PublishSubject<Integer> updateCommunicationTestResult;
  private final PublishSubject<Boolean> updateUIChart;
  private final PublishSubject<Integer> updateUIGestures;
  private final PublishSubject<Integer> updateEncodersError;
  private final PublishSubject<FingersEncoderValue> updateEncoders;




  private RxUpdateMainEvent() {
    selectedFinger = PublishSubject.create();
    fingerAngle = PublishSubject.create();
    gestureState = PublishSubject.create();
    gestureStateWithEncoders = PublishSubject.create();
    fingerSpeed = PublishSubject.create();
    calibrating = PublishSubject.create();
    characteristics = PublishSubject.create();
    updateUIAdvancedSettings = PublishSubject.create();
    updateResetAdvancedSettings = PublishSubject.create();
    updateCommunicationTestResult = PublishSubject.create();
    updateUIChart = PublishSubject.create();
    updateUIGestures = PublishSubject.create();
    updateEncoders = PublishSubject.create();
    updateEncodersError = PublishSubject.create();
  }
  public static RxUpdateMainEvent getInstance() {
    if (instance == null) {
      instance = new RxUpdateMainEvent();
    }
    return instance;
  }

  public void updateSelectedObject(Integer info) { selectedFinger.onNext(info); }
  public void updateFingerAngle(FingerAngle parameters) { fingerAngle.onNext(parameters); }
  public void updateGestureState(GestureState parameters) { gestureState.onNext(parameters); }
  public void updateGestureWithEncodersState(GestureStateWithEncoders parameters) { gestureStateWithEncoders.onNext(parameters); }
  public void updateFingerSpeed(Integer speed) { fingerSpeed.onNext(speed); }
  public void updateCalibrationStatus(Boolean status) { calibrating.onNext(status); }
  public void updateReadCharacteristicBLE(String characteristic) { characteristics.onNext(characteristic); }
  public void updateUIAdvancedSettings(Boolean state) { updateUIAdvancedSettings.onNext(state); }
  public void updateResetAdvancedSettings(Boolean state) { updateResetAdvancedSettings.onNext(state); }
  public void updateCommunicationTestResult(Integer attempt) { updateCommunicationTestResult.onNext(attempt); }
  public void updateUIChart(Boolean state) { updateUIChart.onNext(state); }
  public void updateUIGestures(Integer number) { updateUIGestures.onNext(number); }
  public void updateEncodersError(Integer number) { updateEncodersError.onNext(number); }
  public void updateEncoders(FingersEncoderValue values) { updateEncoders.onNext(values); }



  public Observable<Integer> getSelectedObjectObservable() { return selectedFinger; }
  public Observable<FingerAngle> getFingerAngleObservable() { return fingerAngle; }
  public Observable<GestureState> getGestureStateObservable() { return gestureState; }
  public Observable<GestureStateWithEncoders> getGestureStateWithEncodersObservable() { return gestureStateWithEncoders; }
  public Observable<Integer> getFingerSpeedObservable() { return fingerSpeed; }
  public Observable<Boolean> getCalibratingStatusObservable() { return calibrating; }
  public Observable<String> getReadCharacteristicBLEObservable() { return characteristics; }
  public Observable<Boolean> getUIChart() { return updateUIChart; }
  public Observable<Boolean> getUIAdvancedSettings() { return updateUIAdvancedSettings; }
  public Observable<Boolean> getResetAdvancedSettings() { return updateResetAdvancedSettings; }
  public Observable<Integer> getCommunicationTestResult() { return updateCommunicationTestResult; }
  public Observable<Integer> getUIGestures() { return updateUIGestures; }
  public Observable<Integer> getEncodersError() { return updateEncodersError; }
  public Observable<FingersEncoderValue> getEncoders() { return updateEncoders; }
}
