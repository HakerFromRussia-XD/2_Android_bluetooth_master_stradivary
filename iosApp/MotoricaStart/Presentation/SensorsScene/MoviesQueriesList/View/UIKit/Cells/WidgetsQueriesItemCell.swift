import UIKit

final class WidgetsQueriesItemCell: UITableViewCell {
    static let height = CGFloat(50)
    static let reuseIdentifier = String(describing: WidgetsQueriesItemCell.self)

    @IBOutlet private var titleLabel: UILabel!
    
    func fill(with suggestion: WidgetsQueryListItemViewModel) {
        self.titleLabel.text = suggestion.query
    }
}
