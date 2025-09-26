import Foundation

protocol WidgetDetailsViewModelInput {}

protocol WidgetDetailsViewModelOutput {
    var title: String { get }
}

protocol WidgetDetailsViewModel: WidgetDetailsViewModelInput, WidgetDetailsViewModelOutput { }

final class DefaultWidgetDetailsViewModel: WidgetDetailsViewModel {
    private var imageLoadTask: Cancellable? { willSet { imageLoadTask?.cancel() } }
    private let mainQueue: DispatchQueueType

    // MARK: - OUTPUT
    let title: String
    
    init(
        widget: Widget,
        mainQueue: DispatchQueueType = DispatchQueue.main
    ) {
        self.title = widget.title ?? ""
        self.mainQueue = mainQueue
    }
}
