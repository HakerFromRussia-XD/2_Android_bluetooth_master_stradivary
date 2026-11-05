import Foundation

enum AccountGamesCatalogError: Error, Equatable {
    case invalidResponse
    case httpStatus(Int)
}

final class AccountGamesCatalogClient {
    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    @discardableResult
    func load(from url: URL, completion: @escaping (Result<AccountGamesManifest, Error>) -> Void) -> URLSessionDataTask {
        // Version metadata must be checked against the server on every refresh.
        var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 30)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        let task = session.dataTask(with: request) { data, response, error in
            if let error {
                completion(.failure(error))
                return
            }
            guard let response = response as? HTTPURLResponse, let data else {
                completion(.failure(AccountGamesCatalogError.invalidResponse))
                return
            }
            guard (200..<300).contains(response.statusCode) else {
                completion(.failure(AccountGamesCatalogError.httpStatus(response.statusCode)))
                return
            }
            completion(Result { try JSONDecoder().decode(AccountGamesManifest.self, from: data) })
        }
        task.resume()
        return task
    }
}
