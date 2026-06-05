@testable import MotoricaStart
import XCTest

final class SpinnerDropdownTapResolverTests: XCTestCase {
    func testTapInTransparentButtonGapDoesNotSelectLastItem() {
        let itemFrames = makeItemFrames(itemCount: 5)
        let dropdownFrameIncludingButtonGap = CGRect(x: 0, y: 0, width: 220, height: 298)
        let tapInButtonGap = CGPoint(x: 110, y: 275)

        XCTAssertNil(
            SpinnerDropdownTapResolver.selectedIndex(
                at: tapInButtonGap,
                dropdownFrame: dropdownFrameIncludingButtonGap,
                itemFrames: itemFrames,
                itemCount: 5
            )
        )
    }

    func testTapInExactItemFrameSelectsThatItem() {
        let itemFrames = makeItemFrames(itemCount: 5)

        XCTAssertEqual(
            SpinnerDropdownTapResolver.selectedIndex(
                at: CGPoint(x: 110, y: 225),
                dropdownFrame: CGRect(x: 0, y: 0, width: 220, height: 298),
                itemFrames: itemFrames,
                itemCount: 5
            ),
            4
        )
    }

    func testFallbackSelectsByDropdownFrameWhenItemFramesAreUnavailable() {
        XCTAssertEqual(
            SpinnerDropdownTapResolver.selectedIndex(
                at: CGPoint(x: 110, y: 225),
                dropdownFrame: CGRect(x: 0, y: 0, width: 220, height: 250),
                itemFrames: [:],
                itemCount: 5
            ),
            4
        )
    }

    func testFallbackDoesNotSelectOutsideDropdownFrame() {
        XCTAssertNil(
            SpinnerDropdownTapResolver.selectedIndex(
                at: CGPoint(x: 110, y: 258),
                dropdownFrame: CGRect(x: 0, y: 0, width: 220, height: 250),
                itemFrames: [:],
                itemCount: 5
            )
        )
    }

    private func makeItemFrames(itemCount: Int) -> [Int: CGRect] {
        Dictionary(uniqueKeysWithValues: (0..<itemCount).map { index in
            (
                index,
                CGRect(
                    x: 0,
                    y: CGFloat(index * 50),
                    width: 220,
                    height: 50
                )
            )
        })
    }
}
