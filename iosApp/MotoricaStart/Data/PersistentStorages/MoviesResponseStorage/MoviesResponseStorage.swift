import Foundation

protocol MoviesResponseStorage {
    func getResponse(
        for request: MoviesRequestDTO,
        completion: @escaping (Result<WidgetsResponseDTO?, Error>) -> Void
    )
    func save(response: WidgetsResponseDTO, for requestDto: MoviesRequestDTO)
}
