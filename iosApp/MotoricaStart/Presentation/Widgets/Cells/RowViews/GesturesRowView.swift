import SwiftUI
import Combine
import UIKit
import UniformTypeIdentifiers


//private struct RotationGestureDropDelegate: DropDelegate {
//    let currentItem: GesturesProvider.GestureDisplayItem
//    @Binding var items: [GesturesProvider.GestureDisplayItem]
//    @Binding var draggedItem: GesturesProvider.GestureDisplayItem?
//    var onReorder: ([GesturesProvider.GestureDisplayItem]) -> Void
//
//    func dropEntered(info: DropInfo) {
//        guard let draggedItem,
//              draggedItem != currentItem,
//              let fromIndex = items.firstIndex(of: draggedItem),
//              let toIndex = items.firstIndex(of: currentItem) else { return }
//
//        items.move(fromOffsets: IndexSet(integer: fromIndex), toOffset: toIndex > fromIndex ? toIndex + 1 : toIndex)
//
//        withAnimation(.easeInOut) {
//            onReorder(items)
//        }
//    }
//
//    func dropUpdated(info: DropInfo) -> DropProposal? {
//        DropProposal(operation: .move)
//    }
//
//    func performDrop(info: DropInfo) -> Bool {
//        draggedItem = nil
//        return true
//    }
//}
