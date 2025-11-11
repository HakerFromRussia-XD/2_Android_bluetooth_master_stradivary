//
//  GesturesRowView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.10.2025.
//

import SwiftUI
import Combine
import UIKit
import UniformTypeIdentifiers

struct GesturesRowView: View {
    @ObservedObject var provider: GesturesProvider

    var onSegmentChange: ((GesturesProvider.Segment) -> Void)?
    var onFactoryGestureTap: ((GesturesProvider.GestureDisplayItem) -> Void)?
    var onCustomGestureTap: ((GesturesProvider.GestureDisplayItem) -> Void)?
    var onCustomGestureSettingsTap: ((GesturesProvider.GestureDisplayItem) -> Void)?
    var onRotationGestureMoveUp: ((Int) -> Void)?
    var onRotationGestureMoveDown: ((Int) -> Void)?
    var onRotationGestureRemove: ((Int) -> Void)?
    var onRotationGestureAdd: (() -> Void)?
    var onRotationGesturesReorder: (([GesturesProvider.GestureDisplayItem]) -> Void)?
    var onSprGestureAction: ((GesturesProvider.SprGestureDisplayItem) -> Void)?
    var onSprAddTap: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            segmentSelector
            activeGestureSection
            contentSection
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 16)
        .background(Color("ubi4_back"))
    }

    private var segmentSelector: some View {
        ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 12)
                .fill(Color("ubi4_gray"))
                .frame(height: 48)
                .shadow(color: Color.black.opacity(0.25), radius: 3, x: 0, y: 2)

            GeometryReader { geometry in
                let segmentWidth = geometry.size.width / CGFloat(GesturesProvider.Segment.allCases.count)
                RoundedRectangle(cornerRadius: 10)
                    .fill(Color("ubi4_no_system_red"))
                    .frame(width: segmentWidth - 8, height: 40)
                    .offset(x: segmentOffset(width: segmentWidth), y: 4)
                    .animation(nil, value: provider.selectedSegment)
            }
            .allowsHitTesting(false)

            HStack(spacing: 0) {
                ForEach(Array(GesturesProvider.Segment.allCases.enumerated()), id: \.offset) { index, segment in
                    Button {
                        guard provider.selectedSegment != segment else { return }
                        provider.selectedSegment = segment
                        onSegmentChange?(segment)
                    } label: {
                        Text(segmentTitle(for: segment))
                            .font(.custom("SFProDisplay-Light", size: 12))
                            .foregroundColor(textColor(for: segment))
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                    }
                }
            }
        }
        .frame(height: 48)
    }

    private func segmentTitle(for segment: GesturesProvider.Segment) -> String {
        switch segment {
        case .collection:
            return NSLocalizedString("Collection of gestures", comment: "")
        case .rotationGroup:
            return NSLocalizedString("Rotation group", comment: "")
        case .sprGroup:
            return NSLocalizedString("SPR gestures", comment: "")
        }
    }

    private func textColor(for segment: GesturesProvider.Segment) -> Color {
        provider.selectedSegment == segment ? Color.white : Color("ubi4_deactivate_text")
    }

    private func segmentOffset(width: CGFloat) -> CGFloat {
        let index: CGFloat
        switch provider.selectedSegment {
        case .collection: index = 0
        case .rotationGroup: index = 1
        case .sprGroup: index = 2
        }
        return index * width + 4
    }

    private var activeGestureSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(NSLocalizedString("Active gesture", comment: ""))
                .font(.custom("SFProDisplay-Light", size: 12))
                .foregroundColor(Color.white.opacity(0.7))
            Text(provider.activeGestureTitle ?? NSLocalizedString("Not selected", comment: ""))
                .font(.custom("SFProDisplay-Light", size: 14))
                .foregroundColor(Color.white)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color("ubi4_no_system_red"))
                        .shadow(color: Color.black.opacity(0.25), radius: 3, x: 0, y: 2)
                )
        }
    }

    @ViewBuilder
    private var contentSection: some View {
        switch provider.selectedSegment {
        case .collection:
            collectionView
        case .rotationGroup:
            rotationGroupView
        case .sprGroup:
            sprGroupView
        }
    }

    private var collectionView: some View {
        VStack(alignment: .leading, spacing: 12) {
            Toggle(isOn: Binding(
                get: { provider.isFactoryExpanded },
                set: { provider.isFactoryExpanded = $0 }
            )) {
                Text(NSLocalizedString("Show default collection", comment: ""))
                    .font(.custom("SFProDisplay-Light", size: 12))
                    .foregroundColor(.white)
            }
            .toggleStyle(SwitchToggleStyle(tint: Color("ubi4_no_system_red")))

            if provider.isFactoryExpanded {
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                    ForEach(provider.factoryGestures) { gesture in
                        GestureCard(
                            title: gesture.title,
                            image: gesture.image,
                            isActive: provider.activeGestureId == gesture.id
                        ) {
                            onFactoryGestureTap?(gesture)
                        }
                    }
                }
            }

            VStack(spacing: 12) {
                ForEach(provider.customGestures) { gesture in
                    GestureRow(
                        title: gesture.title,
                        subtitle: gesture.subtitle,
                        isActive: provider.activeGestureId == gesture.id,
                        onTap: { onCustomGestureTap?(gesture) },
                        onSettings: { onCustomGestureSettingsTap?(gesture) }
                    )
                }
            }
        }
    }

    private var rotationGroupView: some View {
        VStack(alignment: .leading, spacing: 12) {
            if provider.rotationGroup.isEmpty {
                Text(NSLocalizedString("Select gestures for the rotation group", comment: ""))
                    .font(.custom("SFProDisplay-Light", size: 12))
                    .foregroundColor(Color("ubi4_deactivate_text"))
                    .multilineTextAlignment(.leading)
            } else {
                RotationGesturesReorderView(
                    items: $provider.rotationGroup,
                    onRemove: { index in
                        onRotationGestureRemove?(index)
                    },
                    onReorder: { items in
                        onRotationGesturesReorder?(items)
                    }
                )
            }
//            VStack(spacing: 10) {
//                ForEach(Array(provider.rotationGroup.enumerated()), id: \.element.id) { index, gesture in
//                    RotationGestureRow(
//                        title: gesture.title,
//                        onRemove: { onRotationGestureRemove?(index) }
//                    )
//                }
//            }

            Button(action: { onRotationGestureAdd?() }) {
                HStack {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .bold))
                    Text(NSLocalizedString("Add gesture", comment: ""))
                        .font(.custom("SFProDisplay-Light", size: 12))
                }
                .foregroundColor(.white)
                .padding()
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color("ubi4_gray"))
                        .shadow(color: Color.black.opacity(0.25), radius: 3, x: 0, y: 2)
                )
            }
        }
    }

    private var sprGroupView: some View {
        VStack(alignment: .leading, spacing: 12) {
            if provider.sprGestures.isEmpty {
                Text(NSLocalizedString("No SPR gestures selected", comment: ""))
                    .font(.custom("SFProDisplay-Light", size: 12))
                    .foregroundColor(Color("ubi4_deactivate_text"))
            } else {
                VStack(spacing: 10) {
                    ForEach(provider.sprGestures) { gesture in
                        GestureRow(
                            title: gesture.title,
                            subtitle: gesture.subtitle,
                            isActive: false,
                            onTap: { onSprGestureAction?(gesture) },
                            onSettings: nil
                        )
                    }
                }
            }

            Button(action: { onSprAddTap?() }) {
                HStack {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .bold))
                    Text(NSLocalizedString("Choose control gestures", comment: ""))
                        .font(.custom("SFProDisplay-Light", size: 12))
                }
                .foregroundColor(.white)
                .padding()
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color("ubi4_gray"))
                        .shadow(color: Color.black.opacity(0.25), radius: 3, x: 0, y: 2)
                )
            }

            Text(NSLocalizedString("You can configure gestures in the SPR group to control the prosthesis sensors or buttons.", comment: ""))
                .font(.custom("SFProDisplay-Light", size: 12))
                .foregroundColor(Color("ubi4_deactivate_text"))
                .multilineTextAlignment(.leading)
        }
    }
}

