import Foundation
import QuartzCore
import shared

enum MotoricaGameControlAppGroup {
    static let value: String? = {
        guard let value = Bundle.main.object(
            forInfoDictionaryKey: "MotoricaGameControlAppGroup"
        ) as? String,
        !value.isEmpty,
        !value.contains("$(") else {
            return nil
        }
        return value
    }()
}

enum MotoricaGameLaunchRequest {
    private enum Keys {
        static let storage = "launchRequest.fluxara_drift.v1"
        static let sushiStorage = "launchRequest.sushidash.v1"
        static let version = "version"
        static let token = "token"
        static let timestampMs = "timestampMs"
        static let scheme = "scheme"
    }

    @discardableResult
    static func create(scheme: String) -> String? {
        let isSushi = scheme.caseInsensitiveCompare("sushi-dash") == .orderedSame
        guard (isSushi || scheme.caseInsensitiveCompare("fluxara-drive") == .orderedSame),
              let appGroup = MotoricaGameControlAppGroup.value,
              let defaults = UserDefaults(suiteName: appGroup) else {
            return nil
        }

        let token = UUID().uuidString
        var request: [String: Any] = [
            Keys.version: 1,
            Keys.token: token,
            Keys.timestampMs: Int64(Date().timeIntervalSince1970 * 1000.0),
            Keys.scheme: scheme
        ]
        if isSushi { request["bundleId"] = "com.hakerfromrussia.trashdash" }
        defaults.set(request, forKey: isSushi ? Keys.sushiStorage : Keys.storage)
        defaults.synchronize()
        NSLog("[BLE stk-game debug] ios launch lease written token=\(token)")
        return token
    }

    static func clear(token: String?) {
        guard let token,
              let appGroup = MotoricaGameControlAppGroup.value,
              let defaults = UserDefaults(suiteName: appGroup) else { return }
        for storage in [Keys.storage, Keys.sushiStorage] {
            guard let request = defaults.dictionary(forKey: storage),
                  request[Keys.token] as? String == token else { continue }
            defaults.removeObject(forKey: storage)
        }
        defaults.synchronize()
    }
}

// One effective UI pair per actual plot target. This is process-local mirroring,
// not a device setting and not an additional BLE command source.
struct MotoricaGameThresholdTarget: Hashable {
    let isV3: Bool
    let address: Int32
    let parameter: Int32
    let code: Int32

    static func canonicalCode(_ code: Int32) -> Int32 {
        [Int32(0x2F), 0x30, 0x1A].contains(code) ? 0x2F : code
    }
    static let standardV3 = MotoricaGameThresholdTarget(isV3: true, address: 1, parameter: 0x0F, code: 0x2F)
}

struct MotoricaGameThresholdPair: Equatable {
    let open: Int
    let close: Int
    var valid: Bool { (0...255).contains(open) && (0...255).contains(close) }
    // MS iOS labels OPEN on CH2 and CLOSE on CH1. Game directions retain their
    // accepted raw order: game openLevel=data[0]/CH1, closeLevel=data[1]/CH2.
    var rawChannels: (open: Int, close: Int) { (open: close, close: open) }
}

enum MotoricaGameThresholdSource { case seed, edited, confirmed }

final class MotoricaGameThresholdState {
    static let shared = MotoricaGameThresholdState()
    static let changed = Notification.Name("MotoricaGameThresholdStateChanged")
    private struct Entry { var pair: MotoricaGameThresholdPair; var editedAt: TimeInterval? }
    private let lock = NSLock()
    private var entries: [MotoricaGameThresholdTarget: Entry] = [:]
    private var selected: MotoricaGameThresholdTarget?

    func currentTarget(isV3: Bool) -> MotoricaGameThresholdTarget? {
        lock.lock(); defer { lock.unlock() }
        return selected?.isV3 == isV3 ? selected : nil
    }
    func current(isV3: Bool) -> MotoricaGameThresholdPair? {
        lock.lock(); defer { lock.unlock() }
        guard let target = selected, target.isV3 == isV3 else { return nil }
        return entries[target]?.pair
    }
    func update(open: Int, close: Int, target: MotoricaGameThresholdTarget,
                source: MotoricaGameThresholdSource, now: TimeInterval = CACurrentMediaTime()) {
        let pair = MotoricaGameThresholdPair(open: open, close: close)
        guard pair.valid else { return }
        lock.lock()
        let old = entries[target]
        // Layout/reconfigure seeds must never replace an effective edited pair
        // with an older confirmed cache. Keep the cell's existing 0.5s grace
        // across reuse as well as inside a currently displayed cell.
        if source == .seed, old != nil {
            let targetChanged = selected != target
            selected = target
            lock.unlock()
            if targetChanged { NotificationCenter.default.post(name: Self.changed, object: self) }
            return
        }
        if source == .confirmed, let old, let editedAt = old.editedAt,
           old.pair != pair, now - editedAt < 0.5 { lock.unlock(); return }
        let changed = old?.pair != pair || selected != target
        entries[target] = Entry(pair: pair, editedAt: source == .edited ? now : nil)
        selected = target
        lock.unlock()
        if changed { NotificationCenter.default.post(name: Self.changed, object: self) }
    }
    func clear() {
        lock.lock(); entries.removeAll(); selected = nil; lock.unlock()
    }
}

