//
//  SliderViewCellV3.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 14.04.2026.
//

import SwiftUI

final class SliderViewCellV3: UITableViewCell {
    static let reuseIdentifier = String(describing: SliderViewCellV3.self)

    private var viewModel: SliderListItemViewModelV3?
    private var provider: SliderProvider?
    private var job: Kotlinx_coroutines_coreJob?

    override func prepareForReuse() {
        super.prepareForReuse()
        print("[V3-SLIDER][CELL] prepareForReuse")
        job?.cancel(cause: nil)
        job = nil
        provider = nil
        viewModel = nil
        contentConfiguration = nil
    }

    @available(iOS 16.0, *)
    func configure(with viewModel: SliderListItemViewModelV3) {
        self.viewModel = viewModel
        print(
            "[V3-SLIDER][CELL] configure title=\(viewModel.title) binding=\(String(describing: viewModel.binding)) range=\(viewModel.minProgress)...\(viewModel.maxProgress)"
        )
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")

        let provider = SliderProvider(
            value_1: .zero,
            title_1: viewModel.title,
            numLabel_1: viewModel.title_2,
            value_2: .zero,
            title_2: viewModel.title,
            numLabel_2: viewModel.title_2,
            isSecondSliderShow: false,
            maxProgress: Float(viewModel.maxProgress),
            minProgress: Float(viewModel.minProgress)
        )
        self.provider = provider

        if let currentValue = viewModel.currentSliderValue() {
            provider.value_1 = Float(currentValue)
            print("[V3-SLIDER][CELL] configure currentSliderValue=\(currentValue)")
        } else {
            print("[V3-SLIDER][CELL] configure currentSliderValue is nil")
        }

        var configuration = UIHostingConfiguration {
            SliderRowView(
                provider: provider,
                onFirstSliderEditingEnded: { [weak self] _ in
                    print("[V3-SLIDER][CELL] onFirstSliderEditingEnded value=\(String(describing: self?.provider?.value_1))")
                    self?.sliderEditingDidEnd()
                },
                onSecondSliderEditingEnded: nil
            )
        }
        configuration = configuration.margins(.vertical, 4)
        contentConfiguration = configuration

        job?.cancel(cause: nil)
        job = WidgetStateBridgeV3.shared.observeUpdates { [weak self] snapshot in
            guard let self else { return }
            guard self.viewModel?.matches(snapshot: snapshot) == true else { return }
            print(
                "[V3-SLIDER][CELL] observeUpdates matched codec=\(snapshot.codecId) serialized=\(snapshot.serializedValue)"
            )
            guard let value = self.viewModel?.sliderValue(from: snapshot) else { return }
            print("[V3-SLIDER][CELL] observeUpdates sliderValue=\(value)")
            DispatchQueue.main.async {
                self.provider?.value_1 = Float(value)
            }
        }

        print("[V3-SLIDER][CELL] requestCurrent")
        viewModel.requestCurrent()
    }

    private func sliderEditingDidEnd() {
        guard let provider else { return }
        let rounded = Int(provider.value_1.rounded())
        print("[V3-SLIDER][CELL] sliderEditingDidEnd provider.value_1=\(provider.value_1) rounded=\(rounded)")
        viewModel?.sendSliderValue(rounded)
    }
}
