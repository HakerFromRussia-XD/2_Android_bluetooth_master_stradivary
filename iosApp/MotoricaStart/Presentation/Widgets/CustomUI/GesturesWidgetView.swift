//
//  GesturesWidgetView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 28.10.2025.
//

import SwiftUI
import UIKit
import shared
import Lottie


struct GesturesWidgetView: View {
    
    // MARK: - Dependencies
    @ObservedObject var provider: GesturesProvider
    let visibleSegments: [GesturesProvider.Segment]
    @State private var selectorAccessibilityOffset: CGFloat = 0
    @State private var selectorDisplayOffset: CGFloat = 0
    @State private var selectorSegmentWidth: CGFloat = 0
    @State private var isSelectorOffsetInitialized = false
    @State private var selectorAnimationTimer: Timer?
    @State private var selectorAnimationMaxStep: CGFloat = 0
    @State private var selectorAnimationStepCount: Int = 0
    @State private var selectorAnimationRollbackDetected = false
    @State private var selectorPreviousAnimationOffset: CGFloat = 0
    @State private var selectorAnimationDirection: CGFloat = 0
    @State private var segmentContentLockedHeight: CGFloat = 0
    @State private var cachedSegmentHeights: [GesturesProvider.Segment: CGFloat] = [:]
    // rotation group
    @State private var isRotationGroupAddGesturesDialogPresented = false
    @State private var isRotationGroupAddGesturesDialogVisible = false
    @State private var rotationGroupAddGesturesDialogDismissWorkItem: DispatchWorkItem?
    @State private var rotationGroupAddGesturesDialogSelection: Set<Int> = []
    @State private var rotationGroupAddGesturesDialogError: String? = nil
    @State private var isRotationDeleteDialogPresented = false
    @State private var isRotationDeleteDialogVisible = false
    @State private var rotationDeleteDialogDismissWorkItem: DispatchWorkItem?
    @State private var rotationDeleteDialogItem: GesturesProvider.GestureDisplayItem?
    @State private var rotationDeleteDialogMessage: String = ""
    // spppr gestures
    @State private var isSprGesturesDialogPresented = false
    @State private var isSprGesturesDialogVisible = false
    @State private var sprGesturesDialogDismissWorkItem: DispatchWorkItem?
    @State private var sprGesturesDialogSelection: Set<Int> = []
    // spppr bindings
    @State private var isSprBindingDialogPresented = false
    @State private var isSprBindingDialogVisible = false
    @State private var sprBindingDialogDismissWorkItem: DispatchWorkItem?
    @State private var sprBindingDialogSelection: Set<Int> = []
    @State private var sprBindingDialogTarget: GesturesProvider.SprGestureDisplayItem?
    
    var animationDuration: Double { 0.3 }
    var onSegmentChange: (GesturesProvider.Segment) -> Void
    var onActiveGestureRequest: () -> Void
    var onFactoryGestureTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onCustomGestureTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onCustomGestureSettingsTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onRotationGestureRemove: (Int) -> Void
    var onRotationGestureAdd: ([GesturesProvider.GestureDisplayItem]) -> Void
    var onRotationGesturesReorder: ([GesturesProvider.GestureDisplayItem]) -> Void
    var onSprGestureAction: (GesturesProvider.SprGestureDisplayItem) -> Void
    var onSprAddTap: () -> Void

