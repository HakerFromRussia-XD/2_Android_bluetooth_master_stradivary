import Foundation

struct CommandListItemViewModel: Equatable, Hashable {
    private let identifier: String
    let title: String
    let deviceAddress: Int
    let parameterID: Int
}

extension CommandListItemViewModel {
    init(widget: Widget) {
        self.identifier = "\(widget.deviceAddress)-\(widget.parameterID)"
        self.title = widget.title ?? ""
        self.deviceAddress = widget.deviceAddress
        self.parameterID = widget.parameterID
    }
    
    func didPressDown() {
        print("didPressDown")
    }
    func didRelease() {
        print("didRelease")
    }
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(identifier)
        hasher.combine(title)
    }

    static func == (lhs: CommandListItemViewModel, rhs: CommandListItemViewModel) -> Bool {
        lhs.identifier == rhs.identifier
        && lhs.title == rhs.title
    }
}
