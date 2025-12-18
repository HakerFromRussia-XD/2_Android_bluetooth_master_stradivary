//
//  PlotThresholds.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 17.12.2025.
//

import Foundation

struct PlotThresholds {
    let threshold1: Int
    let threshold2: Int
    let threshold3: Int
    let threshold4: Int
    let threshold5: Int
    let threshold6: Int

    static func decode(from hex: String) -> PlotThresholds {
        var padded = hex
        if padded.count < 22 {
            padded.append(String(repeating: "0", count: 22 - padded.count))
        }

        func value(_ start: Int, _ end: Int) -> Int {
            guard end <= padded.count, start < end else { return 0 }

            let startIndex = padded.index(padded.startIndex, offsetBy: start)
            let endIndex = padded.index(padded.startIndex, offsetBy: end)
            return Int(padded[startIndex..<endIndex], radix: 16) ?? 0
        }

        return PlotThresholds(
            threshold1: value(0, 2),
            threshold2: value(4, 6),
            threshold3: value(8, 10),
            threshold4: value(12, 14),
            threshold5: value(16, 18),
            threshold6: value(20, 22)
        )
    }

    var values: [Int] {
        [threshold1, threshold2, threshold3, threshold4, threshold5, threshold6]
    }
}
