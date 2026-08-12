## Purpose

Определяет единое поведение Android bottom navigation и iOS tab bar, при котором доступность каждого пункта зависит от наличия отображаемого содержимого соответствующего экрана.

## ADDED Requirements

### Requirement: Every navigation item follows widget availability
Система SHALL показывать пункт платформенной навигации только тогда, когда для связанного с ним экрана опубликован хотя бы один виджет. Правило SHALL применяться ко всем пунктам Android bottom navigation и iOS tab bar, а не только к экрану обучения.

#### Scenario: Screen has widgets
- **WHEN** для экрана опубликован один или более виджетов
- **THEN** связанный обычный пункт платформенной навигации отображается

#### Scenario: Screen has no widgets
- **WHEN** для экрана не опубликовано ни одного виджета
- **THEN** связанный пункт платформенной навигации не отображается

#### Scenario: All display mappings are evaluated
- **WHEN** система пересчитывает видимость навигации
- **THEN** Android и iOS сопоставляют жесты с `display = 0`, сенсоры с `display = 1`, специальные настройки с `display = 2`, обучение с `display = 3` и служебные настройки с `display = 4`

### Requirement: Secret navigation retains its access gate
Пункт служебных настроек SHALL на каждой платформе отображаться только при одновременном наличии хотя бы одного виджета `display = 4` и действующем разрешении на показ секретного пункта.

#### Scenario: Service widgets exist without access
- **WHEN** существуют виджеты `display = 4`, но показ секретного пункта не разрешён
- **THEN** пункт служебных настроек не отображается

#### Scenario: Service widgets and access are available
- **WHEN** существуют виджеты `display = 4` и показ секретного пункта разрешён
- **THEN** пункт служебных настроек отображается

### Requirement: Navigation reacts to widget changes
Система SHALL на Android и iOS пересчитывать видимость всех пунктов после публикации, замены или очистки набора виджетов.

#### Scenario: Widgets are removed from a visible screen
- **WHEN** обновление данных оставляет текущий экран без виджетов
- **THEN** его пункт скрывается без необходимости перезапуска Activity или ViewController hierarchy

#### Scenario: First widget appears on a hidden screen
- **WHEN** обновление данных добавляет первый виджет ранее пустому экрану
- **THEN** его пункт становится видимым без необходимости перезапуска Activity или ViewController hierarchy

### Requirement: Hidden current pages use a valid fallback
Если текущий пункт становится невидимым, система SHALL на соответствующей платформе перейти на доступный видимый пункт, предпочитая экран сенсоров. Если экран сенсоров недоступен, система SHALL выбрать первый доступный пункт в порядке меню. Система SHALL NOT выбирать скрытый пункт.

#### Scenario: Current page becomes empty while sensors are available
- **WHEN** текущий экран теряет последний виджет, а экран сенсоров содержит виджеты
- **THEN** система скрывает текущий пункт и открывает экран сенсоров

#### Scenario: Sensors are unavailable
- **WHEN** текущий экран теряет последний виджет и пункт сенсоров также скрыт
- **THEN** система выбирает первый доступный видимый пункт в порядке меню

#### Scenario: No screens contain widgets
- **WHEN** ни один экран не содержит виджетов или разрешённых пунктов
- **THEN** система не выбирает скрытый пункт и скрывает контейнер bottom navigation или tab bar до появления доступного экрана

### Requirement: User navigation choice is preserved
Автоматический fallback SHALL NOT возвращать пользователя на ранее скрытый экран после того, как пользователь вручную выбрал другой доступный пункт.

#### Scenario: User chooses another page after fallback
- **WHEN** система автоматически перешла с исчезнувшего экрана и пользователь затем выбрал другой видимый пункт
- **THEN** последующее появление виджетов исходного экрана не меняет сделанный пользователем выбор
