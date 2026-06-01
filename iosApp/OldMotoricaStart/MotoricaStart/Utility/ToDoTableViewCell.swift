import UIKit


class ToDoTableViewCell: UITableViewCell {


    @IBOutlet weak var scanLable: UILabel!
    
    
    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
        self.contentView.backgroundColor = UIColor.white
    }

}
