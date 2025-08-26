//
//  ContentView.swift
//  PreviewHost
//
//  Created by Денис Осхин on 25.08.2025.
//

import SwiftUI

struct ContentView: View {
    var body: some View {
        ZStack {
            Color("ubi4_back").ignoresSafeArea()

            StepButton(title: "Gesture №1") {
            }
            .padding()
        }
    }
}

#Preview {
    ContentView()
}
