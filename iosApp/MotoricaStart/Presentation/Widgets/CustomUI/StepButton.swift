//
//  StepButton.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 22.08.2025.
//

import SwiftUICore
import SwiftUI


struct StepButton: View {
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.custom("SFProDisplay-Light", size: 14))
                .foregroundColor(Color("ubi4_white"))
                .frame(width: 48, height: 30)
        }
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(Color("ubi4_gray"))
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(Color("ubi4_gray_border"), lineWidth: 1)
                )
        )
    }
}
