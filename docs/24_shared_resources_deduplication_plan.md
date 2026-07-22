# План дедупликации ресурсов app/shared

## Цель

Оставить единый исходный набор общих ресурсов в `shared/src/commonMain/moko-resources` и удалить идентичные ручные копии из Android-модуля `app`.

## Ограничения

- Удаляются только побайтно идентичные ресурсы либо ресурсы, для которых Android-ссылки безопасно переведены на сгенерированные Moko Resources.
- Одноимённые, но отличающиеся по содержимому файлы не объединяются автоматически: каждый конфликт проверяется отдельно.
- Android-специфичные ресурсы без общей копии остаются в `app`.
- Сгенерированные каталоги `build/generated` не являются исходниками и не редактируются вручную.

## План

1. Сопоставить ресурсы `app/src/main/res` и `shared/src/commonMain/moko-resources` по имени и контрольной сумме.
2. Отдельно выявить одноимённые ресурсы с различающимся содержимым.
3. Проверить все ссылки Android XML/Kotlin на кандидатов к удалению.
4. Убедиться, что Moko генерирует Android-ресурсы с совместимыми именами в AAR модуля `shared`.
5. Удалить идентичные копии из `app`, сохранив единственные исходники в `shared`.
6. Запустить генерацию Moko Resources и `:app:compileDebugKotlinAndroid`.
7. Проверить merged resources, чтобы подтвердить наличие ресурсов в итоговом Android-приложении.

## Критерии готовности

- Общие изображения физически хранятся только в `shared/src/commonMain/moko-resources/images`.
- Android XML и Kotlin успешно разрешают ресурсы из зависимости `:shared`.
- В `app/src/main/res` отсутствуют удалённые дубликаты.
- Сборка `:app:compileDebugKotlinAndroid` завершается успешно.

## Результат

- 350 drawable-ресурсов удалены из `app/src/main/res/drawable`.
- 302 Android-only ресурса перенесены в активный каталог `shared/src/androidMain/res/drawable`.
- Неактивная копия `shared/src/androidMain/kotlin/com/bailout/stickk/ubi4/resources` удалена.
- 28 изображений, общих для Android и iOS, оставлены только в `shared/src/commonMain/moko-resources/images`.
- 4 Lottie JSON оставлены только в `shared/src/commonMain/moko-resources/files`; Android-ссылки переведены на имена, генерируемые Moko Resources.
- Для совместимости существующего Android-кода включён транзитивный `R` (`android.nonTransitiveRClass=false`).
- Повторная проверка не обнаружила побайтно одинаковых файлов ресурсов между `app/src/main` и `shared/src`.
- `./gradlew :app:compileDebugKotlinAndroid` завершился успешно.
