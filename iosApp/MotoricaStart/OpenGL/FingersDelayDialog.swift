//
//  FingersDelayDialog.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 26.01.2026.
//

import SwiftUI
import UIKit

@objcMembers
final class FingersDelayDialogPresenter: NSObject {
    @objc static func present(
        from viewController: UIViewController,
        title: String,
        saveTitle: String,
        cancelTitle: String,
        delayValues: [NSNumber],
        onSave: @escaping ([NSNumber]) -> Void
    ) {
        let initialValues = delayValues.map { $0.floatValue }
        let dismiss = {
            viewController.dismiss(animated: true)
        }
        let dialogView = FingersDelayDialogOverlay(
            title: title,
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
        hostingController.view.backgroundColor = .clear
        viewController.present(hostingController, animated: true)
    }
}

private struct FingersDelayDialogOverlay: View {
    let title: String
    let saveTitle: String
    let cancelTitle: String
    let initialValues: [Float]
    var onSave: ([Float]) -> Void
    var onCancel: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.65)
                .ignoresSafeArea()

            FingersDelayDialog(
                title: title,
                saveTitle: saveTitle,
                cancelTitle: cancelTitle,
                initialValues: initialValues,
                onSave: onSave,
                onCancel: onCancel
            )
            .padding(.horizontal, 8)
        }
    }
}

private struct FingersDelayDialog: View {
    let title: String
    let saveTitle: String
    let cancelTitle: String
    let initialValues: [Float]
    var onSave: ([Float]) -> Void
    var onCancel: () -> Void

    @State private var values: [Float]

    init(
        title: String,
        saveTitle: String,
        cancelTitle: String,
        initialValues: [Float],
        onSave: @escaping ([Float]) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.title = title
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

            ScrollView {
                VStack(spacing: 0) {
                    ForEach(0..<6, id: \.self) { index in
                        if index > 0 {
                            Rectangle()
                                .fill(Color("ubi4_gray_border"))
                                .frame(height: 1)
                        }

                        FingersDelaySliderRow(
                            title: "Палец \(index + 1)",
                            value: binding(for: index),
                            range: 0...100
                        )
                    }
                }
            }
            .frame(maxHeight: 360)
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

private struct FingersDelaySliderRow: View {
    let title: String
    @Binding var value: Float
    let range: ClosedRange<Float>

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(title)
                    .font(.custom("SFProDisplay-Light", size: 14))
                    .padding(.top, 8)
                    .padding(.leading, 12)
                Spacer()
                Text("\(Int(value))")
                    .font(.custom("SFProDisplay-Light", size: 14))
                    .padding(.top, 8)
                    .padding(.trailing, 12)
            }

            HStack {
                StepButton(label: "-", action: decrement)
                    .padding(.leading, 8)
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
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
                StepButton(label: "+", action: increment)
                    .padding(.trailing, 8)
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

private extension Array {
    subscript(safe index: Int) -> Element? {
        guard indices.contains(index) else { return nil }
        return self[index]
    }
}
