import Foundation

struct AccountFirmwareFile {
    let name: String
    let url: URL
    let isDeletable: Bool
}

struct AccountFirmwareArchive {
    let fileName: String
    let descriptorText: String
    let payload: Data
}
