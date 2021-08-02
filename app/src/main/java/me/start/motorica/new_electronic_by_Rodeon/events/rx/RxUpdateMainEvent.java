package me.start.motorica.new_electronic_by_Rodeon.events.rx;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import me.start.motorica.new_electronic_by_Rodeon.models.FingerAngle;
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity;

public class RxUpdateMainEvent {

  private static RxUpdateMainEvent instance;
  private final PublishSubject<MainActivity.SelectStation> selectedFinger;
  private final PublishSubject<FingerAngle> fingerAngle;
  private final PublishSubject<Integer> fingerSpeed;

  private RxUpdateMainEvent() {
    selectedFinger = PublishSubject.create();
    fingerAngle = PublishSubject.create();
    fingerSpeed = PublishSubject.create();
  }
  public static RxUpdateMainEvent getInstance() {
    if (instance == null) {
      instance = new RxUpdateMainEvent();
    }
    return instance;
  }

  public void updateSelectedObject(MainActivity.SelectStation info) { selectedFinger.onNext(info); }
  public void updateFingerAngle(FingerAngle parameters) { fingerAngle.onNext(parameters); }
  public void updateFingerSpeed(Integer speed) { fingerSpeed.onNext(speed); }



  public Observable<MainActivity.SelectStation> getSelectedObjectObservable() { return selectedFinger; }
  public Observable<FingerAngle> getFingerAngleObservable() { return fingerAngle; }
  public Observable<Integer> getFingerSpeedObservable() { return fingerSpeed; }
}