    init(
        provider: GesturesProvider,
        visibleSegments: [GesturesProvider.Segment] = GesturesProvider.Segment.allCases,
        onSegmentChange: @escaping (GesturesProvider.Segment) -> Void,
        onActiveGestureRequest: @escaping () -> Void,
        onFactoryGestureTap: @escaping (GesturesProvider.GestureDisplayItem) -> Void,
        onCustomGestureTap: @escaping (GesturesProvider.GestureDisplayItem) -> Void,
        onCustomGestureSettingsTap: @escaping (GesturesProvider.GestureDisplayItem) -> Void,
        onRotationGestureRemove: @escaping (Int) -> Void,
        onRotationGestureAdd: @escaping ([GesturesProvider.GestureDisplayItem]) -> Void,
        onRotationGesturesReorder: @escaping ([GesturesProvider.GestureDisplayItem]) -> Void,
        onSprGestureAction: @escaping (GesturesProvider.SprGestureDisplayItem) -> Void,
        onSprAddTap: @escaping () -> Void
    ) {
        self.provider = provider
        self.visibleSegments = visibleSegments.isEmpty ? [.collection] : visibleSegments
        self.onSegmentChange = onSegmentChange
        self.onActiveGestureRequest = onActiveGestureRequest
        self.onFactoryGestureTap = onFactoryGestureTap
        self.onCustomGestureTap = onCustomGestureTap
        self.onCustomGestureSettingsTap = onCustomGestureSettingsTap
        self.onRotationGestureRemove = onRotationGestureRemove
        self.onRotationGestureAdd = onRotationGestureAdd
        self.onRotationGesturesReorder = onRotationGesturesReorder
        self.onSprGestureAction = onSprGestureAction
        self.onSprAddTap = onSprAddTap
    }
    
    
    // MARK: - Body
    var body: some View {
        VStack(spacing: 16) {
            segmentSelector
            activeGestureView
            stableSegmentContentView
                .accessibilityIdentifier(AccessibilityIdentifier.gesturesSegmentContentContainer)
                .accessibilityValue(segmentAccessibilityValue(for: provider.selectedSegment))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .animation(nil, value: provider.selectedSegment)
        .transaction { transaction in
            transaction.animation = nil
        }
        .padding(.horizontal, 8)
        .background(Color("ubi4_back"))
        .onAppear {
            if !visibleSegments.contains(provider.selectedSegment) {
                provider.selectedSegment = visibleSegments.first ?? .collection
            }
            applyCachedSegmentHeightIfAvailable(for: provider.selectedSegment)
            onSegmentChange(provider.selectedSegment)
            onActiveGestureRequest()
        }
        .onChange(of: provider.selectedSegment) { segment in
            applyCachedSegmentHeightIfAvailable(for: segment)
        }
        .onDisappear {
            selectorAnimationTimer?.invalidate()
            selectorAnimationTimer = nil
        }
        .fullScreenCover(isPresented: $isRotationGroupAddGesturesDialogPresented) {
            RotationGroupAddGesturesDialogOverlay(
                isVisible: $isRotationGroupAddGesturesDialogVisible,
                title: NSLocalizedString("rotation_dialog_title", comment: ""),
                saveTitle: NSLocalizedString("dialog_save", comment: ""),
                cancelTitle: NSLocalizedString("dialog_cancel", comment: ""),
                options: rotationDialogOptions,
                selection: $rotationGroupAddGesturesDialogSelection,
                errorMessage: rotationGroupAddGesturesDialogError,
                onOptionTap: { option in
                    toggleRotationDialogSelection(option: option)
                },
                onSave: handleRotationDialogSave,
                onCancel: dismissRotationGroupAddGesturesDialog
            )
            .interactiveDismissDisabled()
            .background(ClearFullScreenBackgroundView())
        }
        .fullScreenCover(isPresented: $isSprGesturesDialogPresented) {
            SprGesturesDialogOverlay(
                isVisible: $isSprGesturesDialogVisible,
                title: NSLocalizedString("spr_dialog_title", comment: ""),
                saveTitle: NSLocalizedString("dialog_save", comment: ""),
                cancelTitle: NSLocalizedString("dialog_cancel", comment: ""),
                options: sprDialogOptions,
                selection: $sprGesturesDialogSelection,
                onOptionTap: toggleSprDialogSelection,
                onSave: handleSprDialogSave,
                onCancel: dismissSprGesturesDialog
            )
            .interactiveDismissDisabled()
            .background(ClearFullScreenBackgroundView())
        }
        .fullScreenCover(isPresented: $isSprBindingDialogPresented) {
            RotationGroupAddGesturesDialogOverlay(
                isVisible: $isSprBindingDialogVisible,
                title: SharedRes.strings().assign_gesture.desc().localized(),
                saveTitle: NSLocalizedString("dialog_save", comment: ""),
                cancelTitle: NSLocalizedString("dialog_cancel", comment: ""),
                options: rotationDialogOptions,
                selection: $sprBindingDialogSelection,
                errorMessage: nil,
                onOptionTap: toggleSprBindingSelection,
                onSave: handleSprBindingDialogSave,
                onCancel: dismissSprBindingDialog
            )
            .interactiveDismissDisabled()
            .background(ClearFullScreenBackgroundView())
        }
        .fullScreenCover(isPresented: $isRotationDeleteDialogPresented) {
            RotationDeleteDialogOverlay(
                isVisible: $isRotationDeleteDialogVisible,
                title: NSLocalizedString("rotation_delete_dialog_title", comment: ""),
                message: $rotationDeleteDialogMessage,
                deleteTitle: NSLocalizedString("dialog_delete", comment: ""),
                cancelTitle: NSLocalizedString("dialog_cancel", comment: ""),
                onDelete: handleRotationDeleteConfirm,
                onCancel: dismissRotationDeleteDialog
            )
            .interactiveDismissDisabled()
            .background(ClearFullScreenBackgroundView())
        }
    }

    private struct ClearFullScreenBackgroundView: UIViewRepresentable {
        final class ClearBackgroundHostView: UIView {
            override func didMoveToWindow() {
                super.didMoveToWindow()
                applyClearBackgrounds()
                DispatchQueue.main.async { [weak self] in
                    self?.applyClearBackgrounds()
                }
            }

            private func applyClearBackgrounds() {
                backgroundColor = .clear
                isOpaque = false

                var current: UIView? = self
                while let view = current {
                    view.backgroundColor = .clear
                    view.isOpaque = false
                    current = view.superview
                }
            }
        }

        func makeUIView(context: Context) -> UIView {
            ClearBackgroundHostView()
        }

        func updateUIView(_ uiView: UIView, context: Context) { }
    }

    // MARK: - Segment Selector
    private var segmentSelector: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let segmentCount = CGFloat(max(visibleSegments.count, 1))
            let segmentWidth = width / segmentCount
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
                    .offset(x: selectorDisplayOffset)

                HStack(spacing: 0) {
                    ForEach(visibleSegments, id: \.self) { segment in
                        Button(action: { select(segment: segment) }) {
                            Text(segment.title)
                                .font(.system(size: 12, weight: .light))
                                .foregroundColor(segment == provider.selectedSegment ? .white : Color("ubi4_deactivate_text"))
                                .animation(nil, value: provider.selectedSegment)
                                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
                                .contentShape(Rectangle())
                                .accessibilityIdentifier(accessibilityIdentifier(for: segment))
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .contentShape(Rectangle())
                        .accessibilityIdentifier(accessibilityIdentifier(for: segment))
                        .animation(nil, value: provider.selectedSegment)
                        .buttonStyle(.plain)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(2)
            }
            .overlay(
                Color.clear
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("gestures.segment.selector")
                    .accessibilityIdentifier(AccessibilityIdentifier.gesturesSegmentSelector)
                    .accessibilityValue(segmentSelectorAccessibilityValue(segmentWidth: segmentWidth))
                    .allowsHitTesting(false)
            )
            .onAppear {
                initializeSelectorOffsetIfNeeded(segmentWidth: segmentWidth)
            }
            .onChange(of: segmentWidth) { newValue in
                updateSelectorOffsetForSegmentWidth(newValue)
            }
            .onChange(of: provider.selectedSegment) { _ in
                guard segmentWidth > 0 else { return }
                let targetOffset = segmentHighlightOffset(segmentWidth: segmentWidth)
                if isSelectorOffsetInitialized {
                    animateSelectorOffset(to: targetOffset)
                } else {
                    setSelectorOffsetImmediate(targetOffset)
                }
            }
        }
        .frame(height: 48)
    }
    
    @ViewBuilder
    private func segmentContent(for segment: GesturesProvider.Segment) -> some View {
        switch segment {
        case .collection:
            collectionView
        case .rotationGroup:
            rotationGroupView
        case .sprGroup:
            sprGroupView
        }
    }

    private var segmentContentView: some View {
        segmentContent(for: provider.selectedSegment)
    }

    private var shouldPremeasureCollectionHeight: Bool {
        visibleSegments.contains(.collection)
        && provider.selectedSegment != .collection
        && (cachedSegmentHeights[.collection] ?? 0) <= 0.5
    }

    private var shouldPremeasureRotationHeight: Bool {
        visibleSegments.contains(.rotationGroup)
        && provider.selectedSegment != .rotationGroup
        && (cachedSegmentHeights[.rotationGroup] ?? 0) <= 0.5
    }

    private var stableSegmentContentView: some View {
        segmentContentView
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .onPreferenceChange(SegmentContentHeightPreferenceKey.self) { measuredHeight in
                let normalizedHeight = normalizedHeightForLayout(measuredHeight)
                updateSegmentContentHeight(
                    normalizedHeight,
                    for: provider.selectedSegment
                )
            }
            .overlay(alignment: .topLeading) {
                ZStack(alignment: .topLeading) {
                    hiddenCurrentSegmentMeasurementView
                    if shouldPremeasureCollectionHeight {
                        segmentContent(for: .collection)
                            .fixedSize(horizontal: false, vertical: true)
                            .frame(maxWidth: .infinity, alignment: .topLeading)
                            .opacity(0.001)
                            .allowsHitTesting(false)
                            .accessibilityHidden(true)
                            .background(
                                GeometryReader { proxy in
                                    Color.clear.preference(
                                        key: CollectionSegmentPremeasureHeightPreferenceKey.self,
                                        value: proxy.size.height
                                    )
                                }
                            )
                    }
                    if shouldPremeasureRotationHeight {
                        segmentContent(for: .rotationGroup)
                            .fixedSize(horizontal: false, vertical: true)
                            .frame(maxWidth: .infinity, alignment: .topLeading)
                            .opacity(0.001)
                            .allowsHitTesting(false)
                            .accessibilityHidden(true)
                            .background(
                                GeometryReader { proxy in
                                    Color.clear.preference(
                                        key: RotationSegmentPremeasureHeightPreferenceKey.self,
                                        value: proxy.size.height
                                    )
                                }
                            )
                    }
                }
            }
            .onPreferenceChange(CollectionSegmentPremeasureHeightPreferenceKey.self) { measuredHeight in
                let normalizedHeight = normalizedHeightForLayout(measuredHeight)
                updateSegmentContentHeight(normalizedHeight, for: .collection)
            }
            .onPreferenceChange(RotationSegmentPremeasureHeightPreferenceKey.self) { measuredHeight in
                let normalizedHeight = normalizedHeightForLayout(measuredHeight)
                updateSegmentContentHeight(normalizedHeight, for: .rotationGroup)
            }
            .frame(minHeight: segmentContentLockedHeight, alignment: .top)
    }

    private var hiddenCurrentSegmentMeasurementView: some View {
        segmentContent(for: provider.selectedSegment)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .opacity(0.001)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
            .background(
                GeometryReader { proxy in
                    Color.clear.preference(
                        key: SegmentContentHeightPreferenceKey.self,
                        value: proxy.size.height
                    )
                }
            )
    }

    private func normalizedHeightForLayout(_ measuredHeight: CGFloat) -> CGFloat {
        let clamped = max(0, measuredHeight)
        let scale = UIScreen.main.scale
        guard scale > 0 else { return clamped }
        return (clamped * scale).rounded() / scale
    }

    private func updateSegmentContentHeight(_ measuredHeight: CGFloat, for segment: GesturesProvider.Segment) {
        guard measuredHeight.isFinite else { return }
        if let cached = cachedSegmentHeights[segment],
           abs(measuredHeight - cached) <= 0.5 {
            if segment == provider.selectedSegment {
                applyLockedHeightIfNeeded(measuredHeight)
            }
            return
        }
        cachedSegmentHeights[segment] = measuredHeight
        if segment == provider.selectedSegment {
            applyLockedHeightIfNeeded(measuredHeight)
        }
    }

    private func applyCachedSegmentHeightIfAvailable(for segment: GesturesProvider.Segment) {
        guard let cachedHeight = cachedSegmentHeights[segment], cachedHeight.isFinite else { return }
        applyLockedHeightIfNeeded(cachedHeight)
    }

    private func applyLockedHeightIfNeeded(_ height: CGFloat) {
        let previousHeight = segmentContentLockedHeight
        guard abs(height - previousHeight) > 0.5 else { return }
        var transaction = Transaction(animation: nil)
        transaction.disablesAnimations = true
        withTransaction(transaction) {
            segmentContentLockedHeight = height
        }
    }

    private func segmentHighlightOffset(segmentWidth: CGFloat) -> CGFloat {
        let index = visibleSegments.firstIndex(of: provider.selectedSegment) ?? 0
        return CGFloat(index) * segmentWidth
    }

    private func accessibilityIdentifier(for segment: GesturesProvider.Segment) -> String {
        switch segment {
        case .collection:
            return AccessibilityIdentifier.gesturesSegmentCollectionButton
        case .rotationGroup:
            return AccessibilityIdentifier.gesturesSegmentRotationButton
        case .sprGroup:
            return "AccessibilityIdentifierGesturesSegmentSprButton"
        }
    }

    private func segmentAccessibilityValue(for segment: GesturesProvider.Segment) -> String {
        switch segment {
        case .collection:
            return "collection"
        case .rotationGroup:
            return "rotation"
        case .sprGroup:
            return "spr"
        }
    }

    private func segmentSelectorAccessibilityValue(segmentWidth: CGFloat) -> String {
        let fallbackOffset = segmentHighlightOffset(segmentWidth: segmentWidth)
        let reportedOffset = selectorAccessibilityOffset.isFinite ? selectorAccessibilityOffset : fallbackOffset
        return String(
            format: "segment=%@;offset=%.3f;maxStep=%.3f;steps=%d;rollback=%@",
            segmentAccessibilityValue(for: provider.selectedSegment),
            reportedOffset,
            selectorAnimationMaxStep,
            selectorAnimationStepCount,
            selectorAnimationRollbackDetected ? "true" : "false"
        )
    }
    
    private func select(segment: GesturesProvider.Segment) {
        guard provider.selectedSegment != segment else { return }
        UIView.performWithoutAnimation {
            provider.selectedSegment = segment
        }
        onSegmentChange(segment)
    }

    private func initializeSelectorOffsetIfNeeded(segmentWidth: CGFloat) {
        guard segmentWidth > 0 else { return }
        selectorSegmentWidth = segmentWidth
        let targetOffset = segmentHighlightOffset(segmentWidth: segmentWidth)
        guard !isSelectorOffsetInitialized else {
            if selectorAnimationTimer == nil {
                setSelectorOffsetImmediate(targetOffset)
            }
            return
        }
        setSelectorOffsetImmediate(targetOffset)
    }

    private func updateSelectorOffsetForSegmentWidth(_ segmentWidth: CGFloat) {
        guard segmentWidth > 0 else { return }
        selectorSegmentWidth = segmentWidth
        let targetOffset = segmentHighlightOffset(segmentWidth: segmentWidth)
        guard isSelectorOffsetInitialized else {
            setSelectorOffsetImmediate(targetOffset)
            return
        }
        if selectorAnimationTimer == nil {
            setSelectorOffsetImmediate(targetOffset)
        } else {
            animateSelectorOffset(to: targetOffset)
        }
    }

    private func setSelectorOffsetImmediate(_ offset: CGFloat) {
        isSelectorOffsetInitialized = true
        selectorDisplayOffset = offset
        selectorAccessibilityOffset = offset
        selectorPreviousAnimationOffset = offset
    }

    private func animateSelectorOffset(to targetOffset: CGFloat) {
        let clampedTarget = targetOffset.isFinite ? targetOffset : 0
        let startOffset = selectorDisplayOffset
        let delta = clampedTarget - startOffset

        if abs(delta) < 0.5 {
            selectorAnimationMaxStep = 0
            selectorAnimationStepCount = 0
            selectorAnimationRollbackDetected = false
            selectorAnimationDirection = 0
            setSelectorOffsetImmediate(clampedTarget)
            return
        }

        selectorAnimationTimer?.invalidate()
        selectorAnimationMaxStep = 0
        selectorAnimationStepCount = 0
        selectorAnimationRollbackDetected = false
        selectorAnimationDirection = delta >= 0 ? 1 : -1
        selectorPreviousAnimationOffset = startOffset
        let startTime = CACurrentMediaTime()
        let duration = animationDuration
        let timer = Timer(timeInterval: 1.0 / 60.0, repeats: true) { timer in
            let elapsed = CACurrentMediaTime() - startTime
            let progress = min(max(elapsed / duration, 0), 1)
            let easedProgress = easeInOut(progress: progress)
            let nextOffset = startOffset + (delta * easedProgress)

            let step = nextOffset - selectorPreviousAnimationOffset
            if abs(step) > 0.001 {
                selectorAnimationStepCount += 1
                selectorAnimationMaxStep = max(selectorAnimationMaxStep, abs(step))
                if selectorAnimationDirection > 0, step < -0.8 {
                    selectorAnimationRollbackDetected = true
                } else if selectorAnimationDirection < 0, step > 0.8 {
                    selectorAnimationRollbackDetected = true
                }
                selectorPreviousAnimationOffset = nextOffset
            }

            selectorDisplayOffset = nextOffset
            selectorAccessibilityOffset = nextOffset

            if progress >= 1 {
                timer.invalidate()
                selectorAnimationTimer = nil
                selectorPreviousAnimationOffset = clampedTarget
                setSelectorOffsetImmediate(clampedTarget)
            }
        }
        RunLoop.main.add(timer, forMode: .common)
        selectorAnimationTimer = timer
    }

    private func easeInOut(progress: Double) -> CGFloat {
        let easedValue: Double
        if progress < 0.5 {
            easedValue = 2 * progress * progress
        } else {
            easedValue = 1 - pow(-2 * progress + 2, 2) / 2
        }
        return CGFloat(easedValue)
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
            if provider.rotationGroup.isEmpty {
                rotationGroupEmptyState
            } else {
                rotationGroupContent
            }
        }
    }
    