final class GameControlBroadcaster {
    static let shared = GameControlBroadcaster()

    private enum Keys {
        static let snapshot = "snapshot"
        static let version = "version"
        static let seq = "seq"
        static let timestampMs = "timestampMs"
        static let openLevel = "openLevel"
        static let closeLevel = "closeLevel"
        static let connected = "connected"
    }

    private let logPrefix = "[BLE stk-game debug]"
    private let minPublishInterval: TimeInterval = 1.0 / 30.0
    private let publishQueue = DispatchQueue(label: "com.motorica.start.gamecontrol.publish")
    private var observeJob: Kotlinx_coroutines_coreJob?
    private var thresholdV3Job: Kotlinx_coroutines_coreJob?
    private var thresholdLegacyJob: Kotlinx_coroutines_coreJob?
    private var thresholdObserver: NSObjectProtocol?
    private var latestPublished: [String: Any]?
    private var sequence: Int64 = 0
    private var lastPublishTime: CFTimeInterval = 0
    private var appGroupWarningLogged = false
    private var sushiSnapshots: [[String: Any]] = []

    private init() {}

    func start() {
        guard observeJob == nil else { return }
        thresholdObserver = NotificationCenter.default.addObserver(
            forName: MotoricaGameThresholdState.changed, object: nil, queue: nil
        ) { [weak self] _ in self?.publishThresholdChange() }
        thresholdV3Job = WidgetStateBridgeV3.shared.observeUpdates { [weak self] snapshot in
            guard UiInterfaceModeBridgeV3.shared.isEnabled(), snapshot.codecId == "THRESHOLDS" else { return }
            let target = MotoricaGameThresholdState.shared.currentTarget(isV3: true) ?? .standardV3
            guard snapshot.addressDevice == target.address, snapshot.parameterID == target.parameter,
                  MotoricaGameThresholdTarget.canonicalCode(snapshot.dataCode) == target.code,
                  let deviceOpen = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "openThreshold"),
                  let deviceClose = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "closeThreshold") else { return }
            MotoricaGameThresholdState.shared.update(open: deviceClose, close: deviceOpen,
                target: target, source: .confirmed)
            self?.publishThresholdChange()
        }
        thresholdLegacyJob = WidgetStateBridge.shared.observeThresholdFlow { [weak self] ref in
            guard !UiInterfaceModeBridgeV3.shared.isEnabled(), ref.dataCode == 26 else { return }
            guard let target = MotoricaGameThresholdState.shared.currentTarget(isV3: false),
                  ref.addressDevice == target.address, ref.parameterID == target.parameter else { return }
            let raw = ParameterProvider.Companion().getParameter(
                deviceAddress: ref.addressDevice, parameterID: ref.parameterID).data
            if let pair = Self.legacyUIPair(raw) {
                MotoricaGameThresholdState.shared.update(open: pair.open, close: pair.close,
                    target: target, source: .confirmed)
            }
            self?.publishThresholdChange()
        }
        observeJob = WidgetStateBridge.shared.observeGameControlSignal { [weak self] signal in
            if !signal.connected { MotoricaGameThresholdState.shared.clear() }
            let thresholds = signal.connected ? self?.currentThresholds() : nil
            self?.publishQueue.async {
                self?.publish(signal: signal, force: !signal.connected, thresholds: thresholds)
            }
        }
        NSLog("\(logPrefix) ios broadcaster started")
    }

    func stop() {
        observeJob?.cancel(cause: nil)
        observeJob = nil
        thresholdV3Job?.cancel(cause: nil); thresholdV3Job = nil
        thresholdLegacyJob?.cancel(cause: nil); thresholdLegacyJob = nil
        if let thresholdObserver { NotificationCenter.default.removeObserver(thresholdObserver) }
        thresholdObserver = nil
        MotoricaGameThresholdState.shared.clear()
        publishDisconnected()
        NSLog("\(logPrefix) ios broadcaster stopped")
    }

    func publishDisconnected() {
        publish(openLevel: 0, closeLevel: 0, connected: false, force: true)
    }

    private static func legacyUIPair(_ raw: String) -> MotoricaGameThresholdPair? {
        guard raw.count >= 6 else { return nil }
        let bytes = Array(raw)
        guard let open = Int(String(bytes[0..<2]), radix: 16),
              let close = Int(String(bytes[4..<6]), radix: 16) else { return nil }
        let pair = MotoricaGameThresholdPair(open: open, close: close)
        return pair.valid ? pair : nil
    }

    private func currentThresholds() -> (open: Int, close: Int)? {
        let isV3 = UiInterfaceModeBridgeV3.shared.isEnabled()
        if let effective = MotoricaGameThresholdState.shared.current(isV3: isV3) {
            return effective.rawChannels
        }
        if isV3 {
            // Only the canonical standard registry target is a fallback before
            // the actual plot widget seeds its binding. Do not pick old aliases.
            let target = MotoricaGameThresholdTarget.standardV3
            guard let snapshot = WidgetStateBridgeV3.shared.getCurrent(
                addressDevice: target.address, parameterID: target.parameter, dataCode: target.code
            ), snapshot.codecId == "THRESHOLDS",
               let deviceOpen = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "openThreshold"),
               let deviceClose = V3SnapshotParser.intField(from: snapshot.serializedValue, field: "closeThreshold"),
               (0...255).contains(deviceOpen), (0...255).contains(deviceClose) else { return nil }
            // Canonical V3 values already use raw channel order. No second swap.
            return (open: deviceOpen, close: deviceClose)
        }
        // Legacy needs an actual UI/ref target; a global first matching
        // parameter may belong to another widget and is not authoritative.
        return nil
    }

    private func publishThresholdChange() {
        let thresholds = currentThresholds()
        publishQueue.async { [weak self] in
            guard let self, var snapshot = self.latestPublished else { return }
            let available = (snapshot[Keys.connected] as? Bool == true) && thresholds != nil
            let open = available ? thresholds!.open : -1
            let close = available ? thresholds!.close : -1
            guard snapshot["thresholdsAvailable"] as? Bool != available ||
                  snapshot["openThreshold"] as? Int != open ||
                  snapshot["closeThreshold"] as? Int != close else { return }
            // Metadata update only: seq/timestamp/levels/connected are unchanged.
            snapshot["thresholdsAvailable"] = available
            snapshot["openThreshold"] = open
            snapshot["closeThreshold"] = close
            self.writeSnapshot(snapshot, metadataOnly: true)
        }
    }

    private func writeSnapshot(_ snapshot: [String: Any], metadataOnly: Bool) {
        guard let appGroup = MotoricaGameControlAppGroup.value,
              let defaults = UserDefaults(suiteName: appGroup) else { return }
        latestPublished = snapshot
        defaults.set(snapshot, forKey: Keys.snapshot)
        if metadataOnly {
            if let last = sushiSnapshots.last,
               (last[Keys.seq] as? Int64) == (snapshot[Keys.seq] as? Int64),
               (last[Keys.timestampMs] as? Int64) == (snapshot[Keys.timestampMs] as? Int64) {
                sushiSnapshots[sushiSnapshots.count - 1] = snapshot
            }
        } else {
            if snapshot[Keys.connected] as? Bool != true { sushiSnapshots.removeAll(keepingCapacity: true) }
            sushiSnapshots.append(snapshot)
            if sushiSnapshots.count > 64 { sushiSnapshots.removeFirst(sushiSnapshots.count - 64) }
        }
        defaults.set(sushiSnapshots, forKey: "sushidash.snapshots.v1")
        defaults.synchronize()
    }

    private func publish(signal: GameControlSignal, force: Bool, thresholds: (open: Int, close: Int)?) {
        publish(
            openLevel: Int(signal.openLevel),
            closeLevel: Int(signal.closeLevel),
            connected: signal.connected,
            force: force,
            packetSeq: Int64(signal.packetSeq),
            thresholds: thresholds
        )
    }

    private func publish(openLevel: Int, closeLevel: Int, connected: Bool, force: Bool) {
        publishQueue.async { [weak self] in
            self?.publish(openLevel: openLevel, closeLevel: closeLevel, connected: connected, force: force, packetSeq: 0, thresholds: nil)
        }
    }

    private func publish(openLevel: Int, closeLevel: Int, connected: Bool, force: Bool, packetSeq: Int64,
                         thresholds: (open: Int, close: Int)?) {
        let now = CACurrentMediaTime()
        guard force || now - lastPublishTime >= minPublishInterval else { return }
        lastPublishTime = now

        guard let appGroup = MotoricaGameControlAppGroup.value,
              UserDefaults(suiteName: appGroup) != nil else {
            if !appGroupWarningLogged {
                appGroupWarningLogged = true
                NSLog("\(logPrefix) ios broadcaster expected exactly one signed app group")
            }
            return
        }

        sequence += 1
        let snapshot: [String: Any] = [
            Keys.version: 1,
            Keys.seq: sequence,
            Keys.timestampMs: Int64(Date().timeIntervalSince1970 * 1000.0),
            Keys.openLevel: min(max(openLevel, 0), 255),
            Keys.closeLevel: min(max(closeLevel, 0), 255),
            Keys.connected: connected,
            "thresholdsAvailable": connected && thresholds != nil,
            "openThreshold": thresholds?.open ?? -1,
            "closeThreshold": thresholds?.close ?? -1
        ]
        writeSnapshot(snapshot, metadataOnly: false)

        if !connected || sequence <= 3 || sequence % 30 == 0 {
            NSLog("\(logPrefix) ios publish seq=\(sequence) packetSeq=\(packetSeq) open=\(snapshot[Keys.openLevel] ?? 0) close=\(snapshot[Keys.closeLevel] ?? 0) connected=\(connected ? 1 : 0)")
        }
    }
}
