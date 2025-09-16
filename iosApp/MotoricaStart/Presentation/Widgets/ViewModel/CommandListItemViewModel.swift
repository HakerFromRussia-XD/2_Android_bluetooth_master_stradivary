import Foundation

struct CommandListItemViewModel: Equatable, Hashable {
    private let uuid = UUID()
    let title: String
    let deviceAddress: Int
    let parameterID: Int
}

extension CommandListItemViewModel {
    init(widget: Widget) {
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
        hasher.combine(uuid)
    }

    static func == (lhs: CommandListItemViewModel, rhs: CommandListItemViewModel) -> Bool {
        lhs.uuid == rhs.uuid
    }
}