    private var rotationGroupContent: some View {
        VStack(spacing: 12) {
            RotationGesturesReorderView(
                items: $provider.rotationGroup,
                onRemove: { index in
                    presentRotationDeleteDialog(for: index)
                    print("onRemove 1 \(index)")
                },
                onReorder: { items in
                    onRotationGesturesReorder(items)
                }
            )

            if provider.rotationGroup.count < RotationGroupAddGesturesDialog.Constants.maxGestures {
                rotationGroupAddButton
            }
        }
    }
    
    private var rotationGroupAddButton: some View {
        Button(action: presentRotationGroupAddGesturesDialog) {
            Label(NSLocalizedString("add_gesture", comment: ""), systemImage: "plus")
                .font(.system(size: 12, weight: .light))
                .foregroundColor(.white)
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
    
    private var rotationGroupEmptyState: some View {
        VStack(spacing: 20) {
            VStack(spacing: 8) {
                Button(action: presentRotationGroupAddGesturesDialog) {
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
                
                Image("ic_long_arrow")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 60)
                    .offset(x: -100)
                
                Text(NSLocalizedString("rotation_group_tap_hint", comment: ""))
                    .font(.custom("OpenSansRoman-Bold", size: 14))
                    .foregroundColor(Color("ubi4_deactivate_text"))
                    .multilineTextAlignment(.center)
                    .alignmentGuide(.leading) { d in d[HorizontalAlignment.center] }
                    .offset(x: -130)
            }
            VStack(spacing: 8) {
                Text(NSLocalizedString("rotation_group_hint", comment: ""))
                    .font(.custom("SFProText-Bold", size: 14))
                    .foregroundColor(Color("ubi4_deactivate_text"))
                    .multilineTextAlignment(.center)

                Text(NSLocalizedString("rotation_group_switch_hint", comment: ""))
                    .font(.custom("SFProText-Bold", size: 14))
                    .foregroundColor(Color("ubi4_deactivate_text"))
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)

            Image(uiImage: SharedRes.images().annotationman.toUIImage()!)
                .resizable()
                .scaledToFit()
                .frame(width: 180)
        }
        .frame(maxWidth: .infinity)
    }

    private func presentRotationGroupAddGesturesDialog() {
        rotationGroupAddGesturesDialogSelection = Set(provider.rotationGroup.map { $0.id })
        rotationGroupAddGesturesDialogError = nil
        rotationGroupAddGesturesDialogDismissWorkItem?.cancel()
        isRotationGroupAddGesturesDialogVisible = false
        setDialogPresentedWithoutSystemAnimation {
            isRotationGroupAddGesturesDialogPresented = true
        }
        DispatchQueue.main.async {
            withAnimation(.easeInOut(duration: animationDuration)) {
                isRotationGroupAddGesturesDialogVisible = true
            }
        }
    }

    private func dismissRotationGroupAddGesturesDialog() {
        rotationGroupAddGesturesDialogError = nil
        guard isRotationGroupAddGesturesDialogPresented else { return }
        withAnimation(.easeInOut(duration: animationDuration)) {
            isRotationGroupAddGesturesDialogVisible = false
        }
        rotationGroupAddGesturesDialogDismissWorkItem?.cancel()
        let workItem = DispatchWorkItem {
            setDialogPresentedWithoutSystemAnimation {
                isRotationGroupAddGesturesDialogPresented = false
            }
            rotationGroupAddGesturesDialogDismissWorkItem = nil
        }
        rotationGroupAddGesturesDialogDismissWorkItem = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + animationDuration,
            execute: workItem
        )
    }

    private func handleRotationDialogSave() {
        let selectedIds = rotationGroupAddGesturesDialogSelection

        // Keep the current order for gestures that were already in the rotation group.
        var gestures = provider.rotationGroup.filter { selectedIds.contains($0.id) }

        // Append newly selected gestures in dialog order.
        let existingIds = Set(gestures.map { $0.id })
        let newGestures = rotationDialogOptions.compactMap { option -> GesturesProvider.GestureDisplayItem? in
            guard selectedIds.contains(option.id), existingIds.contains(option.id) == false else { return nil }
            return option.item
        }
        gestures.append(contentsOf: newGestures)
        
        onRotationGestureAdd(gestures)
        dismissRotationGroupAddGesturesDialog()
    }

    private func toggleRotationDialogSelection(option: RotationGroupAddGesturesSelectionOption) {
        if rotationGroupAddGesturesDialogSelection.contains(option.id) {
            rotationGroupAddGesturesDialogSelection.remove(option.id)
            rotationGroupAddGesturesDialogError = nil
            return
        }

        let maxCount = RotationGroupAddGesturesDialog.Constants.maxGestures
        if rotationGroupAddGesturesDialogSelection.count >= maxCount {
            rotationGroupAddGesturesDialogError = NSLocalizedString("rotation_dialog_limit_message", comment: "")
            return
        }

        rotationGroupAddGesturesDialogSelection.insert(option.id)
        rotationGroupAddGesturesDialogError = nil
    }

    private var rotationDialogOptions: [RotationGroupAddGesturesSelectionOption] {
        let factory = provider.factoryGestures.map {
            RotationGroupAddGesturesSelectionOption(item: $0, type: .factory)
        }
        let custom = provider.customGestures.map {
            RotationGroupAddGesturesSelectionOption(item: $0, type: .custom)
        }
        return factory + custom
    }
    
    private func presentRotationDeleteDialog(for index: Int) {
        guard provider.rotationGroup.indices.contains(index) else { return }
        let item = provider.rotationGroup[index]
        rotationDeleteDialogItem = item
        let format = NSLocalizedString("rotation_delete_dialog_message", comment: "")
        rotationDeleteDialogMessage = String(format: format, item.title)
        print ("presentRotationDeleteDialog for \(index) \(rotationDeleteDialogMessage)")
        rotationDeleteDialogDismissWorkItem?.cancel()
        isRotationDeleteDialogVisible = false
        setDialogPresentedWithoutSystemAnimation {
            isRotationDeleteDialogPresented = true
        }
        DispatchQueue.main.async {
            withAnimation(.easeInOut(duration: animationDuration)) {
                isRotationDeleteDialogVisible = true
            }
        }
    }

    private func dismissRotationDeleteDialog() {
        guard isRotationDeleteDialogPresented else { return }
        withAnimation(.easeInOut(duration: animationDuration)) {
            isRotationDeleteDialogVisible = false
        }
        rotationDeleteDialogDismissWorkItem?.cancel()
        let workItem = DispatchWorkItem {
            setDialogPresentedWithoutSystemAnimation {
                isRotationDeleteDialogPresented = false
            }
            rotationDeleteDialogItem = nil
            rotationDeleteDialogDismissWorkItem = nil
        }
        rotationDeleteDialogDismissWorkItem = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + animationDuration,
            execute: workItem
        )
    }

