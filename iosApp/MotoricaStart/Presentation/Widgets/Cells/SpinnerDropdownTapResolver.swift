import CoreGraphics

enum SpinnerDropdownTapResolver {
    static func selectedIndex(
        at location: CGPoint,
        dropdownFrame: CGRect,
        itemFrames: [Int: CGRect],
        itemCount: Int
    ) -> Int? {
        guard itemCount > 0 else { return nil }

        let validItemFrames = itemFrames.filter { itemFrame in
            itemFrame.key >= 0 && itemFrame.key < itemCount
        }

        if let exactIndex = validItemFrames.first(where: { itemFrame in
            itemFrame.value.contains(location)
        })?.key {
            return exactIndex
        }

        if !validItemFrames.isEmpty {
            return nil
        }

        guard dropdownFrame.contains(location) else { return nil }

        let relativeY = location.y - dropdownFrame.minY
        let itemHeight = max(dropdownFrame.height / CGFloat(itemCount), 1)
        let rawIndex = Int(floor(relativeY / itemHeight))
        return min(max(rawIndex, 0), itemCount - 1)
    }
}