private struct RotationGesturesReorderView: View {
    @Binding var items: [GesturesProvider.GestureDisplayItem]
    var onRemove: (Int) -> Void
    var onReorder: ([GesturesProvider.GestureDisplayItem]) -> Void

    @State private var draggedItem: GesturesProvider.GestureDisplayItem?

    var body: some View {
        VStack(spacing: 0) {
            ForEach(items) { item in
                RotationGestureRow(
                    title: item.title,
                    subtitle: item.subtitle,
                    isDragging: draggedItem == item,
                    isLast: item == items.last,
                    onRemove: {
                        if let index = items.firstIndex(of: item) {
                            onRemove(index)
                        }
                    }
                )
                .frame(maxWidth: .infinity)
                .contentShape(Rectangle())
                .onDrag {
                    draggedItem = item
                    return NSItemProvider(object: NSString(string: "rotation-\(item.id)"))
                } preview: {
                    RotationGestureRow(
                        title: item.title,
                        subtitle: item.subtitle,
                        isDragging: true,
                        isLast: false,
                        onRemove: {}
                    )
                    .frame(maxWidth: .infinity)
                    .fixedSize(horizontal: true, vertical: true)
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
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color("ubi4_gray"))
        )
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color("ubi4_gray_border"), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.25), radius: 3, x: 0, y: 2)
    }
}

