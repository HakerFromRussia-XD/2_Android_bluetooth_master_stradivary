import Foundation

struct CommandListItemViewModel: Equatable, Hashable {
    let title: String
    let deviceAddress: Int
    let parameterID: Int
}

extension CommandListItemViewModel {
    init(widget: Widget, showSecondSlider: Bool = false) {
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
}
