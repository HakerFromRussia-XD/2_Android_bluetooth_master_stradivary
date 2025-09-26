import Foundation

protocol WidgetDetailsViewModelInput {}

protocol WidgetDetailsViewModelOutput {
    var title: String { get }
    var posterImage: Observable<Data?> { get }
}

protocol WidgetDetailsViewModel: WidgetDetailsViewModelInput, WidgetDetailsViewModelOutput { }

final class DefaultWidgetDetailsViewModel: WidgetDetailsViewModel {
    private let posterImagesRepository: PosterImagesRepository
    private var imageLoadTask: Cancellable? { willSet { imageLoadTask?.cancel() } }
    private let mainQueue: DispatchQueueType

    // MARK: - OUTPUT
    let title: String
    let posterImage: Observable<Data?> = Observable(nil)
    
    init(
        widget: Widget,
        posterImagesRepository: PosterImagesRepository,
        mainQueue: DispatchQueueType = DispatchQueue.main
    ) {
        self.title = widget.title ?? ""
        self.posterImagesRepository = posterImagesRepository
        self.mainQueue = mainQueue
    }
}