    private func handleRotationDeleteConfirm() {
        guard let item = rotationDeleteDialogItem,
            let index = provider.rotationGroup.firstIndex(of: item) else {
            dismissRotationDeleteDialog()
            return
        }
        onRotationGestureRemove(index)
        dismissRotationDeleteDialog()
    }

    
    // MARK: - SPR Group View
    private var sprGroupView: some View {
        VStack(alignment: .leading, spacing: 16) {
            if provider.sprGestures.isEmpty {
                sprEmptyState
            } else {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 2), spacing: 12) {
                    ForEach(provider.sprGestures) { item in
                        SprGestureTile(
                            title: item.title,
                            gestureName: item.subtitle,
                            animationName: SprGestureAnimationMapper.animationName(for: item.id),
                            onDotsTap: { presentSprBindingDialog(for: item) }
                        )
                    }
                    .padding(.top, 4)
                }
                Button(action: presentSprGesturesDialog) {
                    Label(NSLocalizedString("control_gestures", comment: ""), systemImage: "plus")
                        .font(.system(size: 12, weight: .light))
                        .foregroundColor(.white)
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
        }
    }
    
    private var sprEmptyState: some View {
            VStack(spacing: 20) {
                VStack(spacing: 8) {
                    Button(action: presentSprGesturesDialog) {
                        Label(NSLocalizedString("control_gestures", comment: ""), systemImage: "plus")
                            .font(.system(size: 12, weight: .light))
                            .foregroundColor(.white)
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
                    
                    Image("ic_long_arrow")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 60)
                        .offset(x: -100)
                    
                    Text(NSLocalizedString("rotation_group_tap_hint", comment: ""))
                        .font(.custom("OpenSansRoman-Bold", size: 14))
                        .foregroundColor(Color("ubi4_deactivate_text"))
                        .multilineTextAlignment(.center)
                        .alignmentGuide(.leading) { d in d[HorizontalAlignment.center] }
                        .offset(x: -130)
                }
                VStack(spacing: 8) {
                    Text(NSLocalizedString("annotation_main_text", comment: ""))
                        .font(.custom("SFProText-Bold", size: 14))
                        .foregroundColor(Color("ubi4_deactivate_text"))
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)

                Image(uiImage: SharedRes.images().annotationman.toUIImage()!)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 180)
            }
            .frame(maxWidth: .infinity)
        }
    
    private func presentSprGesturesDialog() {
        sprGesturesDialogSelection = Set(provider.sprGestures.map { $0.id })
        sprGesturesDialogDismissWorkItem?.cancel()
        isSprGesturesDialogVisible = false
        setDialogPresentedWithoutSystemAnimation {
            isSprGesturesDialogPresented = true
        }
        DispatchQueue.main.async {
            withAnimation(.easeInOut(duration: animationDuration)) {
                isSprGesturesDialogVisible = true
            }
        }
    }

    private func dismissSprGesturesDialog() {
        guard isSprGesturesDialogPresented else { return }
        withAnimation(.easeInOut(duration: animationDuration)) {
            isSprGesturesDialogVisible = false
        }
        sprGesturesDialogDismissWorkItem?.cancel()
        let workItem = DispatchWorkItem { [self] in
            setDialogPresentedWithoutSystemAnimation {
                isSprGesturesDialogPresented = false
            }
            sprGesturesDialogDismissWorkItem = nil
        }
        sprGesturesDialogDismissWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + animationDuration, execute: workItem)
    }

    private func toggleSprDialogSelection(option: SprGestureSelectionOption) {
        if sprGesturesDialogSelection.contains(option.id) {
            sprGesturesDialogSelection.remove(option.id)
            return
        }

        sprGesturesDialogSelection.insert(option.id)
    }

    private func handleSprDialogSave() {
        let selected = sprDialogOptions.filter { sprGesturesDialogSelection.contains($0.id) }
        provider.sprGestures = selected.map { option in
            if let existingItem = provider.sprGestures.first(where: { $0.id == option.id }) {
                return existingItem
            }

            return GesturesProvider.SprGestureDisplayItem(
                id: option.id,
                title: option.title,
                subtitle: nil,
                boundGestureId: nil
            )
        }
        onSprAddTap()
        dismissSprGesturesDialog()
    }

    private func presentSprBindingDialog(for item: GesturesProvider.SprGestureDisplayItem) {
        sprBindingDialogTarget = item
        if let boundGestureId = item.boundGestureId {
            sprBindingDialogSelection = [boundGestureId]
        } else {
            sprBindingDialogSelection = []
        }
        sprBindingDialogDismissWorkItem?.cancel()
        isSprBindingDialogVisible = false
        setDialogPresentedWithoutSystemAnimation {
            isSprBindingDialogPresented = true
        }
        DispatchQueue.main.async {
            withAnimation(.easeInOut(duration: animationDuration)) {
                isSprBindingDialogVisible = true
            }
        }
    }

    private func dismissSprBindingDialog() {
        guard isSprBindingDialogPresented else { return }
        withAnimation(.easeInOut(duration: animationDuration)) {
            isSprBindingDialogVisible = false
        }
        sprBindingDialogDismissWorkItem?.cancel()
        let workItem = DispatchWorkItem {
            setDialogPresentedWithoutSystemAnimation {
                isSprBindingDialogPresented = false
            }
            sprBindingDialogTarget = nil
            sprBindingDialogDismissWorkItem = nil
        }
        sprBindingDialogDismissWorkItem = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + animationDuration,
            execute: workItem
        )
    }

    private func setDialogPresentedWithoutSystemAnimation(_ updates: () -> Void) {
        var transaction = Transaction(animation: nil)
        transaction.disablesAnimations = true
        withTransaction(transaction) {
            updates()
        }
    }

    private func toggleSprBindingSelection(option: RotationGroupAddGesturesSelectionOption) {
        if sprBindingDialogSelection.contains(option.id) {
            sprBindingDialogSelection.remove(option.id)
            return
        }

        sprBindingDialogSelection = [option.id]
    }

    private func handleSprBindingDialogSave() {
        guard let target = sprBindingDialogTarget,
              let selectedId = sprBindingDialogSelection.first,
              let selectedOption = rotationDialogOptions.first(where: { $0.id == selectedId }),
              let index = provider.sprGestures.firstIndex(where: { $0.id == target.id }) else {
            dismissSprBindingDialog()
            return
        }

        provider.sprGestures[index].subtitle = selectedOption.item.title
        provider.sprGestures[index].boundGestureId = selectedId
        onSprGestureAction(provider.sprGestures[index])
        dismissSprBindingDialog()
    }
    
    private var sprDialogOptions: [SprGestureSelectionOption] {
        SprGesturesCatalog.all
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
                    .font(.system(size: 16))
                    .foregroundColor(Color("ubi4_white"))
                    .padding(16)
                    .background(
                        Rectangle()
                            .fill(Color.white.opacity(0.001))
                    )
                    .padding(.trailing, 0)
            }
            .buttonStyle(.plain)
        }
    }
}

