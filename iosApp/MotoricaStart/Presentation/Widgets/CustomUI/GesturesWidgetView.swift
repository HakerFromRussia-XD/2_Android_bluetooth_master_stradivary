//
//  GesturesWidgetView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 28.10.2025.
//

import SwiftUI
import UIKit
import UniformTypeIdentifiers


struct GesturesWidgetView: View {
    
    // MARK: - Dependencies
    @ObservedObject var provider: GesturesProvider
    @State private var highlightOffsetX: CGFloat = 0

    var onSegmentChange: (GesturesProvider.Segment) -> Void
    var onFactoryGestureTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onCustomGestureTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onCustomGestureSettingsTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onRotationGestureMoveUp: (Int) -> Void
    var onRotationGestureMoveDown: (Int) -> Void
    var onRotationGestureRemove: (Int) -> Void
    var onRotationGestureAdd: () -> Void
    var onRotationGesturesReorder: ([GesturesProvider.GestureDisplayItem]) -> Void
    var onSprGestureAction: (GesturesProvider.SprGestureDisplayItem) -> Void
    var onSprAddTap: () -> Void

    
    // MARK: - Body
    var body: some View {
        VStack(spacing: 16) {
            segmentSelector
            Group {
                activeGestureView
                
                switch provider.selectedSegment {
                case .collection:
                    collectionView
                case .rotationGroup:
                    rotationGroupView
                case .sprGroup:
                    sprGroupView
                }
            }
            .animation(nil, value: provider.selectedSegment)
        }
        .transaction { transaction in
            transaction.animation = nil
        }
        .padding(.horizontal, 8)
        .background(Color("ubi4_back"))
    }

    
    // MARK: - Segment Selector
    private var segmentSelector: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let segmentCount = CGFloat(GesturesProvider.Segment.allCases.count)
            let segmentWidth = (width) / segmentCount
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color("ubi4_gray"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                    )
                    .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)

                RoundedRectangle(cornerRadius: 10)
                    .fill(Color("ubi4_back"))
                    .padding(1)
                    .frame(width: segmentWidth)
                    .offset(x: highlightOffsetX)
                    .animation(.easeOut(duration: 0.3), value: highlightOffsetX)

                HStack(spacing: 0) {
                    ForEach(Array(GesturesProvider.Segment.allCases.enumerated()), id: \.offset) { index, segment in
                        Button(action: { select(segment: segment) }) {
                            Text(segment.title)
                                .font(.system(size: 12, weight: .light))
                                .foregroundColor(segment == provider.selectedSegment ? .white : Color("ubi4_deactivate_text"))
                                .animation(nil, value: provider.selectedSegment)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                        }
                        .animation(nil, value: provider.selectedSegment)
                        .buttonStyle(.plain)
                    }
                }
                .padding(2)
            }
            .onChange(of: provider.selectedSegment) { _ in
                updateHighlightOffset(segmentWidth: segmentWidth, animated: true)
            }
        }
        .transaction { transaction in
            transaction.animation = nil
        }
        .frame(height: 48)
    }
    
    private func updateHighlightOffset(segmentWidth: CGFloat, animated: Bool) {
        let index = GesturesProvider.Segment.allCases.firstIndex(of: provider.selectedSegment) ?? 0
        let newOffset = CGFloat(index) * segmentWidth
        if animated {
            withAnimation(.easeOut(duration: 0.3)) {
                highlightOffsetX = newOffset
            }
        } else {
            highlightOffsetX = newOffset
        }
    }
    
    private func select(segment: GesturesProvider.Segment) {
        guard provider.selectedSegment != segment else { return }
        provider.selectedSegment = segment
        onSegmentChange(segment)
    }

    
    // MARK: - Active Gesture
    private var activeGestureView: some View {
        VStack(alignment: .leading, spacing: 8) {
            let activeTitle = provider.activeGestureTitle ?? NSLocalizedString("gesture_not_selected", comment: "")
            let format = NSLocalizedString("active_gesture_is", comment: "")
            Text(String(format: format, activeTitle))
                .font(.system(size: 12, weight: .light))
                .foregroundColor(.white)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color("ubi4_gray"))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
        )
    }

    
    // MARK: - Collection View
    private var collectionView: some View {
        VStack(alignment: .leading, spacing: 16) {
            collapsibleSection(
                title: NSLocalizedString("collection_of_gestures", comment: ""),
                isExpanded: provider.isFactoryExpanded,
                toggle: toggleFactorySection
            ) {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 2), spacing: 12) {
                    ForEach(provider.factoryGestures) { item in
                        GestureTile(
                            title: item.title,
                            subtitle: item.subtitle,
                            image: item.image,
                            isActive: provider.activeGestureId == item.id,
                            action: { onFactoryGestureTap(item) }
                        )
                        .aspectRatio(1, contentMode: .fit)
                    }
                }
                .padding(.top, 4)
            }

            VStack(alignment: .leading, spacing: 12) {
                Text(NSLocalizedString("custom_gestures_section", comment: ""))
                    .font(.system(size: 12, weight: .light))
                    .foregroundColor(.white)

                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 2), spacing: 12) {
                    ForEach(provider.customGestures) { item in
                        CustomGestureTile(
                            title: item.title,
                            subtitle: item.subtitle,
                            isActive: provider.activeGestureId == item.id,
                            onTap: { onCustomGestureTap(item) },
                            onSettingsTap: { onCustomGestureSettingsTap(item) }
                        )
                    }
                }
            }
        }
    }

    private func toggleFactorySection() {
        provider.isFactoryExpanded.toggle()
    }
    
    private func collapsibleSection<Content: View>(
        title: String,
        isExpanded: Bool,
        toggle: @escaping () -> Void,
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.system(size: 12, weight: .light))
                    .foregroundColor(.white)
                Spacer()
                Button(action: toggle) {
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(.white)
                        .padding(6)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color("ubi4_gray"))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8)
                                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                                )
                        )
                        .animation(nil, value: isExpanded)
                }
                .buttonStyle(.plain)
            }

            if isExpanded {
                content()
            }
        }
        .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
    }

    
    // MARK: - Rotation Group View
    private var rotationGroupView: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(NSLocalizedString("rotation_group", comment: ""))
                .font(.system(size: 12, weight: .light))
                .foregroundColor(.white)

            VStack(spacing: 12) {
//                ForEach(Array(provider.rotationGroup.enumerated()), id: \.offset) { index, item in
//                    RotationGestureRow(
//                        title: item.title,
//                        subtitle: item.subtitle,
//                        onMoveUp: { onRotationGestureMoveUp(index) },
//                        onMoveDown: { onRotationGestureMoveDown(index) },
//                        onRemove: { onRotationGestureRemove(index) }
//                    )
//                }
                RotationGesturesReorderView(
                    items: $provider.rotationGroup,
                    onRemove: { index in onRotationGestureRemove(index) },
                    onReorder: { items in
                        onRotationGesturesReorder(items)
                    }
                )
                Button(action: onRotationGestureAdd) {
                    Label(NSLocalizedString("add_gesture", comment: ""), systemImage: "plus")
                        .font(.system(size: 12, weight: .light))
                        .foregroundColor( .white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color("ubi4_gray"))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                                )
                        )
                }
                .buttonStyle(.plain)
                .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
            }

            Text(NSLocalizedString("rotation_group_hint", comment: ""))
                .font(.system(size: 12, weight: .light))
                .foregroundColor(Color("ubi4_deactivate_text"))
        }
    }

    
    // MARK: - SPR Group View
    private var sprGroupView: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(NSLocalizedString("spr_gestures", comment: ""))
                .font(.system(size: 12, weight: .light))
                .foregroundColor(.white)

            if provider.sprGestures.isEmpty {
                Text(NSLocalizedString("spr_empty_placeholder", comment: ""))
                    .font(.system(size: 12, weight: .light))
                    .foregroundColor(Color("ubi4_deactivate_text"))
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 24)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color("ubi4_gray"))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                            )
                    )
            } else {
                VStack(spacing: 12) {
                    ForEach(provider.sprGestures) { item in
                        SprGestureRow(
                            title: item.title,
                            subtitle: item.subtitle,
                            onTap: { onSprGestureAction(item) }
                        )
                    }
                }
            }

            Button(action: onSprAddTap) {
                Label(NSLocalizedString("control_gestures", comment: ""), systemImage: "plus")
                    .font(.system(size: 12, weight: .light))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color("ubi4_gray"))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                            )
                    )
            }
            .buttonStyle(.plain)

            Text(NSLocalizedString("annotation_main_text", comment: ""))
                .font(.system(size: 12, weight: .light))
                .foregroundColor(Color("ubi4_deactivate_text"))
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
    }
}


