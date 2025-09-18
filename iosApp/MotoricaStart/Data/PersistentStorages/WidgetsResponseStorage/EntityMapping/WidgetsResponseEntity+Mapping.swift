import Foundation
import CoreData

extension WidgetsResponseEntity {
    func toDTO() -> WidgetsResponseDTO {
        return .init(
            page: Int(page),
            totalPages: Int(totalPages),

            widgets: (widgets?.allObjects as? [WidgetResponseEntity] ?? [])
                .sorted { $0.id < $1.id }
                .map { $0.toDTO() }
        )
    }
}

extension WidgetResponseEntity {
    func toDTO() -> WidgetsResponseDTO.WidgetDTO {
        return .init(
            id: Int(id),
            title: title,
            widgetType: WidgetsResponseDTO.WidgetDTO.WidgetTypeDTO(rawValue: widgetType ?? ""),
            overview: overview,
            releaseDate: releaseDate,
            isAd: isAd,
            widget: (value(forKey: "widgetData") as? Data).flatMap { data in
                try? JSONDecoder().decode(AnyCodable.self, from: data)
            }
        )
    }
}

extension WidgetsRequestDTO {
    func toEntity(in context: NSManagedObjectContext) -> WidgetsRequestEntity {
        let entity: WidgetsRequestEntity = .init(context: context)
        entity.query = query
        entity.page = Int32(page)
        return entity
    }
}

extension WidgetsResponseDTO {
    func toEntity(in context: NSManagedObjectContext) -> WidgetsResponseEntity {
        let entity: WidgetsResponseEntity = .init(context: context)
        entity.page = Int32(page)
        entity.totalPages = Int32(totalPages)
        widgets.forEach {
            entity.addToWidgets($0.toEntity(in: context))
        }
        return entity
    }
}

extension WidgetsResponseDTO.WidgetDTO {
    func toEntity(in context: NSManagedObjectContext) -> WidgetResponseEntity {
        let entity: WidgetResponseEntity = .init(context: context)
        entity.id = Int64(id)
        entity.title = title
        entity.widgetType = widgetType?.rawValue
        entity.overview = overview
        entity.releaseDate = releaseDate
        entity.isAd = isAd ?? false
        if let widget,
           let widgetData = try? JSONEncoder().encode(widget) {
            entity.setValue(widgetData, forKey: "widgetData")
        } else {
            entity.setValue(nil, forKey: "widgetData")
        }
        return entity
    }
}
