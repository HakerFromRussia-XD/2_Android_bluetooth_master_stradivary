//
//  FingersDelayDialog.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 26.01.2026.
//

import SwiftUI
import UIKit
import shared

@objcMembers
final class FingersDelayDialogPresenter: NSObject {
    @objc static func present(
        from viewController: UIViewController,
        title: String,
        subTitle: String,
        saveTitle: String,
        cancelTitle: String,
        delayValues: [NSNumber],
        onSave: @escaping ([NSNumber]) -> Void
    ) {
        let initialValues = delayValues.map { $0.floatValue }
        let dismiss = {
            viewController.dismiss(animated: false)
        }
        let dialogView = FingersDelayDialogOverlay(
            title: title,
            subTitle: subTitle,
            saveTitle: saveTitle,
            cancelTitle: cancelTitle,
            initialValues: initialValues,
            onSave: { values in
                onSave(values.map { NSNumber(value: $0) })
                dismiss()
            },
            onCancel: dismiss
        )
        let hostingController = UIHostingController(rootView: dialogView)
        hostingController.modalPresentationStyle = .overFullScreen
        hostingController.modalTransitionStyle = .crossDissolve
        hostingController.view.backgroundColor = .clear
        viewController.present(hostingController, animated: true)
    }
}

private struct FingersDelayDialogOverlay: View {
    let title: String
    let subTitle: String
    let saveTitle: String
    let cancelTitle: String
    let initialValues: [Float]
    var onSave: ([Float]) -> Void
    var onCancel: () -> Void
    @State private var isVisible = false

    private var animationDuration: Double { 0.3 }
    
    var body: some View {
        ZStack {
            Color.black.opacity(0.65)
                .ignoresSafeArea()

            FingersDelayDialog(
                title: title,
                subTitle: subTitle,
                saveTitle: saveTitle,
                cancelTitle: cancelTitle,
                initialValues: initialValues,
                onSave: { values in
                    performDismiss {
                        onSave(values)
                    }
                },
                onCancel: {
                    performDismiss(onCancel)
                }
            )
            .padding(.horizontal, 8)
        }
        .opacity(isVisible ? 1 : 0)
        .onAppear {
            withAnimation(.easeInOut(duration: animationDuration)) {
                isVisible = true
            }
        }
    }

    private func performDismiss(_ action: @escaping () -> Void) {
        withAnimation(.easeInOut(duration: animationDuration)) {
            isVisible = false
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + animationDuration) {
            action()
        }
    }
}

private struct FingersDelayDialog: View {
    let title: String
    let subTitle: String
    let saveTitle: String
    let cancelTitle: String
    let initialValues: [Float]
    var onSave: ([Float]) -> Void
    var onCancel: () -> Void

    @State private var values: [Float]
    private let rowHeight: CGFloat = 72
    private let separatorHeight: CGFloat = 1
    private let rowsCount: CGFloat = 6

    private var slidersBlockHeight: CGFloat {
        rowsCount * rowHeight + (rowsCount - 1) * separatorHeight
    }

    init(
        title: String,
        subTitle: String,
        saveTitle: String,
        cancelTitle: String,
        initialValues: [Float],
        onSave: @escaping ([Float]) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.title = title
        self.subTitle = subTitle
        self.saveTitle = saveTitle
        self.cancelTitle = cancelTitle
        self.initialValues = initialValues
        self.onSave = onSave
        self.onCancel = onCancel
        _values = State(initialValue: Array(initialValues.prefix(6)) + Array(repeating: 0, count: max(0, 6 - initialValues.count)))
    }

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

    private var dialogContent: some View {
        VStack(spacing: 20) {
            Text(title)
                .font(.custom("SFProText-Bold", size: 18))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.horizontal, 16)
            
            Text(subTitle)
                .font(.custom("SFProText-Bold", size: 14))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.horizontal, 16)

            VStack(spacing: 0) {
                VStack(spacing: 0) {
                    ForEach(0..<6, id: \.self) { index in
                        if index > 0 {
                            Rectangle()
                                .fill(Color("ubi4_gray_border"))
                                .frame(height: 1)
                        }

                        FingersDelaySliderRow(
                            title: fingerDelayTitles[safe: index] ?? "",
                            value: binding(for: index),
                            range: 0...100
                        )
                    }
                }
            }
            .frame(height: slidersBlockHeight)
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

                Button(action: { onSave(values) }) {
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

    private var fingerDelayTitles: [String] {
        [
            SharedRes.strings()._1_finger_delay.desc().localized(),
            SharedRes.strings()._2_finger_delay.desc().localized(),
            SharedRes.strings()._3_finger_delay.desc().localized(),
            SharedRes.strings()._4_finger_delay.desc().localized(),
            SharedRes.strings()._5_finger_delay.desc().localized(),
            SharedRes.strings()._6_finger_delay.desc().localized()
        ]
    }
    
    private func binding(for index: Int) -> Binding<Float> {
        Binding(
            get: { values[safe: index] ?? 0 },
            set: { newValue in
                guard values.indices.contains(index) else { return }
                values[index] = newValue
            }
        )
    }
}

@objcMembers
final class KmmLocalizedStrings: NSObject {
    static func delayStateTitle() -> String {
        SharedRes.strings().delay_state.desc().localized()
    }

    static func delayStateOpenDescription() -> String {
        SharedRes.strings().delay_state_open_description.desc().localized()
    }

    static func delayStateCloseDescription() -> String {
        SharedRes.strings().delay_state_close_description.desc().localized()
    }

    static func dialogSave() -> String {
        SharedRes.strings().save.desc().localized()
    }

    static func dialogCancel() -> String {
        SharedRes.strings().cancel.desc().localized()
    }
    
    static func measureType() -> String {
        SharedRes.strings().measure_ms.desc().localized()
    }
}

private struct FingersDelaySliderRow: View {
    let title: String
    @Binding var value: Float
    let range: ClosedRange<Float>

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(title)
                    .font(.custom("SFProDisplay-Light", size: 14))
                    .foregroundColor(.white)
                    .padding(.top, 8)
                    .padding(.leading, 12)
                Spacer()
                Text(verbatim: "\(Int(value*10)) \(KmmLocalizedStrings.measureType())")
                    .font(.custom("SFProDisplay-Light", size: 14))
                    .foregroundColor(.white)
                    .padding(.top, 8)
                    .padding(.trailing, 12)
            }

            HStack {
//                StepButton(label: "-", action: decrement)
//                    .padding(.leading, 8)
                CustomSlider(
                    value: $value,
                    range: range,
                    trackHeight: 30,
                    cornerRadius: 10,
                    borderWidth: 1,
                    activeColor: Color("ubi4_active"),
                    inactiveColor: Color("ubi4_gray"),
                    borderColor: Color("ubi4_gray_border"),
                    editingDidEnd: { _ in }
                )
                .padding(.horizontal, 24)
                .padding(.bottom, 8)
//                StepButton(label: "+", action: increment)
//                    .padding(.trailing, 8)
            }
            .padding(.bottom, 4)
        }
    }

    private func decrement() {
        let nextValue = value - 1
        value = max(range.lowerBound, min(nextValue, range.upperBound))
    }

    private func increment() {
        let nextValue = value + 1
        value = max(range.lowerBound, min(nextValue, range.upperBound))
    }
}

//measure_ms

private extension Array {
    subscript(safe index: Int) -> Element? {
        guard indices.contains(index) else { return nil }
        return self[index]
    }
}