// MARK: - Gesture Tiles
private struct GestureTile: View {
    let title: String
    let subtitle: String?
    let image: UIImage?
    let isActive: Bool
    let action: () -> Void

    private var borderColor: Color {
        isActive ? Color("ubi4_active") : Color("ubi4_gray_border")
    }

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.system(size: 12, weight: .light))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.leading)
                
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(maxWidth: .infinity)
                        .frame(maxWidth: .infinity, alignment: .center)
                }

                if let subtitle, subtitle.isEmpty == false {
                    Text(subtitle)
                        .font(.system(size: 10, weight: .light))
                        .foregroundColor(Color("ubi4_deactivate_text"))
                }

                Spacer()
            }
            .padding(12)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color("ubi4_gray"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(borderColor, lineWidth: 1)
                    )
                    .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
            )
        }
        .buttonStyle(.plain)
    }
}

private struct CustomGestureTile: View {
    let title: String
    let subtitle: String?
    let isActive: Bool
    let onTap: () -> Void
    let onSettingsTap: () -> Void

    private var borderColor: Color {
        isActive ? Color("ubi4_active") : Color("ubi4_gray_border")
    }

    var body: some View {
        ZStack(alignment: .trailing) {
            Button(action: onTap) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(title)
                            .font(.system(size: 12, weight: .light))
                            .foregroundColor(.white)
                        if let subtitle, subtitle.isEmpty == false {
                            Text(subtitle)
                                .font(.system(size: 10, weight: .light))
                                .foregroundColor(Color("ubi4_deactivate_text"))
                        }
                    }
                    Spacer()
                }
                .padding(.horizontal, 12)
                .frame(height: 48)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color("ubi4_gray"))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(borderColor, lineWidth: 1)
                        )
                        .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
                )
            }
            .buttonStyle(.plain)

            Button(action: onSettingsTap) {
                Image(systemName: "gearshape.fill")
                    .font(.system(size: 14))
                    .foregroundColor(Color("ubi4_back"))
                    .padding(10)
                    .background(
                        Circle()
                            .fill(Color.white.opacity(0.9))
                            .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
                    )
                    .padding(.trailing, 8)
            }
            .buttonStyle(.plain)
        }
    }
}