// MARK: - rotation group
private struct RotationGesturesReorderView: View {
    @Binding var items: [GesturesProvider.GestureDisplayItem]
    var onRemove: (Int) -> Void
    var onReorder: ([GesturesProvider.GestureDisplayItem]) -> Void

    @State private var draggedItem: GesturesProvider.GestureDisplayItem?
    @State private var dragOffset: CGSize = .zero
    @State private var cumulativeDragCorrection: CGFloat = 0
    @State private var dragged = false
    @State private var itemFrames: [Int: CGRect] = [:]
    @State private var previewWidth: CGFloat = .zero
    @StateObject private var haptic = HapticEngineUIKit()

    private let activationDuration: TimeInterval = 0.001
    private let releaseAnimation: Animation = .spring(response: 0.5, dampingFraction: 0.7, blendDuration: 0.5)

    var body: some View {
        ZStack {
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
                    .animation(animation(for: item), value: items.map(\.id))
                    .zIndex(draggedItem == item ? 1 : 0)
                }

            }
            .animation(draggedItem == nil ? releaseAnimation : nil, value: items)
            .background(Color("ubi4_gray"))  //цвет фона
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
    }
    
    private func longPressDragGesture(for item: GesturesProvider.GestureDisplayItem) -> some SwiftUI.Gesture {
        DragGesture(minimumDistance: 0, coordinateSpace: .named("rotationList"))
            .onChanged { drag in
                guard let itemFrame = itemFrames[item.id] else { return }

                if draggedItem == nil {
                    let handleActivationMinX = itemFrame.maxX - handleActivationWidth
                    guard drag.startLocation.x >= handleActivationMinX else { return }

                    /// При первом движении инициализируем "захват"
                    draggedItem = item
                    dragOffset = .zero
                    cumulativeDragCorrection = 0

                    if draggedItem == item {
                        print("✅ [Drag started] for \(item)")
                    }
                }

                if draggedItem == item {
                    withTransaction(Transaction(animation: nil)) {
                        updateOrder(with: drag)
                    }
                }
            }
            .onEnded { _ in
                guard draggedItem == item else { return }
                print("🏁 [DRAG ended] releasing \(item)")
                withAnimation(releaseAnimation) { dragOffset = .zero }
                draggedItem = nil
                cumulativeDragCorrection = 0
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
            return dragOffset.height
        } else {
            return 0
        }
    }

    private func updateOrder(with drag: DragGesture.Value) {
        guard let draggedItem,
              let currentFrame = itemFrames[draggedItem.id],
              let currentIndex = items.firstIndex(of: draggedItem) else { return }
        print(String(format: "0⃣ 🟢 [FINGER] y=%.2f  dragOffset=%.2f", drag.location.y, drag.translation.height))
        if let f = itemFrames[draggedItem.id] {
            print(String(format: "1⃣ 📐 draggedItem id=%d  frame.minY=%.2f  midY=%.2f  maxY=%.2f", draggedItem.id, f.minY, f.midY, f.maxY))
        }

        /// вычисление позиции пальца по Y
        let draggedMidY = currentFrame.midY + drag.translation.height - cumulativeDragCorrection

        /// вычисление какие элементы выше, а какие ниже перетаскиваемого. Удаление перетаскиваемого для лёгких расчётов
        let orderedItems = items.compactMap { item -> (GesturesProvider.GestureDisplayItem, CGRect)? in
            guard let frame = itemFrames[item.id] else { return nil }
            return (item, frame)
        }
        guard orderedItems.count == items.count else { return }
        let sortedItems = orderedItems.sorted { $0.1.minY < $1.1.minY }
        guard let currentPosition = sortedItems.firstIndex(where: { $0.0 == draggedItem }) else { return }
        var itemsWithoutDragged = sortedItems
        itemsWithoutDragged.remove(at: currentPosition)
        
        /// тут установка зоны для перещёлкивания порядка элементов
        let thresholdMultiplier: CGFloat = drag.translation.height >= 0 ? 0.30 : 0.80
        let destinationPosition = itemsWithoutDragged.firstIndex {
            let thresholdY = $0.1.minY + $0.1.height * thresholdMultiplier
            return draggedMidY < thresholdY
        } ?? itemsWithoutDragged.count
    
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
        print(String(format: "3⃣ 📦 currentIndex=%d → targetIndex=%d  destinationPos=%d", currentIndex, targetIndex, destinationPosition))

        let positionDelta = targetIndex - currentIndex
        let rowHeight = currentFrame.height
        let newCorrection = cumulativeDragCorrection + CGFloat(positionDelta) * rowHeight
        dragOffset = CGSize(width: drag.translation.width, height: drag.translation.height - newCorrection)
        cumulativeDragCorrection = newCorrection
        
        /// ⚙️ эта часть работает ахуенно
        guard targetIndex != currentIndex else { return }
        let clampedIndex = max(0, min(targetIndex, updatedItems.count))
        updatedItems.insert(element, at: clampedIndex)
        print("4⃣ 📋 items before reorder:", items.map(\.id))
        print("4⃣ 📋 items after reorder:", updatedItems.map(\.id))
        haptic.fire()
        withAnimation(.interactiveSpring(response: 0.35, dampingFraction: 0.85, blendDuration: 0.25)) {
            items = updatedItems
        }

        onReorder(updatedItems)
    }
    
    private func animation(for item: GesturesProvider.GestureDisplayItem) -> Animation? {
        guard draggedItem != item else { return nil }
        return .interactiveSpring(response: 0.35, dampingFraction: 0.85, blendDuration: 0.25)
    }
}

