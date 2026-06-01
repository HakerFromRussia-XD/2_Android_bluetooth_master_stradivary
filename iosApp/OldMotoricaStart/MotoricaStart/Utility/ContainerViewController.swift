import UIKit

class ContainerViewController: UIViewController {
    @IBOutlet weak var containerView: UIView!
    
    var scanViewController:ScanViewController!
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "ScanVC" {
            scanViewController = (segue.destination as! UINavigationController).childViewControllers.first as? ScanViewController
        }
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
}
