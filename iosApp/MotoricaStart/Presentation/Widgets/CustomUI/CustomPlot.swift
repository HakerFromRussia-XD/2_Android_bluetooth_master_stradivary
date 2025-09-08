//
//  CustomSwitcher.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 02.09.2025.
//
import SwiftUI
import Charts

struct CustomPlot: View {
    struct DataPoint: Identifiable {
        let id = UUID()
        let x: Double
        let y: Double
    }

    var data: [DataPoint]

    var body: some View {
        GeometryReader { proxy in
            LineChartContainer(data: data)
            .frame(width: proxy.size.width, height: proxy.size.height)
            .background(Color("ubi4_gray"))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onEnded { value in
                        let relativeY = value.location.y / proxy.size.height
                        let newValue = (1 - relativeY) * 1000 // sample scale
                    }
            )
        }
    }
}

private struct LineChartContainer: UIViewRepresentable {
    var data: [CustomPlot.DataPoint]

    func makeUIView(context: Context) -> LineChartView {
        let chart = LineChartView()
        chart.legend.enabled = false
        chart.rightAxis.enabled = false
        chart.xAxis.labelPosition = .bottom
        chart.setScaleEnabled(false)
        chart.dragEnabled = false
        chart.pinchZoomEnabled = false
        chart.doubleTapToZoomEnabled = false
        chart.backgroundColor = NSUIColor(named: "ubi4_gray")
        return chart
    }

    func updateUIView(_ uiView: LineChartView, context: Context) {
        let entries = data.map { ChartDataEntry(x: $0.x, y: $0.y) }
        let set = LineChartDataSet(entries: entries, label: "nil")
        set.drawCirclesEnabled = false
        set.drawValuesEnabled = false
        set.mode = .linear
        set.lineWidth = 1
        set.setColor(NSUIColor(named: "ubi4_active") ?? .systemBlue)

        uiView.data = LineChartData(dataSet: set)
        uiView.leftAxis.removeAllLimitLines()
        uiView.notifyDataSetChanged()
    }
}