final class HapticEngineUIKit: ObservableObject {
    private let generator: UIImpactFeedbackGenerator

    init(style: UIImpactFeedbackGenerator.FeedbackStyle = .medium) {
        generator = UIImpactFeedbackGenerator(style: style)
        prepareGenerator()
    }
    
    func fire(intensity: CGFloat = 1.0) {
        guard Thread.isMainThread else {
            DispatchQueue.main.async { [self] in fire(intensity: intensity) }
            return
        }

        generator.impactOccurred(intensity: intensity)
        prepareGenerator()
    }

    private func prepareGenerator() {
        if Thread.isMainThread {
            generator.prepare()
        } else {
            DispatchQueue.main.async { [self] in generator.prepare() }
        }
    }
}

private struct RotationGesturesWidthPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = .zero

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct SegmentContentHeightPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = .zero

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct CollectionSegmentPremeasureHeightPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = .zero

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct RotationSegmentPremeasureHeightPreferenceKey: PreferenceKey {
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
        .background(
            Group {
                if isDragging {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color("ubi4_gray_border"))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color("ubi4_rotation_gray_border"), lineWidth: 1)
                        )
                        .shadow(color: .black.opacity(0.25), radius: 3, x: 0, y: 2)
                } else {
                    Color.clear
                }
            }
        )
        .overlay(alignment: .bottom) {
            if !isLast && !isDragging {
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

// MARK: - rotation group dialogs
private struct RotationGroupAddGesturesDialog: View {
    struct Constants {
        static let maxGestures = 8
    }

    let title: String
    let saveTitle: String
    let cancelTitle: String
    let options: [RotationGroupAddGesturesSelectionOption]
    @Binding var selection: Set<Int>
    let errorMessage: String?
    var onOptionTap: (RotationGroupAddGesturesSelectionOption) -> Void
    var onSave: () -> Void
    var onCancel: () -> Void

    var body: some View {
        VStack {
            Spacer()
            dialogContent
                .padding(.horizontal, 32)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .ignoresSafeArea()
    }

    @ViewBuilder
    private var dialogContent: some View {
        VStack(spacing: 20) {
            Text(title)
                .font(.custom("SFProText-Bold", size: 18))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.horizontal, 16)

            ScrollView {
                VStack(spacing: 0) {
                    ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                        if shouldShowDivider(before: index) {
                            Rectangle()
                                .fill(Color("ubi4_gray_border"))
                                .frame(height: 1)
                        }

                        Button(action: { onOptionTap(option) }) {
                            HStack {
                                Text(option.item.title)
                                    .font(.system(size: 14, weight: .regular))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                if selection.contains(option.id) {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 14, weight: .bold))
                                        .foregroundColor(Color("ubi4_active"))
                                }
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 16)
                            .frame(maxWidth: .infinity)
                            .background(Color.clear)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .frame(maxHeight: 320)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color("ubi4_gray"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                    )
            )
            .padding(.horizontal, 16)

            if let errorMessage {
                Text(errorMessage)
                    .font(.system(size: 12, weight: .regular))
                    .foregroundColor(Color("ubi4_no_system_red"))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
            }

            VStack(spacing: 0) {
                Rectangle()
                    .fill(Color("ubi4_gray_border"))
                    .frame(height: 1)

                Button(action: onSave) {
                    Text(saveTitle)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color("ubi4_yes_system_blue"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                Rectangle()
                    .fill(Color("ubi4_gray_border"))
                    .frame(height: 1)

                Button(action: onCancel) {
                    Text(cancelTitle)
                        .font(.system(size: 16, weight: .light))
                        .foregroundColor(Color("ubi4_yes_system_blue"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.top, 16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color("ubi4_back"))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.35), radius: 12, x: 0, y: 8)
        )
    }

    private func shouldShowDivider(before index: Int) -> Bool {
        guard index > 0 else { return false }
        return index != 0
    }
}

private struct RotationGroupAddGesturesSelectionOption: Identifiable, Hashable {
    enum SourceType: Equatable {
        case factory
        case custom
    }

    let item: GesturesProvider.GestureDisplayItem
    let type: SourceType

    var id: Int { item.id }
}

private struct RotationGroupAddGesturesDialogOverlay: View {
    @Binding var isVisible: Bool
    let title: String
    let saveTitle: String
    let cancelTitle: String
    let options: [RotationGroupAddGesturesSelectionOption]
    @Binding var selection: Set<Int>
    let errorMessage: String?
    var onOptionTap: (RotationGroupAddGesturesSelectionOption) -> Void
    var onSave: () -> Void
    var onCancel: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.65)
                .ignoresSafeArea()

            RotationGroupAddGesturesDialog(
                title: title,
                saveTitle: saveTitle,
                cancelTitle: cancelTitle,
                options: options,
                selection: $selection,
                errorMessage: errorMessage,
                onOptionTap: onOptionTap,
                onSave: onSave,
                onCancel: onCancel
            )
            .padding(.horizontal, 8)
        }
        .opacity(isVisible ? 1 : 0)
    }
}

private struct RotationDeleteDialogOverlay: View {
    @Binding var isVisible: Bool
    let title: String
    @Binding var message: String
    let deleteTitle: String
    let cancelTitle: String
    var onDelete: () -> Void
    var onCancel: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.65)
                .ignoresSafeArea()

            RotationDeleteGestureDialog(
                title: title,
                message: $message,
                deleteTitle: deleteTitle,
                cancelTitle: cancelTitle,
                onDelete: onDelete,
                onCancel: onCancel
            )
            .padding(.horizontal, 8)
        }
        .opacity(isVisible ? 1 : 0)
    }
}

