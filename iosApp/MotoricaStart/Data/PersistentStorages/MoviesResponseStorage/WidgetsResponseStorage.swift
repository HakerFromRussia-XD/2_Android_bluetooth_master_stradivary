import Foundation

protocol WidgetsResponseStorage {
    func getResponse(
        for request: WidgetsRequestDTO,
        completion: @escaping (Result<WidgetsResponseDTO?, Error>) -> Void
    )
    func save(response: WidgetsResponseDTO, for requestDto: WidgetsRequestDTO)
}