private struct RotationGesturesReorderView: View {
    @Binding var items: [GesturesProvider.GestureDisplayItem]
    var onRemove: (Int) -> Void
    var onReorder: ([GesturesProvider.GestureDisplayItem]) -> Void

    @State private var draggedItem: GesturesProvider.GestureDisplayItem?

    var body: some View {
        VStack(spacing: 12) {
            ForEach(items) { item in
                RotationGestureRow(
                    title: item.title,
                    subtitle: item.subtitle,
                    isDragging: draggedItem == item,
                    onRemove: {
                        if let index = items.firstIndex(of: item) {
                            onRemove(index)
                        }
                    }
                )
                .onDrag {
                    draggedItem = item
                    return NSItemProvider(object: NSString(string: "rotation-\(item.id)"))
                }
                .onDrop(
                    of: [UTType.text],
                    delegate: RotationGestureDropDelegate(
                        currentItem: item,
                        items: $items,
                        draggedItem: $draggedItem,
                        onReorder: onReorder
                    )
                )
            }
        }
    }
}

private struct RotationGestureRow: View {
    let title: String
    let subtitle: String?
//    var onMoveUp: () -> Void
//    var onMoveDown: () -> Void
    var isDragging: Bool
    var onRemove: () -> Void

    private var backgroundColor: Color {
        isDragging ? Color("ubi4_gray").opacity(0.8) : Color("ubi4_gray")
    }
    
    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 12, weight: .light))
                    .foregroundColor(.white)
                if let subtitle, subtitle.isEmpty == false {
                    Text(subtitle)
                        .font(.system(size: 10, weight: .light))
                        .foregroundColor(Color("ubi4_deactivate_text"))
                }
            }
            Spacer()
//            controlButton(systemName: "chevron.up", action: onMoveUp)
//            controlButton(systemName: "chevron.down", action: onMoveDown)
            Image(systemName: "line.3.horizontal")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Color("ubi4_deactivate_text"))
                .padding(.vertical, 12)
            controlButton(systemName: "trash", action: onRemove)
        }
        .padding(.horizontal, 12)
        .background(
            RoundedRectangle(cornerRadius: 12)
//                .fill(Color("ubi4_gray"))
                .fill(backgroundColor)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
        )
        .animation(.easeInOut(duration: 0.3), value: isDragging)
    }

    private func controlButton(systemName: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 28, height: 28)
                .background(
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Color("ubi4_back"))
                        .overlay(
                            RoundedRectangle(cornerRadius: 6)
                                .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                        )
                )
        }
        .buttonStyle(.plain)
    }
}

private struct RotationGestureDropDelegate: DropDelegate {
    let currentItem: GesturesProvider.GestureDisplayItem
    @Binding var items: [GesturesProvider.GestureDisplayItem]
    @Binding var draggedItem: GesturesProvider.GestureDisplayItem?
    var onReorder: ([GesturesProvider.GestureDisplayItem]) -> Void

    func dropEntered(info: DropInfo) {
        guard let draggedItem,
              draggedItem != currentItem,
              let fromIndex = items.firstIndex(of: draggedItem),
              let toIndex = items.firstIndex(of: currentItem) else { return }

        items.move(fromOffsets: IndexSet(integer: fromIndex), toOffset: toIndex > fromIndex ? toIndex + 1 : toIndex)

        withAnimation(.easeInOut) {
            onReorder(items)
        }
    }

    func dropUpdated(info: DropInfo) -> DropProposal? {
        DropProposal(operation: .move)
    }

    func performDrop(info: DropInfo) -> Bool {
        draggedItem = nil
        return true
    }
}

private struct SprGestureRow: View {
    let title: String
    let subtitle: String?
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 12, weight: .light))
                        .foregroundColor(.white)
                    if let subtitle, subtitle.isEmpty == false {
                        Text(subtitle)
                            .font(.system(size: 10, weight: .light))
                            .foregroundColor(Color("ubi4_deactivate_text"))
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(Color("ubi4_deactivate_text"))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color("ubi4_gray"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                    )
                    .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
            )
        }
        .buttonStyle(.plain)
    }
}
