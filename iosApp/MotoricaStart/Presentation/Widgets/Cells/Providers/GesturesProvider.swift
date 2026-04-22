//
//  GestureProvider.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 24.10.2025.
//

import Combine
import UIKit

final class GesturesProvider: ObservableObject {
//    @Published var selectedSegment: Segment = .collection
    private static let selectedSegmentDefaultsKey = "GesturesWidgetSelectedSegment"
    private enum Constants {
        static let factoryExpandedKey = "GesturesProvider.isFactoryExpanded"
    }

    @Published var selectedSegment: Segment = .collection {
        didSet { saveSelectedSegment(selectedSegment) }
    }
//    @Published var isFactoryExpanded: Bool = true
    @Published var isFactoryExpanded: Bool {
        didSet {
            Self.saveFactoryExpandedState(isFactoryExpanded)
        }
    }
    @Published var activeGestureId: Int?
    @Published var activeGestureTitle: String?
    @Published var factoryGestures: [GestureDisplayItem]
    @Published var customGestures: [GestureDisplayItem]
    @Published var rotationGroup: [GestureDisplayItem]
    @Published var sprGestures: [SprGestureDisplayItem]

    init(factoryGestures: [GestureDisplayItem],
         customGestures: [GestureDisplayItem],
         rotationGroup: [GestureDisplayItem],
         sprGestures: [SprGestureDisplayItem],
         activeGestureId: Int = 0,
         activeGestureTitle: String?
    ) {
        self.isFactoryExpanded = Self.loadFactoryExpandedState()
        self.selectedSegment = Self.loadSelectedSegment()
        self.factoryGestures = factoryGestures
        self.customGestures = customGestures
        self.rotationGroup = rotationGroup
        self.sprGestures = sprGestures
        self.activeGestureId = activeGestureId
        self.activeGestureTitle = activeGestureTitle
    }
    
    private static func loadFactoryExpandedState() -> Bool {
        UserDefaults.standard.object(forKey: Constants.factoryExpandedKey) as? Bool ?? true
    }

    private static func saveFactoryExpandedState(_ isExpanded: Bool) {
        UserDefaults.standard.set(isExpanded, forKey: Constants.factoryExpandedKey)
    }
    
    struct GestureDisplayItem: Identifiable, Hashable {
        let id: Int
        let title: String
        var subtitle: String?
        let image: UIImage?

        static func == (lhs: GestureDisplayItem, rhs: GestureDisplayItem) -> Bool {
            lhs.id == rhs.id
            && lhs.title == rhs.title
            && lhs.subtitle == rhs.subtitle
        }

        func hash(into hasher: inout Hasher) {
            hasher.combine(id)
            hasher.combine(title)
            hasher.combine(subtitle)
        }
    }

    struct SprGestureDisplayItem: Identifiable, Hashable {
        let id: Int
        let title: String
        var subtitle: String?
        var boundGestureId: Int?
    }

    enum Segment: Int, CaseIterable {
        case collection
        case rotationGroup
        case sprGroup

        var title: String {
            switch self {
            case .collection:
                return NSLocalizedString("collection_of_gestures", comment: "")
            case .rotationGroup:
                return NSLocalizedString("rotation_group", comment: "")
            case .sprGroup:
                return NSLocalizedString("spr_gestures", comment: "")
            }
        }
    }
    
    private static func loadSelectedSegment() -> Segment {
        if ProcessInfo.processInfo.arguments.contains("-ui-test-gestures-default-rotation") {
            return .rotationGroup
        }
        let savedValue = UserDefaults.standard.integer(forKey: selectedSegmentDefaultsKey)
        return Segment(rawValue: savedValue) ?? .collection
    }

    private func saveSelectedSegment(_ segment: Segment) {
        UserDefaults.standard.set(segment.rawValue, forKey: Self.selectedSegmentDefaultsKey)
    }
}
