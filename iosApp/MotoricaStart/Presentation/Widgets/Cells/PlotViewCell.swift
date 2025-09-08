import UIKit
import SwiftUI
import Combine

final class PlotViewCell: UITableViewCell {
    static let reuseIdentifier = String(describing: PlotViewCell.self)
    static let height = CGFloat(130)
    private var viewModel: PlotListItemViewModel!

    var data: [CustomPlot.DataPoint]
    
    // Реализуем обязательный инициализатор для создания ячейки из кода
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        self.data = []
        super.init(style: style, reuseIdentifier: reuseIdentifier)
    }
    
    required init?(coder: NSCoder) {
        self.data = []
        super.init(coder: coder)
    }
    
    @available(iOS 16.0, *)
    func configure(with viewModel: PlotListItemViewModel) {
        self.viewModel = viewModel
        selectionStyle = .none
        backgroundColor = UIColor(named: "ubi4_back")
        
        // 2. Вклеиваем SwiftUI контент
        contentConfiguration = UIHostingConfiguration {
            CustomPlot(data: self.data)
        }
    }
}
