import XCTest
import UIKit
import shared
@testable import MotoricaStart

final class HelpInstructionLocalizationTests: XCTestCase {
    @MainActor
    func testSensorLegendMatchesFigmaReference() throws {
        let isRussian = Locale.preferredLanguages.first?.hasPrefix("ru") == true
        let locale = isRussian ? "ru" : "en"
        let controller = HelpViewController(pageId: "sensors")
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 390, height: 844))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        defer { window.isHidden = true }
        controller.loadViewIfNeeded()
        controller.view.layoutIfNeeded()
        let views = descendants(controller.view)
        let opening = try XCTUnwrap(views.first { $0.accessibilityIdentifier == "help.sensor.opening.row" } as? UIStackView)
        let closing = try XCTUnwrap(views.first { $0.accessibilityIdentifier == "help.sensor.closing.row" } as? UIStackView)
        let openingTitle = isRussian ? "Датчик открытия" : "Opening sensor"
        let closingTitle = isRussian ? "Датчик закрытия" : "Closing sensor"
        for (row, title, white) in [(opening, openingTitle, CGFloat(1)), (closing, closingTitle, CGFloat(191.0 / 255.0))] {
            XCTAssertEqual(row.bounds.height, 30, accuracy: 0.01)
            let label = try XCTUnwrap(row.arrangedSubviews.compactMap { $0 as? UILabel }.first)
            let marker = try XCTUnwrap(row.arrangedSubviews.first)
            XCTAssertEqual(label.text, title)
            XCTAssertEqual(label.font.pointSize, 14)
            XCTAssertTrue(label.font.fontName.hasPrefix("OpenSans"))
            assertColor(label.textColor, white: 131.0 / 255.0)
            assertColor(try XCTUnwrap(marker.backgroundColor), white: white)
            XCTAssertEqual(marker.bounds.width, 14, accuracy: 0.01)
            XCTAssertEqual(marker.bounds.height, 14, accuracy: 0.01)
            XCTAssertEqual(label.frame.minX - marker.frame.maxX, 12, accuracy: 0.01)
        }
        let openingY = opening.convert(.zero, to: controller.view).y
        let closingY = closing.convert(.zero, to: controller.view).y
        XCTAssertEqual(closingY - openingY, 30, accuracy: 0.01)
        XCTAssertEqual(opening.layer.shadowOpacity, 0.25, accuracy: 0.001)
        XCTAssertEqual(opening.layer.shadowRadius, 4)
        XCTAssertEqual(opening.layer.shadowOffset, CGSize(width: 0, height: 4))
        XCTAssertEqual(closing.layer.shadowOpacity, 0)
        let snapshot = UIGraphicsImageRenderer(size: CGSize(width: opening.bounds.width, height: 80)).image { context in
            UIColor(white: 55.0 / 255.0, alpha: 1).setFill()
            context.fill(CGRect(x: 0, y: 0, width: opening.bounds.width, height: 80))
            context.cgContext.translateBy(x: 0, y: 10)
            opening.layer.render(in: context.cgContext)
            context.cgContext.translateBy(x: 0, y: 30)
            closing.layer.render(in: context.cgContext)
        }
        let attachment = XCTAttachment(image: snapshot)
        attachment.name = "sensor-legend-\(locale)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    @MainActor
    func testAdvancedInstructionUsesNativeLocalizedTextAndWidgetImages() throws {
        let isRussian = Locale.preferredLanguages.first?.hasPrefix("ru") == true
        let locale = isRussian ? "ru" : "en"
        let controller = HelpViewController(pageId: "advanced")
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 390, height: 844))
        window.rootViewController = controller
        window.makeKeyAndVisible()
        defer { window.isHidden = true }
        controller.loadViewIfNeeded()
        controller.view.layoutIfNeeded()

        let views = descendants(controller.view)
        let labels = views.compactMap { $0 as? UILabel }
        let expectedTitle = isRussian ? "Переключение жестов сенсорами" : "Switching gestures with sensors"
        let firstTitle = try XCTUnwrap(labels.first { $0.text == expectedTitle })
        XCTAssertEqual(firstTitle.font.pointSize, 14)
        XCTAssertTrue(firstTitle.font.fontName.hasPrefix("Inter"), firstTitle.font.fontName)
        XCTAssertFalse(labels.contains { $0.text?.contains("Если хочешь") == true })
        let page = try XCTUnwrap(InstructionBridge.shared.page(id: "advanced"))
        var checkedTextBlocks = 0
        for card in page.cards {
            for block in card.blocks where block.type == .heading || block.type == .paragraph {
                let text = try XCTUnwrap(block.text?.desc().localized())
                let label = try XCTUnwrap(labels.first { $0.text == text })
                let isHeading = block.type == .heading
                XCTAssertEqual(label.font.pointSize, 14)
                XCTAssertTrue(label.font.fontName.hasPrefix(isHeading ? "Inter" : "OpenSans"), label.font.fontName)
                XCTAssertGreaterThan(label.bounds.width, 0)
                let required = label.sizeThatFits(CGSize(width: label.bounds.width, height: .greatestFiniteMagnitude))
                XCTAssertGreaterThanOrEqual(label.bounds.height + 1, required.height, "Truncated text: \(text)")
                checkedTextBlocks += 1
            }
        }
        XCTAssertEqual(checkedTextBlocks, 32)
        let widgets = views.compactMap { $0 as? UIImageView }.filter {
            $0.accessibilityIdentifier?.hasPrefix("help.widget.") == true
        }
        XCTAssertEqual(widgets.count, 8)
        for (index, widget) in widgets.enumerated() {
            XCTAssertEqual(widget.accessibilityIdentifier, "help.widget.ubi4_help_widget_\(index + 1)_\(locale)")
            XCTAssertNotNil(widget.image)
            XCTAssertGreaterThan(widget.bounds.height, 0)
            XCTAssertGreaterThan(widget.bounds.width, 0)
        }
        let scroll = try XCTUnwrap(views.compactMap { $0 as? UIScrollView }.first)
        let stack = try XCTUnwrap(scroll.subviews.compactMap { $0 as? UIStackView }.first)
        XCTAssertGreaterThan(stack.bounds.height, 0)
        let snapshot = UIGraphicsImageRenderer(bounds: stack.bounds).image { context in
            (controller.view.backgroundColor ?? .black).setFill()
            context.fill(stack.bounds)
            stack.layer.render(in: context.cgContext)
        }
        let attachment = XCTAttachment(image: snapshot)
        attachment.name = "advanced-instruction-\(locale)"
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func descendants(_ view: UIView) -> [UIView] {
        [view] + view.subviews.flatMap(descendants)
    }

    private func assertColor(_ color: UIColor, white: CGFloat, file: StaticString = #filePath, line: UInt = #line) {
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        XCTAssertTrue(color.getRed(&red, green: &green, blue: &blue, alpha: &alpha), file: file, line: line)
        for channel in [red, green, blue] {
            XCTAssertEqual(channel, white, accuracy: 0.001, file: file, line: line)
        }
        XCTAssertEqual(alpha, 1, accuracy: 0.001, file: file, line: line)
    }
}