private struct GestureCard: View {
    let title: String
    var image: UIImage?
    var isActive: Bool
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 0) {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(maxWidth: .infinity)
                }
                
                Text(title)
                    .font(.custom("SFProDisplay-Light", size: 12))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding()
            .frame(maxWidth: .infinity, minHeight: 110, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color("ubi4_gray"))
                    .shadow(color: Color.black.opacity(0.25), radius: 3, x: 0, y: 2)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isActive ? Color("ubi4_active") : Color.clear, lineWidth: 1)
            )
        }
    }
}

private struct GestureRow: View {
    let title: String
    var subtitle: String?
    var isActive: Bool
    var onTap: () -> Void
    var onSettings: (() -> Void)?

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.custom("SFProDisplay-Light", size: 12))
                    .foregroundColor(.white)
                if let subtitle {
                    Text(subtitle)
                        .font(.custom("SFProDisplay-Light", size: 10))
                        .foregroundColor(Color("ubi4_deactivate_text"))
                }
            }
            Spacer()
            if let onSettings {
                Button(action: onSettings) {
                    Image(systemName: "gearshape")
                        .foregroundColor(.white)
                }
                .padding(.trailing, 8)
            }
            Button(action: onTap) {
                Image(systemName: "hand.tap")
                    .foregroundColor(.white)
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color("ubi4_gray"))
                .shadow(color: Color.black.opacity(0.5), radius: 3, x: 0, y: 2)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(isActive ? Color("ubi4_active") : Color.clear, lineWidth: 1)
        )
    }
}

private struct RotationGestureRow: View {
    let title: String
    let subtitle: String?
    var isDragging: Bool
    var isLast: Bool
    var onRemove: () -> Void

    private var backgroundColor: Color {
        isDragging ? Color("ubi4_gray").opacity(0.8) : .clear
    }
    
    var body: some View {
//        HStack {
//            Text(title)
//                .font(.custom("SFProDisplay-Light", size: 12))
//                .foregroundColor(.white)
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.custom("SFProDisplay-Light", size: 12))
                    .foregroundColor(.white)
                if let subtitle, subtitle.isEmpty == false {
                    Text(subtitle)
                        .font(.custom("SFProDisplay-Light", size: 10))
                        .foregroundColor(Color("ubi4_deactivate_text"))
                }
            }
            Spacer()
            Button(action: onRemove) {
                Image(systemName: "trash")
                    .foregroundColor(Color("ubi4_no_system_red"))
            }
            Image(systemName: "line.3.horizontal")
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(Color("ubi4_deactivate_text"))
                .padding(.vertical, 12)
        }
//        .padding()
//        .frame(maxWidth: .infinity)
//        .background(
//            RoundedRectangle(cornerRadius: 12)
//                .fill(Color("ubi4_gray"))
//                .shadow(color: Color.black.opacity(0.25), radius: 3, x: 0, y: 2)
//        )
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
