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
    @State private var highlightOffsetX: CGFloat = 0
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
    var onFactoryGestureTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onCustomGestureTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onCustomGestureSettingsTap: (GesturesProvider.GestureDisplayItem) -> Void
    var onRotationGestureRemove: (Int) -> Void
    var onRotationGestureAdd: ([GesturesProvider.GestureDisplayItem]) -> Void
    var onRotationGesturesReorder: ([GesturesProvider.GestureDisplayItem]) -> Void
    var onSprGestureAction: (GesturesProvider.SprGestureDisplayItem) -> Void
    var onSprAddTap: () -> Void

    
    // MARK: - Body
    var body: some View {
        ZStack {
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
        .fullScreenCover(isPresented: $isRotationDeleteDialogPresented) {
            RotationDeleteDialogOverlay(
                isVisible: $isRotationDeleteDialogVisible,
                title: NSLocalizedString("rotation_delete_dialog_title", comment: ""),
                message: $rotationDeleteDialogMessage,
                deleteTitle: NSLocalizedString("dialog_delete", comment: ""), //SharedRes.strings().delete.desc().localized(),
                cancelTitle: NSLocalizedString("dialog_cancel", comment: ""),
                onDelete: handleRotationDeleteConfirm,
                onCancel: dismissRotationDeleteDialog
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
    }

    private struct ClearFullScreenBackgroundView: UIViewRepresentable {
        func makeUIView(context: Context) -> UIView {
            let view = UIView()
            view.backgroundColor = .clear

            DispatchQueue.main.async {
                view.superview?.superview?.backgroundColor = .clear
                view.superview?.backgroundColor = .clear
            }

            return view
        }

        func updateUIView(_ uiView: UIView, context: Context) { }
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
                updateHighlightOffset(segmentWidth: segmentWidth)
            }
        }
        .transaction { transaction in
            transaction.animation = nil
        }
        .frame(height: 48)
    }
    
    private func updateHighlightOffset(segmentWidth: CGFloat) {
        let index = GesturesProvider.Segment.allCases.firstIndex(of: provider.selectedSegment) ?? 0
        let newOffset = CGFloat(index) * segmentWidth
        highlightOffsetX = newOffset
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
        isRotationGroupAddGesturesDialogPresented = true
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
            isRotationGroupAddGesturesDialogPresented = false
            rotationGroupAddGesturesDialogDismissWorkItem = nil
        }
        rotationGroupAddGesturesDialogDismissWorkItem = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + animationDuration,
            execute: workItem
        )
    }

    private func handleRotationDialogSave() {
        let selected = rotationDialogOptions.filter { rotationGroupAddGesturesDialogSelection.contains($0.id) }
        let gestures = selected.map { $0.item }
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
        isRotationDeleteDialogPresented = true
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
            isRotationDeleteDialogPresented = false
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
                        .aspectRatio(1, contentMode: .fit)
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
        isSprGesturesDialogPresented = true
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
            isSprGesturesDialogPresented = false
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
        provider.sprGestures = selected.map {
            GesturesProvider.SprGestureDisplayItem(
                id: $0.id,
                title: $0.title,
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
        isSprBindingDialogPresented = true
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
            isSprBindingDialogPresented = false
            sprBindingDialogTarget = nil
            sprBindingDialogDismissWorkItem = nil
        }
        sprBindingDialogDismissWorkItem = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + animationDuration,
            execute: workItem
        )
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
                    .padding(.top, 8)
                    .padding(.bottom, 16)
            }
            Spacer(minLength: 0)
        }
        .padding([.leading, .bottom], 12)
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

private struct SprGestureSelectionOption: Identifiable, Hashable {
    let id: Int
    let title: String
}

private enum SprGesturesCatalog {
    static let all: [SprGestureSelectionOption] = [
        .init(id: 1, title: SharedRes.strings().thumb_finger.desc().localized()),
        .init(id: 2, title: SharedRes.strings().flexion.desc().localized()),
        .init(id: 3, title: SharedRes.strings().extension.desc().localized()),
        .init(id: 4, title: SharedRes.strings().palm_closing.desc().localized()),
        .init(id: 5, title: SharedRes.strings().palm_opening.desc().localized()),
        .init(id: 6, title: SharedRes.strings().ok_pinch.desc().localized()),
        .init(id: 7, title: SharedRes.strings().pistol_pointer_gesture.desc().localized()),
        .init(id: 8, title: SharedRes.strings().gesture_key.desc().localized()),
        .init(id: 9, title: SharedRes.strings().adduction.desc().localized()),
        .init(id: 10, title: SharedRes.strings().abduction.desc().localized()),
        .init(id: 11, title: SharedRes.strings().pronation.desc().localized()),
        .init(id: 12, title: SharedRes.strings().supination.desc().localized())
    ]
}

private var AnimationLoadedNameKey: UInt8 = 0
extension LottieAnimationView {
    var loadedAnimationName: String? {
        get { objc_getAssociatedObject(self, &AnimationLoadedNameKey) as? String }
        set { objc_setAssociatedObject(self, &AnimationLoadedNameKey, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }
}
