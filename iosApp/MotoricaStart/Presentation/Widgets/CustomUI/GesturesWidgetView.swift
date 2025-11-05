//
//  GesturesWidgetView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 28.10.2025.
//

import SwiftUI
import UIKit

struct GesturesWidgetView: View {

    // MARK: - Dependencies

    @ObservedObject var provider: GesturesProvider

    var onSegmentChange: (GesturesProvider.Segment) -> Void
    var onFactoryGestureTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onCustomGestureTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onCustomGestureSettingsTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onRotationGestureMoveUp: (Int) -> Void
    var onRotationGestureMoveDown: (Int) -> Void
    var onRotationGestureRemove: (Int) -> Void
    var onRotationGestureAdd: () -> Void
    var onSprGestureAction: (GesturesProvider.SprGestureDisplayItem) -> Void
    var onSprAddTap: () -> Void

    // MARK: - Body

    var body: some View {
        VStack(spacing: 16) {
            segmentSelector
//            activeGestureView
//
//            switch provider.selectedSegment {
//            case .collection:
//                collectionView
//            case .rotationGroup:
//                rotationGroupView
//            case .sprGroup:
//                sprGroupView
//            }
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
        //TODO: раскомментить после эксперимента с отступами
//        .padding(.vertical, 4)
        .padding(.horizontal, 8)
        .background(Color("ubi4_back"))
    }

    // MARK: - Segment Selector

    private var segmentSelector: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let segmentCount = CGFloat(GesturesProvider.Segment.allCases.count)
            let segmentWidth = (width - 0) / segmentCount // -4
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color("ubi4_gray"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                    )
                    .shadow(color: .black.opacity(0.15), radius: 3, x: 0, y: 1)

                RoundedRectangle(cornerRadius: 10)
                    .fill(Color("ubi4_back"))
                    .padding(1)
                    .frame(width: segmentWidth)
                    .offset(x: highlightOffset(width: segmentWidth))

                HStack(spacing: 0) {
                    ForEach(Array(GesturesProvider.Segment.allCases.enumerated()), id: \.offset) { index, segment in
                        Button(action: { select(segment: segment) }) {
                            Text(segment.title)
                                .font(.system(size: 12, weight: .light))
                                .foregroundColor(segment == provider.selectedSegment ? .white : Color("ubi4_deactivate_text"))
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(2)
            }
        }
        .frame(height: 48)
    }

    private func highlightOffset(width: CGFloat) -> CGFloat {
        guard let index = GesturesProvider.Segment.allCases.firstIndex(of: provider.selectedSegment) else { return 0 }
        return CGFloat(index) * width
    }

    private func select(segment: GesturesProvider.Segment) {
        guard provider.selectedSegment != segment else { return }
        provider.selectedSegment = segment
//        withAnimation { provider.selectedSegment = segment }
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
                .shadow(color: .black.opacity(0.15), radius: 3, x: 0, y: 1)
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
//        withAnimation { provider.isFactoryExpanded.toggle() }
    }

    // MARK: - Rotation Group View

    private var rotationGroupView: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(NSLocalizedString("rotation_group", comment: ""))
                .font(.system(size: 12, weight: .light))
                .foregroundColor(.white)

            VStack(spacing: 12) {
                ForEach(Array(provider.rotationGroup.enumerated()), id: \.offset) { index, item in
                    RotationGestureRow(
                        title: item.title,
                        subtitle: item.subtitle,
                        onMoveUp: { onRotationGestureMoveUp(index) },
                        onMoveDown: { onRotationGestureMoveDown(index) },
                        onRemove: { onRotationGestureRemove(index) }
                    )
                }
                Button(action: onRotationGestureAdd) {
                    Label(NSLocalizedString("add_gesture", comment: ""), systemImage: "plus")
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

    // MARK: - Components

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
                }
                .buttonStyle(.plain)
            }

            if isExpanded {
                content()
            }
        }
//        .animation(nil, value: isExpanded)
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
                    .shadow(color: .black.opacity(0.15), radius: 3, x: 0, y: 1)
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
                        .shadow(color: .black.opacity(0.15), radius: 3, x: 0, y: 1)
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
                            .shadow(color: .black.opacity(0.2), radius: 3, x: 0, y: 1)
                    )
                    .padding(.trailing, 8)
            }
            .buttonStyle(.plain)
        }
    }
}

private struct RotationGestureRow: View {
    let title: String
    let subtitle: String?
    var onMoveUp: () -> Void
    var onMoveDown: () -> Void
    var onRemove: () -> Void

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
            controlButton(systemName: "chevron.up", action: onMoveUp)
            controlButton(systemName: "chevron.down", action: onMoveDown)
            controlButton(systemName: "trash", action: onRemove)
        }
        .padding(.horizontal, 12)
//        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color("ubi4_gray"))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.15), radius: 3, x: 0, y: 1)
        )
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
                    .shadow(color: .black.opacity(0.15), radius: 3, x: 0, y: 1)
            )
        }
        .buttonStyle(.plain)
    }
}