private struct RotationDeleteGestureDialog: View {
    let title: String
    @Binding var message: String
    let deleteTitle: String
    let cancelTitle: String
    var onDelete: () -> Void
    var onCancel: () -> Void

    var body: some View {
        VStack {
            Spacer()
            dialogContent
                .padding(.horizontal, 32)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .ignoresSafeArea()
    }

    @ViewBuilder
    private var dialogContent: some View {
        VStack(spacing: 16) {
            Text(title)
                .font(.custom("SFProText-Bold", size: 18))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.horizontal, 16)

            Text(message)
                .font(.system(size: 12, weight: .regular))
                .foregroundColor(Color("ubi4_deactivate_text"))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 16)

            VStack(spacing: 0) {
                Rectangle()
                    .fill(Color("ubi4_gray_border"))
                    .frame(height: 1)

                Button(action: onDelete) {
                    Text(deleteTitle)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color("ubi4_no_system_red"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                Rectangle()
                    .fill(Color("ubi4_gray_border"))
                    .frame(height: 1)

                Button(action: onCancel) {
                    Text(cancelTitle)
                        .font(.system(size: 16, weight: .light))
                        .foregroundColor(Color("ubi4_yes_system_blue"))
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.top, 16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color("ubi4_back"))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.35), radius: 12, x: 0, y: 8)
        )
    }
}


// MARK: - spppr
private struct SprGestureTile: View {
    let title: String
    let gestureName: String?
    let animationName: String?
    var onDotsTap: () -> Void

