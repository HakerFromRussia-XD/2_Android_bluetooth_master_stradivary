import Foundation

struct CommandListItemViewModel: Equatable, Hashable {
    let id: Widget.Identifier
    let title: String
    let deviceAddress: Int
    let parameterID: Int
}

extension CommandListItemViewModel {
    init(id: Widget.Identifier, widget: Widget) {
        self.id = id
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
        hasher.combine(id)
    }

    static func == (lhs: CommandListItemViewModel, rhs: CommandListItemViewModel) -> Bool {
        lhs.id == rhs.id
    }
}
