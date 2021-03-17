/*Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.romans.motorica.new_electronic_by_Rodeon.events.rx;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class RxUpdateMainEvent {

  private static RxUpdateMainEvent instance;
  private PublishSubject<Boolean> subject;

  private RxUpdateMainEvent() {
    subject = PublishSubject.create();
  }

  public static RxUpdateMainEvent getInstance() {
    if (instance == null) {
      instance = new RxUpdateMainEvent();
    }
    return instance;
  }

  public void updateBadge() {
    subject.onNext(false);
  }

  public Observable<Boolean> getObservable() {
    return subject;
  }
}
