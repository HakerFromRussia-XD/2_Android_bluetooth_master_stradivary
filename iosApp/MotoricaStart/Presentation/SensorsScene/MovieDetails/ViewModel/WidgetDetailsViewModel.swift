import Foundation

protocol WidgetDetailsViewModelInput {}

protocol WidgetDetailsViewModelOutput {
    var title: String { get }
    var posterImage: Observable<Data?> { get }
    var overview: String { get }
}

protocol WidgetDetailsViewModel: WidgetDetailsViewModelInput, WidgetDetailsViewModelOutput { }

final class DefaultWidgetDetailsViewModel: WidgetDetailsViewModel {
    private let posterImagesRepository: PosterImagesRepository
    private var imageLoadTask: Cancellable? { willSet { imageLoadTask?.cancel() } }
    private let mainQueue: DispatchQueueType

    // MARK: - OUTPUT
    let title: String
    let posterImage: Observable<Data?> = Observable(nil)
    let overview: String
    
    init(
        widget: Widget,
        posterImagesRepository: PosterImagesRepository,
        mainQueue: DispatchQueueType = DispatchQueue.main
    ) {
        self.title = widget.title ?? ""
        self.overview = widget.overview ?? ""
        self.posterImagesRepository = posterImagesRepository
        self.mainQueue = mainQueue
    }
}
