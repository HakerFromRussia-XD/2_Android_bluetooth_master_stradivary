//
//  SliderRowView.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 10.06.2025.
//

import Combine

/// Observable-обёртка, чтобы обновлять значение слайдера извне.
final class SliderProvider: ObservableObject {
    @Published var value_1: Float
    let title_1: String
    let numLabel_1: String
    @Published var value_2: Float
    let title_2: String
    let numLabel_2: String
    @Published var isSecondSliderShow: Bool
    var maxProgress: Float
    var minProgress: Float

    init(
        value_1: Float = 0,
        title_1: String,
        numLabel_1: String,
        value_2: Float = 0,
        title_2: String,
        numLabel_2: String,
        isSecondSliderShow: Bool = true,
        maxProgress: Float = 100,
        minProgress: Float = 0
    ) {
        self.value_1 = value_1
        self.title_1 = title_1
        self.numLabel_1 = numLabel_1
        self.value_2 = value_2
        self.title_2 = title_2
        self.numLabel_2 = numLabel_2
        self.isSecondSliderShow = isSecondSliderShow
        self.maxProgress = maxProgress
        self.minProgress = minProgress
    }
}



