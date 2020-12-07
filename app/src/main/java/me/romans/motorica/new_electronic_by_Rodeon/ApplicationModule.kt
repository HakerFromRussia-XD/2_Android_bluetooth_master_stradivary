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

package me.romans.motorica.new_electronic_by_Rodeon

import android.app.Application
import me.romans.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.romans.motorica.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
class ApplicationModule(private val application: Application) {

  @Provides
  @Singleton
  internal fun providePreferenceManager(): PreferenceManager {
    return PreferenceManager(this.application)
  }

  @Provides
  @Singleton
  internal fun provideSqliteManager(): SqliteManager {
    return SqliteManager(this.application,
        SqliteManager.DATABASE_NAME,
        null,
        SqliteManager.DATABASE_VERSION)
  }
}
