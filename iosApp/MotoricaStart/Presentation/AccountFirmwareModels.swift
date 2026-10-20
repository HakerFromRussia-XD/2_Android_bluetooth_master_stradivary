import Foundation
import shared

struct AccountFirmwareFile {
    let name: String
    let url: URL?
    let isDeletable: Bool
    let remoteFile: RemoteFirmwareFile?

    init(name: String, url: URL, isDeletable: Bool) {
        self.name = name
        self.url = url
        self.isDeletable = isDeletable
        self.remoteFile = nil
    }

    init(remoteFile: RemoteFirmwareFile) {
        self.name = remoteFile.name
        self.url = nil
        self.isDeletable = false
        self.remoteFile = remoteFile
    }
}

struct AccountFirmwareArchive {
    let fileName: String
    let descriptorText: String
    let payload: Data
}
