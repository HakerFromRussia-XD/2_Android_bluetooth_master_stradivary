import Foundation

struct WidgetsRequestDTO: Encodable {
    let query: String
    let page: Int
}
