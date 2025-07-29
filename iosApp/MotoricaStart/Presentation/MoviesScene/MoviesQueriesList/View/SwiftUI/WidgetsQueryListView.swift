import Foundation
import SwiftUI

@available(iOS 13.0, *)
extension WidgetsQueryListItemViewModel: Identifiable { }

@available(iOS 13.0, *)
struct WidgetsQueryListView: View {
    @ObservedObject var viewModelWrapper: WidgetsQueryListViewModelWrapper
    
    var body: some View {
        List(viewModelWrapper.items) { item in
            Button(action: {
                self.viewModelWrapper.viewModel?.didSelect(item: item)
            }) {
                Text(item.query)
            }
        }
        .onAppear {
            self.viewModelWrapper.viewModel?.viewWillAppear()
        }
    }
}

@available(iOS 13.0, *)
final class WidgetsQueryListViewModelWrapper: ObservableObject {
    var viewModel: WidgetsQueryListViewModel?
    @Published var items: [WidgetsQueryListItemViewModel] = []
    
    init(viewModel: WidgetsQueryListViewModel?) {
        self.viewModel = viewModel
        viewModel?.items.observe(on: self) { [weak self] values in self?.items = values }
    }
}

#if DEBUG
@available(iOS 13.0, *)
struct WidgetsQueryListView_Previews: PreviewProvider {
    static var previews: some View {
        WidgetsQueryListView(viewModelWrapper: previewViewModelWrapper)
    }
    
    static var previewViewModelWrapper: WidgetsQueryListViewModelWrapper = {
        var viewModel = WidgetsQueryListViewModelWrapper(viewModel: nil)
        viewModel.items = [WidgetsQueryListItemViewModel(query: "item 1"),
                           WidgetsQueryListItemViewModel(query: "item 2")
        ]
        return viewModel
    }()
}
#endif
