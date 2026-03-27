@testable import MotoricaStart
import XCTest

final class ObservableTests: XCTestCase {
    private final class ObserverToken {}

    func testObserve_immediatelyEmitsCurrentValue() {
        let observable = Observable<Int>(7)
        let token = ObserverToken()
        var received = [Int]()

        observable.observe(on: token) { received.append($0) }

        XCTAssertEqual(received, [7])
    }

    func testValueChange_notifiesObserver() {
        let observable = Observable<String>("initial")
        let token = ObserverToken()
        var received = [String]()

        observable.observe(on: token) { received.append($0) }
        observable.value = "updated"

        XCTAssertEqual(received, ["initial", "updated"])
    }

    func testRemoveObserver_stopsFurtherUpdates() {
        let observable = Observable<Int>(1)
        let token = ObserverToken()
        var received = [Int]()

        observable.observe(on: token) { received.append($0) }
        observable.remove(observer: token)
        observable.value = 2

        XCTAssertEqual(received, [1])
    }
}
