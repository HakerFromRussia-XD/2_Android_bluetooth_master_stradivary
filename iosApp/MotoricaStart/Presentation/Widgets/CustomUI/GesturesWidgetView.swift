//
//  GesturesWidgetView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 28.10.2025.
//

import SwiftUI
import UIKit
//import UniformTypeIdentifiers


struct GesturesWidgetView: View {
    
    // MARK: - Dependencies
    @ObservedObject var provider: GesturesProvider
    @State private var highlightOffsetX: CGFloat = 0

    var onSegmentChange: (GesturesProvider.Segment) -> Void
    var onFactoryGestureTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onCustomGestureTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onCustomGestureSettingsTap: (GesturesProvider.GestureDisplayItem) -> Void
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
//            withAnimation(.easeOut(duration: 0.3)) {
                highlightOffsetX = newOffset
//            }
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
//                        onRemove: { onRotationGestureRemove(index) }
//                    )
//                }
                RotationGesturesReorderView(
                    items: $provider.rotationGroup,
                    onRemove: {
                        index in onRotationGestureRemove(index)
                        print("onRemove \(index)")
                    },
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
    @State private var dragOffset: CGSize = .zero
    @State private var reorderOffset: CGFloat = 0
    @State private var dragged = false
    @State private var itemFrames: [Int: CGRect] = [:]
    @State private var previewWidth: CGFloat = .zero

    private let activationDuration: TimeInterval = 0.001

    var body: some View {
        VStack(spacing: 0) {
            ForEach(items) { item in
                RotationGestureRow(
                    title: item.title,
                    isDragging: draggedItem == item,
                    isLast: item == items.last,
                    onRemove: {
                        if let index = items.firstIndex(of: item) {
                            onRemove(index)
                        }
                    },
                    handle: {
                        Image(systemName: "line.3.horizontal")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Color("ubi4_deactivate_text"))
                            .padding(.vertical, 12)
                            .contentShape(Rectangle())          // расширяем зону тапа хэндла
                            .gesture(longPressDragGesture(for: item))
                    }
                )
                .frame(maxWidth: .infinity)
                .contentShape(Rectangle())
                .background(frameReader(for: item))
                .offset(y: offset(for: item))
                .onAppear { print("→ appear \(item.id)") }
                .zIndex(draggedItem == item ? 1 : 0)
                
//                preview: do {
//                    RotationGestureDragPreview(
//                        title: item.title,
//                        subtitle: item.subtitle
//                    )
//                    .frame(width: previewWidth == .zero ? nil : previewWidth)
//                }
            }
            
        }
//        .background(Color("ubi4_gray"))  //цвет фона
        .coordinateSpace(name: "rotationList")
        .onPreferenceChange(RotationGestureRowFramePreferenceKey.self) { value in
            DispatchQueue.main.async {
                itemFrames = value
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color("ubi4_gray_border"), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
    }
    
    private func longPressDragGesture(for item: GesturesProvider.GestureDisplayItem) -> some Gesture {
        DragGesture(minimumDistance: 0, coordinateSpace: .named("rotationList"))
            .onChanged { drag in
                guard let itemFrame = itemFrames[item.id] else { return }

                if draggedItem == nil {
                    let handleActivationMinX = itemFrame.maxX - handleActivationWidth
                    guard drag.startLocation.x >= handleActivationMinX else { return }

                    // При первом движении инициализируем "захват"
                    draggedItem = item
                    dragOffset = .zero

                    if draggedItem == item {
                        print("✅ [Drag started] for \(item)")
                    }
                }

                if draggedItem == item {
                    withTransaction(Transaction(animation: nil)) {
                        dragOffset = drag.translation
                        updateOrder(with: drag)
                    }
                }
            }
            .onEnded { _ in
                guard draggedItem == item else { return }
                print("🏁 [DRAG ended] releasing \(item)")
                withAnimation(.spring(response: 0.5, dampingFraction: 0.7, blendDuration: 0.5)) { dragOffset = .zero }
                draggedItem = nil
            }
    }
    
    /// Ширина активной зоны справа, откуда можно начать перетаскивание.
    private var handleActivationWidth: CGFloat { 56 }

    private func frameReader(for item: GesturesProvider.GestureDisplayItem) -> some View {
        GeometryReader { proxy in
            Color.clear.preference(
                key: RotationGestureRowFramePreferenceKey.self,
                value: [item.id: proxy.frame(in: .named("rotationList"))]
            )
        }
        .allowsHitTesting(false)
        .onChange(of: itemFrames) { _ in
            DispatchQueue.main.async {
                itemFrames = itemFrames
            }
        }
    }

    private func offset(for item: GesturesProvider.GestureDisplayItem) -> CGFloat {
        if draggedItem == item {
            print(String(format: "↕️ offset(for id=%d) = %.2f", item.id, dragOffset.height))
            return dragOffset.height
        } else {
            return 0
        }
    }

    private func updateOrder(with drag: DragGesture.Value) {
        guard let draggedItem,
              let currentFrame = itemFrames[draggedItem.id],
              let currentIndex = items.firstIndex(of: draggedItem) else { return }
        print(String(format: "🟢 [FINGER] y=%.2f  dragOffset=%.2f", drag.location.y, drag.translation.height))
        if let f = itemFrames[draggedItem.id] {
            print(String(format: "📐 draggedItem id=%d  frame.minY=%.2f  midY=%.2f  maxY=%.2f", draggedItem.id, f.minY, f.midY, f.maxY))
        }

//        let currentMidY = currentFrame.midY + drag.translation.height
//        let fingerY = drag.location.y
        let currentMidY = drag.location.y
        print("2️⃣ Вычисление новой позиции пальца \(currentMidY) = \(currentFrame.midY) + \(drag.translation.height)")
//        let fingerY = drag.location.y
//        print(String(format: "🟢 [FINGER] y=%.2f (drag.location.y, real finger pos)", fingerY))

        let orderedItems = items.compactMap { item -> (GesturesProvider.GestureDisplayItem, CGRect)? in
            guard let frame = itemFrames[item.id] else { return nil }
            return (item, frame)
        }

        guard orderedItems.count == items.count else { return }
        
        let sortedItems = orderedItems.sorted { $0.1.minY < $1.1.minY }
        
        guard let currentPosition = sortedItems.firstIndex(where: { $0.0 == draggedItem }) else { return }
        
        var itemsWithoutDragged = sortedItems
        itemsWithoutDragged.remove(at: currentPosition)

        let destinationPosition = itemsWithoutDragged.firstIndex { currentMidY < $0.1.midY } ?? itemsWithoutDragged.count

        var updatedItems = items
        let element = updatedItems.remove(at: currentIndex)

        let targetIndex: Int
        if destinationPosition == itemsWithoutDragged.count {
            targetIndex = updatedItems.count
        } else {
            let destinationItem = itemsWithoutDragged[destinationPosition].0
            guard let destinationIndex = updatedItems.firstIndex(of: destinationItem) else { return }
            targetIndex = destinationIndex
        }
        print(String(format: "📦 currentIndex=%d → targetIndex=%d  destinationPos=%d", currentIndex, targetIndex, destinationPosition))

        guard targetIndex != currentIndex else { return }

        // ⚙️ Исправление: корректируем dragOffset на изменение layout
        // считаем вертикальное смещение между старыми и новыми фреймами
        let previousMidY = currentFrame.midY
        let clampedIndex = max(0, min(targetIndex, updatedItems.count))
        updatedItems.insert(element, at: clampedIndex)
        
//        withAnimation(.interactiveSpring(response: 0.35, dampingFraction: 0.85, blendDuration: 0.3)) {
////            var updatedItems = items
////            let element = updatedItems.remove(at: currentIndex)
////            let clampedIndex = max(0, min(targetIndex, updatedItems.count))
////            updatedItems.insert(element, at: clampedIndex)
////            items = updatedItems
////            onReorder(updatedItems)
//            updatedItems.insert(element, at: targetIndex)
//            items = updatedItems
//            onReorder(updatedItems)
//        }

        print("📋 items before reorder:", items.map(\.id))
        print("📋 items after reorder:", updatedItems.map(\.id))
        var transaction = Transaction()
        transaction.disablesAnimations = true

        withTransaction(transaction) {
            items = updatedItems
        }
        
        // После перестановки SwiftUI пересчитает frame.
        // Чтобы элемент остался под пальцем — пересчитаем dragOffset.
        DispatchQueue.main.async {
            if let newFrame = itemFrames[draggedItem.id] {
                let deltaY = newFrame.midY - previousMidY
                dragOffset.height -= deltaY
                print(String(format: "🧮 Коррекция dragOffset: deltaY=%.2f → dragOffset=%.2f", deltaY, dragOffset.height))
            }
        }

        onReorder(updatedItems)
    }
}

private struct RotationGestureDragPreview: View {
    let title: String

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 12, weight: .light))
                    .foregroundColor(.white)
            }

            Spacer()

            Image(systemName: "trash")
                .foregroundColor(Color("ubi4_no_system_red"))
                .padding(.trailing, 16)

            Image(systemName: "line.3.horizontal")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Color("ubi4_deactivate_text"))
                .padding(.vertical, 12)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
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
}

