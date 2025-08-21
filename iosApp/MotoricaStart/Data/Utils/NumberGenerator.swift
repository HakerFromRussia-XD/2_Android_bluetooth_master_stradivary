//
//  NumberGenerator.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 14.05.2025.
//
import Foundation
import Combine

/// Генерирует числа 0…100 каждые 0.5 c
final class NumberGenerator {
    static let shared = NumberGenerator()          // Assistant: синглтон для DI-контейнера
    private let subject = CurrentValueSubject<(Int, Int), Never>((0, 0))
    var publisher: AnyPublisher<(Int, Int), Never> { subject.eraseToAnyPublisher() }
    
    private var timerCancellable: AnyCancellable?
    
    private init() {
        timerCancellable = Timer.publish(every: 0.5,
                                         on: .main,
                                         in: .common)
            .autoconnect()
            .scan((0, 50)) { current, _ in
                            let first = current.0 >= 100 ? 0 : current.0 + 1
                            let second = current.1 >= 100 ? 0 : current.1 + 1
                            return (first, second)
                        }
            .sink { [weak self] value in self?.subject.send(value) }
    }
}
