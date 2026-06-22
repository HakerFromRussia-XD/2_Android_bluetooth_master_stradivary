import Foundation
import QuartzCore
import shared

final class GameControlBroadcaster {
    static let shared = GameControlBroadcaster()

    private enum Keys {
        static let appGroup = "group.com.motorica.gamecontrol"
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
    private var observeJob: Kotlinx_coroutines_coreJob?
    private var sequence: Int64 = 0
    private var lastPublishTime: CFTimeInterval = 0
    private var appGroupWarningLogged = false

    private init() {}

    func start() {
        guard observeJob == nil else { return }
        observeJob = WidgetStateBridge.shared.observeGameControlSignal { [weak self] signal in
            self?.publish(signal: signal, force: !signal.connected)
        }
        NSLog("\(logPrefix) ios broadcaster started")
    }

    func stop() {
        observeJob?.cancel(cause: nil)
        observeJob = nil
        publishDisconnected()
        NSLog("\(logPrefix) ios broadcaster stopped")
    }

    func publishDisconnected() {
        publish(openLevel: 0, closeLevel: 0, connected: false, force: true)
    }

    private func publish(signal: GameControlSignal, force: Bool) {
        publish(
            openLevel: Int(signal.openLevel),
            closeLevel: Int(signal.closeLevel),
            connected: signal.connected,
            force: force,
            packetSeq: Int64(signal.packetSeq)
        )
    }

    private func publish(openLevel: Int, closeLevel: Int, connected: Bool, force: Bool) {
        publish(openLevel: openLevel, closeLevel: closeLevel, connected: connected, force: force, packetSeq: 0)
    }

    private func publish(openLevel: Int, closeLevel: Int, connected: Bool, force: Bool, packetSeq: Int64) {
        let now = CACurrentMediaTime()
        guard force || now - lastPublishTime >= minPublishInterval else { return }
        lastPublishTime = now

        guard let defaults = UserDefaults(suiteName: Keys.appGroup) else {
            if !appGroupWarningLogged {
                appGroupWarningLogged = true
                NSLog("\(logPrefix) ios broadcaster app group unavailable: \(Keys.appGroup)")
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
            Keys.connected: connected
        ]
        defaults.set(snapshot, forKey: Keys.snapshot)
        defaults.synchronize()

        if !connected || sequence <= 3 || sequence % 30 == 0 {
            NSLog("\(logPrefix) ios publish seq=\(sequence) packetSeq=\(packetSeq) open=\(snapshot[Keys.openLevel] ?? 0) close=\(snapshot[Keys.closeLevel] ?? 0) connected=\(connected ? 1 : 0)")
        }
    }
}
