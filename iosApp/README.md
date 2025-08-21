
# Коротко о том что лежит в базе приложения (Clean Architecture and MVVM)

iOS проект использует архитектуру "чистых слоёв" и MVVM. **больше информации можно получить в статье**: <a href="https://tech.olx.com/clean-architecture-and-mvvm-on-ios-c9d167d9f5b3">Medium Post about Clean Architecture + MVVM</a>


![Alt text](README_FILES/CleanArchitecture+MVVM.png?raw=true "Clean Architecture Layers")

## Слои
* **Domain Layer** = Entities + Use Cases + Repositories Interfaces
* **Data Repositories Layer** = Repositories Implementations + API (Network) + Persistence DB
* **Presentation Layer (MVVM)** = ViewModels + Views

### Направление зависимостей
![Alt text](README_FILES/CleanArchitectureDependencies.png?raw=true "Modules Dependencies")

**Примечание:** **Доменный слой** не должен включать ничего из других слоев(e.g Presentation — UIKit or SwiftUI or Data Layer — Mapping Codable)
