//
//  GestureOpticProvider.swift
//  MotoricaStart
//
//  Created by Motorica LLC on 24.10.2025.
//

import SwiftUI

final class GestureOpticProvider: ObservableObject {
    @Published var selectedSegment: Segment = .collection
    @Published var isFactoryExpanded: Bool = true
    @Published var activeGestureId: Int?
    @Published var activeGestureTitle: String?
    @Published var factoryGestures: [GestureDisplayItem]
    @Published var customGestures: [GestureDisplayItem]
    @Published var rotationGroup: [GestureDisplayItem]
    @Published var sprGestures: [SprGestureDisplayItem]
//    let title: String

    init(factoryGestures: [GestureDisplayItem],
         customGestures: [GestureDisplayItem],
         rotationGroup: [GestureDisplayItem],
         sprGestures: [SprGestureDisplayItem],
         activeGestureId: Int = 0,
         activeGestureTitle: String?
    ) {
        self.factoryGestures = factoryGestures
        self.customGestures = customGestures
        self.rotationGroup = rotationGroup
        self.sprGestures = sprGestures
        self.activeGestureId = activeGestureId
        self.activeGestureTitle = activeGestureTitle
    }
    
    
    struct GestureDisplayItem: Identifiable, Hashable {
        let id: Int
        let title: String
        var subtitle: String?
    }

    struct SprGestureDisplayItem: Identifiable, Hashable {
        let id: Int
        let title: String
        var subtitle: String?
    }

    enum Segment: CaseIterable {
        case collection
        case rotationGroup
        case sprGroup

        var title: String {
            switch self {
            case .collection:
                return NSLocalizedString("Collection", comment: "")
            case .rotationGroup:
                return NSLocalizedString("Rotation", comment: "")
            case .sprGroup:
                return "SPR"
            }
        }
    }
}
