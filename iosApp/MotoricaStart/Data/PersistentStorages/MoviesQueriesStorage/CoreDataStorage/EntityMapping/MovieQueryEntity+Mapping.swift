import Foundation
import CoreData

extension WidgetQueryEntity {
    convenience init(widgetQuery: WidgetQuery, insertInto context: NSManagedObjectContext) {
        self.init(context: context)
        query = widgetQuery.query
        createdAt = Date()
    }
}

extension WidgetQueryEntity {
    func toDomain() -> WidgetQuery {
        return .init(query: query ?? "")
    }
}