    var body: some View {
        GeometryReader { geometry in
            VStack(alignment: .leading, spacing: -16) {
                HStack {
                    Text(title)
                        .font(.system(size: 12, weight: .light))
                        .foregroundColor(Color("ubi4_deactivate_text"))
                        .multilineTextAlignment(.leading)
                    Spacer()

                    Button(action: onDotsTap) {
                        Image(systemName: "ellipsis")
                            .foregroundColor(Color("ubi4_white"))
                            .frame(width: 48, height: 42)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }

                if let animationName {
                    SprGestureAnimationView(animationName: animationName)
                        .frame(width: 140, height: 140)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
                }

                if let gestureName, gestureName.isEmpty == false {
                    Text(gestureName)
                        .font(.custom("OpenSansRoman-Bold", size: 12))
                        .foregroundColor(Color("ubi4_white"))
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.top, 0)
                        .padding(.bottom, 16)
                }
                Spacer(minLength: 0)
            }

            .padding([.leading, .bottom], 12)
            .frame(width: geometry.size.width, height: geometry.size.width, alignment: .topLeading)
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
        .aspectRatio(1, contentMode: .fit)
    }
}

private struct SprGestureAnimationView: UIViewRepresentable {
    let animationName: String

    
    func makeUIView(context: Context) -> UIView {
        // 1) ОБЯЗАТЕЛЬНО создаём контейнер
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false

        // 2) создаём реальный LottieView
        let animationView = LottieAnimationView(configuration: .shared)
        animationView.translatesAutoresizingMaskIntoConstraints = false
        animationView.contentMode = .scaleAspectFit

        container.addSubview(animationView)

        // 3) фиксируем *размер контейнера*
        NSLayoutConstraint.activate([
            container.widthAnchor.constraint(equalToConstant: 140),
            container.heightAnchor.constraint(equalToConstant: 140),

            // 4) растягиваем анимацию НА контейнер
            animationView.topAnchor.constraint(equalTo: container.topAnchor),
            animationView.bottomAnchor.constraint(equalTo: container.bottomAnchor),
            animationView.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            animationView.trailingAnchor.constraint(equalTo: container.trailingAnchor),
        ])

        loadAnimation(into: animationView)
        return container
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {
        guard let animationView = uiView.subviews.first as? LottieAnimationView else { return }

        if animationView.loadedAnimationName != animationName {
            loadAnimation(into: animationView)
        }

        if !animationView.isAnimationPlaying {
            animationView.play()
        }
    }
    
    private func loadAnimation(into view: LottieAnimationView) {
        if Bundle.main.url(forResource: animationName, withExtension: "lottie") != nil {
            DotLottieFile.named(animationName, bundle: .main) { result in
                DispatchQueue.main.async {
                    switch result {
                    case .success(let file):
                        guard let animationContainer = file.animations.first else {
                            loadJsonAnimation(into: view)
                            return
                        }

                        view.animation = animationContainer.animation
                        if let provider = animationContainer.configuration.imageProvider {
                            view.imageProvider = provider
                        }
                        view.contentMode = .scaleAspectFill
                        view.loopMode = .loop
                        view.animationSpeed = CGFloat(animationContainer.configuration.speed)
                        view.play()
                    case .failure:
                        loadJsonAnimation(into: view)
                    }
                }
            }
        } else {
            loadJsonAnimation(into: view)
        }
    }

    private func loadJsonAnimation(into view: LottieAnimationView) {
        if let animation = LottieAnimation.named(animationName) {
            view.animation = animation
            view.imageProvider = BundleImageProvider(bundle: .main, searchPath: nil)
            view.loopMode = .loop
            view.animationSpeed = 1
            view.play()
        } else {
            view.stop()
            view.animation = nil
        }
    }
}

private enum SprGestureAnimationMapper {
    static func animationName(for id: Int) -> String? {
        switch id {
        case 1: return "thumb_fingers"
        case 2: return "wrist_flex"
        case 3: return "wrist_extend"
        case 4: return "close"
        case 5: return "open"
        case 6: return "pinch"
        case 7: return "indication"
        case 8: return "key"
        case 9: return "adduction"
        case 10: return "abduction"
        case 11: return "pronation"
        case 12: return "supination"
        default: return nil
        }
    }
}


// MARK: - spppr dialogs
private struct SprGesturesDialog: View {
    let title: String
    let saveTitle: String
    let cancelTitle: String
    let options: [SprGestureSelectionOption]
    @Binding var selection: Set<Int>
    var onOptionTap: (SprGestureSelectionOption) -> Void
    var onSave: () -> Void
    var onCancel: () -> Void

    var body: some View {
        VStack {
            Spacer()
            VStack(spacing: 20) {
                Text(title)
                    .font(.custom("SFProText-Bold", size: 18))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.horizontal, 16)

                ScrollView {
                    VStack(spacing: 0) {
                        ForEach(Array(options.enumerated()), id: \.offset) { index, option in
                            if index > 0 {
                                Rectangle()
                                    .fill(Color("ubi4_gray_border"))
                                    .frame(height: 1)
                            }

                            Button(action: { onOptionTap(option) }) {
                                HStack {
                                    Text(option.title)
                                        .font(.system(size: 14, weight: .regular))
                                        .foregroundColor(.white)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                    if selection.contains(option.id) {
                                        Image(systemName: "checkmark")
                                            .font(.system(size: 14, weight: .bold))
                                            .foregroundColor(Color("ubi4_active"))
                                    }
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 16)
                                .frame(maxWidth: .infinity)
                                .background(Color.clear)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .frame(maxHeight: 320)
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color("ubi4_gray"))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                        )
                )
                .padding(.horizontal, 16)

                VStack(spacing: 0) {
                    Rectangle()
                        .fill(Color("ubi4_gray_border"))
                        .frame(height: 1)

                    Button(action: onSave) {
                        Text(saveTitle)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(Color("ubi4_yes_system_blue"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)

                    Rectangle()
                        .fill(Color("ubi4_gray_border"))
                        .frame(height: 1)

                    Button(action: onCancel) {
                        Text(cancelTitle)
                            .font(.system(size: 16, weight: .light))
                            .foregroundColor(Color("ubi4_yes_system_blue"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.top, 16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color("ubi4_back"))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                    )
                    .shadow(color: .black.opacity(0.35), radius: 12, x: 0, y: 8)
            )
            .padding(.horizontal, 32)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .ignoresSafeArea()
    }
}

private struct SprGesturesDialogOverlay: View {
    @Binding var isVisible: Bool
    let title: String
    let saveTitle: String
    let cancelTitle: String
    let options: [SprGestureSelectionOption]
    @Binding var selection: Set<Int>
    var onOptionTap: (SprGestureSelectionOption) -> Void
    var onSave: () -> Void
    var onCancel: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.65)
                .ignoresSafeArea()

            SprGesturesDialog(
                title: title,
                saveTitle: saveTitle,
                cancelTitle: cancelTitle,
                options: options,
                selection: $selection,
                onOptionTap: onOptionTap,
                onSave: onSave,
                onCancel: onCancel
            )
            .padding(.horizontal, 8)
        }
        .opacity(isVisible ? 1 : 0)
    }
}

private var AnimationLoadedNameKey: UInt8 = 0
extension LottieAnimationView {
    var loadedAnimationName: String? {
        get { objc_getAssociatedObject(self, &AnimationLoadedNameKey) as? String }
        set { objc_setAssociatedObject(self, &AnimationLoadedNameKey, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }
}
