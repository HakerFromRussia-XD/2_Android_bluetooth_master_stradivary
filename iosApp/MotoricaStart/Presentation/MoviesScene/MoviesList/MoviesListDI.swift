//
//  MoviesListDI.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 07.05.2025.
//

import Foundation

struct MoviesListDI {                                                  // Assistant
    static func makeViewModel() -> DefaultMoviesListViewModel {
        let moviesRepo = DefaultMoviesRepository()                     // Assistant
        let queriesRepo = DefaultMoviesQueriesRepository()             // Assistant
        let useCase = DefaultSearchMoviesUseCase(
            moviesRepository: moviesRepo,
            moviesQueriesRepository: queriesRepo
        )
        return DefaultMoviesListViewModel(searchMoviesUseCase: useCase)
    }
}
