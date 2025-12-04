//
//  Debouncer.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 03.12.2025.
//

import Foundation

/// Универсальный дебаунсер: выполняет действие только через паузу после последнего вызова.
final class Debouncer {

    private var workItem: DispatchWorkItem?
    private let queue: DispatchQueue
    private let delay: TimeInterval

    /// - Parameters:
    ///   - delay: задержка в секундах после последнего события
    ///   - queue: очередь выполнения (по умолчанию main)
    init(delay: TimeInterval, queue: DispatchQueue = .main) {
        self.delay = delay
        self.queue = queue
    }

    /// Запланировать выполнение действия. Каждый новый вызов сбрасывает предыдущий таймер.
    func schedule(_ action: @escaping () -> Void) {
        workItem?.cancel()

        let item = DispatchWorkItem(block: action)
        workItem = item

        queue.asyncAfter(deadline: .now() + delay, execute: item)
    }

    /// Отменить запланированное действие.
    func cancel() {
        workItem?.cancel()
        workItem = nil
    }
}
