package me.start.motorica.new_electronic_by_Rodeon.events.rx;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import me.start.motorica.new_electronic_by_Rodeon.models.FingerAngle;
import me.start.motorica.new_electronic_by_Rodeon.models.GestureState;
import me.start.motorica.new_electronic_by_Rodeon.models.GestureStateWithEncoders;
import me.start.motorica.new_electronic_by_Rodeon.models.SensorsStates;
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity;

public class RxUpdateMainEvent {

  private static RxUpdateMainEvent instance;
  private final PublishSubject<Integer> selectedFinger;
  private final PublishSubject<SensorsStates> sensorsStates;
  private final PublishSubject<FingerAngle> fingerAngle;
  private final PublishSubject<GestureState> gestureState;
  private final PublishSubject<GestureStateWithEncoders> gestureStateWithEncoders;
  private final PublishSubject<Integer> fingerSpeed;

  private RxUpdateMainEvent() {
    selectedFinger = PublishSubject.create();
    sensorsStates = PublishSubject.create();
    fingerAngle = PublishSubject.create();
    gestureState = PublishSubject.create();
    gestureStateWithEncoders = PublishSubject.create();
    fingerSpeed = PublishSubject.create();
  }
  public static RxUpdateMainEvent getInstance() {
    if (instance == null) {
      instance = new RxUpdateMainEvent();
    }
    return instance;
  }

  public void updateSelectedObject(Integer info) { selectedFinger.onNext(info); }
  public void updateSensorsStatesObject(SensorsStates parameters) { sensorsStates.onNext(parameters); }
  public void updateFingerAngle(FingerAngle parameters) { fingerAngle.onNext(parameters); }
  public void updateGestureState(GestureState parameters) { gestureState.onNext(parameters); }
  public void updateGestureWithEncodersState(GestureStateWithEncoders parameters) { gestureStateWithEncoders.onNext(parameters); }
  public void updateFingerSpeed(Integer speed) { fingerSpeed.onNext(speed); }

  public Observable<Integer> getSelectedObjectObservable() { return selectedFinger; }
  public Observable<SensorsStates> getSensorsStatesObjectObservable() { return sensorsStates; }
  public Observable<FingerAngle> getFingerAngleObservable() { return fingerAngle; }
  public Observable<GestureState> getGestureStateObservable() { return gestureState; }
  public Observable<GestureStateWithEncoders> getGestureStateWithEncodersObservable() { return gestureStateWithEncoders; }
  public Observable<Integer> getFingerSpeedObservable() { return fingerSpeed; }
}
