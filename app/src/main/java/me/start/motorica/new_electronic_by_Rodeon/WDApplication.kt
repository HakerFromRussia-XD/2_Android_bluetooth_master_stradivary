/* Licensed under the Apache License, Version 2.0 (the "License");
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

package me.start.motorica.new_electronic_by_Rodeon

import androidx.multidex.MultiDexApplication
import dagger.Component
import me.start.motorica.old_electronic_by_Misha.data.BluetoothModule
import javax.inject.Singleton
import me.start.motorica.new_electronic_by_Rodeon.ApplicationModule as ApplicationModule1


class WDApplication : MultiDexApplication() {
  private var bluetoothModule: BluetoothModule? = null


  override fun onCreate() {
    super.onCreate()
    app = this
    component = DaggerWDApplication_ApplicationComponent.builder()
        .applicationModule(ApplicationModule1(this))
        .build()
    bluetoothModule = BluetoothModule(this)
  }

  companion object {
    var app: WDApplication? = null

    lateinit var component: ApplicationComponent

    @JvmStatic
    fun app(): WDApplication? {
      return app
    }
  }

  @Singleton
  @Component(modules = [ApplicationModule1::class])
  interface ApplicationComponent : ApplicationGraph

  fun bluetoothModule(): BluetoothModule? {
    return bluetoothModule
  }
}
