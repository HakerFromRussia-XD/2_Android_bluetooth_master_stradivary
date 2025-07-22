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
    private let subject = CurrentValueSubject<Int, Never>(0)
    var publisher: AnyPublisher<Int, Never> { subject.eraseToAnyPublisher() }
    
    private var timerCancellable: AnyCancellable?
    
    private init() {
        timerCancellable = Timer.publish(every: 0.5,
                                         on: .main,
                                         in: .common)
            .autoconnect()
            .scan(0) { current, _ in current >= 100 ? 0 : current + 1 }
            .sink { [weak self] value in self?.subject.send(value) }
    }
}
