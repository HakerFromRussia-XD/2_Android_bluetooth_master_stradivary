import Foundation
import shared

enum AccountFirmwareSource {
    case local(URL, isDeletable: Bool)
    case remote(V3ServiceFirmwareFile)
}

struct AccountFirmwareFile {
    let name: String
    let source: AccountFirmwareSource

    var url: URL? {
        if case .local(let url, _) = source { return url }
        return nil
    }

    var isDeletable: Bool {
        if case .local(_, let isDeletable) = source { return isDeletable }
        return false
    }

    init(name: String, url: URL, isDeletable: Bool) {
        self.name = name
        self.source = .local(url, isDeletable: isDeletable)
    }

    init(remoteFile: V3ServiceFirmwareFile) {
        self.name = remoteFile.name
        self.source = .remote(remoteFile)
    }
}

struct AccountFirmwareArchive {
    let fileName: String
    let descriptorText: String
    let payload: Data
}