private struct RotationGesturesWidthPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = .zero

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct RotationGestureRowFramePreferenceKey: PreferenceKey {
    static var defaultValue: [Int: CGRect] = [:]
    static func reduce(value: inout [Int: CGRect], nextValue: () -> [Int: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { $1 })
    }
}

private struct RotationGestureRow<Handle: View>: View {
    let title: String
    var isDragging: Bool
    var isLast: Bool
    var onRemove: () -> Void
    let handle: Handle

    init(
        title: String,
        isDragging: Bool,
        isLast: Bool,
        onRemove: @escaping () -> Void,
        @ViewBuilder handle: () -> Handle
    ) {
        self.title = title
        self.isDragging = isDragging
        self.isLast = isLast
        self.onRemove = onRemove
        self.handle = handle()
    }
    
    private var backgroundColor: Color {
//        isDragging ? Color("ubi4_gray").opacity(0.8) : Color("ubi4_gray")
        isDragging ? Color("ubi4_gray").opacity(1) : .clear
    }
    
    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 12, weight: .light))
                    .foregroundColor(.white)
            }
            Spacer()
            Button(action: onRemove) {
                Image(systemName: "trash")
                    .foregroundColor(Color("ubi4_no_system_red"))
                    .padding(.trailing, 16)
            }
            handle
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .background(backgroundColor)
        .overlay(alignment: .bottom) {
            if !isLast {
                Rectangle()
                    .fill(Color("ubi4_gray_border"))
                    .frame(height: 1)
            }
        }
        .animation(.easeInOut(duration: 0.3), value: isDragging)
        .frame(maxWidth: .infinity, alignment: .leading)
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
