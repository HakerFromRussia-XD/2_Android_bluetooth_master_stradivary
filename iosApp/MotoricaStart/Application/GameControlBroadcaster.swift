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
    private var sequence: Int64 = 0
    private var lastPublishTime: CFTimeInterval = 0
    private var appGroupWarningLogged = false

    private init() {}

    func start() {
        guard observeJob == nil else { return }
        observeJob = WidgetStateBridge.shared.observeGameControlSignal { [weak self] signal in
            self?.publishQueue.async {
                self?.publish(signal: signal, force: !signal.connected)
            }
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

        guard let appGroup = MotoricaGameControlAppGroup.value,
              let defaults = UserDefaults(suiteName: appGroup) else {
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
            Keys.connected: connected
        ]
        defaults.set(snapshot, forKey: Keys.snapshot)

        if !connected || sequence <= 3 || sequence % 30 == 0 {
            NSLog("\(logPrefix) ios publish seq=\(sequence) packetSeq=\(packetSeq) open=\(snapshot[Keys.openLevel] ?? 0) close=\(snapshot[Keys.closeLevel] ?? 0) connected=\(connected ? 1 : 0)")
        }
    }
}
