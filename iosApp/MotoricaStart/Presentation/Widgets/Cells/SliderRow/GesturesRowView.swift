//
//  GesturesRowView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 29.10.2025.
//

import SwiftUI

final class GesturesProvider: ObservableObject {
    struct GestureDisplayItem: Identifiable, Hashable {
        let id: Int
        let title: String
        var subtitle: String?
    }

    struct SprGestureDisplayItem: Identifiable, Hashable {
        let id: Int
        let title: String
        var subtitle: String?
    }

    enum Segment: CaseIterable {
        case collection
        case rotationGroup
        case sprGroup

        var title: String {
            switch self {
            case .collection:
                return NSLocalizedString("Collection", comment: "")
            case .rotationGroup:
                return NSLocalizedString("Rotation", comment: "")
            case .sprGroup:
                return "SPR"
            }
        }
    }

    @Published var selectedSegment: Segment = .collection
    @Published var isFactoryExpanded: Bool = true
    @Published var activeGestureId: Int?
    @Published var activeGestureTitle: String?
    @Published var factoryGestures: [GestureDisplayItem]
    @Published var customGestures: [GestureDisplayItem]
    @Published var rotationGroup: [GestureDisplayItem]
    @Published var sprGestures: [SprGestureDisplayItem]

    init(
        factoryGestures: [GestureDisplayItem],
        customGestures: [GestureDisplayItem],
        rotationGroup: [GestureDisplayItem],
        sprGestures: [SprGestureDisplayItem],
        activeGestureId: Int?,
        activeGestureTitle: String?
    ) {
        self.factoryGestures = factoryGestures
        self.customGestures = customGestures
        self.rotationGroup = rotationGroup
        self.sprGestures = sprGestures
        self.activeGestureId = activeGestureId
        self.activeGestureTitle = activeGestureTitle
    }
}

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
                    .fill(Color("ubi4_active"))
                    .frame(width: segmentWidth - 8, height: 40)
                    .offset(x: segmentOffset(width: segmentWidth), y: 4)
                    .animation(.easeInOut(duration: 0.25), value: provider.selectedSegment)
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
                        .fill(Color("ubi4_active"))
                        .shadow(color: Color.black.opacity(0.24), radius: 3, x: 0, y: 2)
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
            .toggleStyle(SwitchToggleStyle(tint: Color("ubi4_active")))

            if provider.isFactoryExpanded {
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                    ForEach(provider.factoryGestures) { gesture in
                        GestureCard(
                            title: gesture.title,
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
            }

            VStack(spacing: 10) {
                ForEach(Array(provider.rotationGroup.enumerated()), id: \.element.id) { index, gesture in
                    RotationGestureRow(
                        title: gesture.title,
                        onMoveUp: { onRotationGestureMoveUp?(index) },
                        onMoveDown: { onRotationGestureMoveDown?(index) },
                        onRemove: { onRotationGestureRemove?(index) }
                    )
                }
            }

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
                        .shadow(color: Color.black.opacity(0.2), radius: 2, x: 0, y: 2)
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
                        .shadow(color: Color.black.opacity(0.2), radius: 2, x: 0, y: 2)
                )
            }

            Text(NSLocalizedString("You can configure gestures in the SPR group to control the prosthesis sensors or buttons.", comment: ""))
                .font(.custom("SFProDisplay-Light", size: 12))
                .foregroundColor(Color("ubi4_deactivate_text"))
                .multilineTextAlignment(.leading)
        }
    }
}

private struct GestureCard: View {
    let title: String
    var isActive: Bool
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.custom("SFProDisplay-Light", size: 12))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding()
            .frame(maxWidth: .infinity, minHeight: 110, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(isActive ? Color("ubi4_active") : Color("ubi4_gray"))
                    .shadow(color: Color.black.opacity(0.2), radius: 3, x: 0, y: 2)
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
                .fill(isActive ? Color("ubi4_active") : Color("ubi4_gray"))
                .shadow(color: Color.black.opacity(0.2), radius: 2, x: 0, y: 2)
        )
    }
}

private struct RotationGestureRow: View {
    let title: String
    var onMoveUp: () -> Void
    var onMoveDown: () -> Void
    var onRemove: () -> Void

    var body: some View {
        HStack {
            Text(title)
                .font(.custom("SFProDisplay-Light", size: 12))
                .foregroundColor(.white)
            Spacer()
            Button(action: onMoveUp) {
                Image(systemName: "chevron.up")
                    .foregroundColor(.white)
            }
            Button(action: onMoveDown) {
                Image(systemName: "chevron.down")
                    .foregroundColor(.white)
            }
            Button(action: onRemove) {
                Image(systemName: "trash")
                    .foregroundColor(Color("ubi4_active"))
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color("ubi4_gray"))
                .shadow(color: Color.black.opacity(0.2), radius: 2, x: 0, y: 2)
        )
    }
}
