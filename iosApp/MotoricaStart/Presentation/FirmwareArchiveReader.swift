import Foundation
import zlib

enum FirmwareArchiveReader {
    enum ReaderError: LocalizedError {
        case cannotOpenArchive
        case missingDescriptor
        case missingPayload
        case invalidDescriptorEncoding
        case unsupportedCompression(Int)
        case invalidArchive
        case decompressionFailed(Int32)

        var errorDescription: String? {
            switch self {
            case .cannotOpenArchive:
                return "Cannot open firmware archive"
            case .missingDescriptor:
                return "FW_ini.ini not found in firmware archive"
            case .missingPayload:
                return "Firmware .bin file not found in archive"
            case .invalidDescriptorEncoding:
                return "Cannot read firmware descriptor"
            case let .unsupportedCompression(method):
                return "Unsupported firmware archive compression method: \(method)"
            case .invalidArchive:
                return "Invalid firmware archive"
            case let .decompressionFailed(code):
                return "Firmware archive decompression failed: \(code)"
            }
        }
    }

    static func readArchive(at url: URL) throws -> AccountFirmwareArchive {
        guard let archiveData = try? Data(contentsOf: url) else {
            throw ReaderError.cannotOpenArchive
        }
        let entries = try parseCentralDirectory(in: archiveData)

        guard let descriptorEntry = entries.first(where: {
            !$0.path.hasSuffix("/") && $0.path.split(separator: "/").last?.lowercased() == "fw_ini.ini"
        }) else {
            throw ReaderError.missingDescriptor
        }

        guard let payloadEntry = entries.first(where: {
            !$0.path.hasSuffix("/") && $0.path.lowercased().hasSuffix(".bin")
        }) else {
            throw ReaderError.missingPayload
        }

        let descriptorData = try extract(entry: descriptorEntry, from: archiveData)
        guard let descriptorText = String(data: descriptorData, encoding: .utf8)
            ?? String(data: descriptorData, encoding: .windowsCP1251)
        else {
            throw ReaderError.invalidDescriptorEncoding
        }

        return AccountFirmwareArchive(
            fileName: url.lastPathComponent,
            descriptorText: descriptorText,
            payload: try extract(entry: payloadEntry, from: archiveData)
        )
    }

    private static func parseCentralDirectory(in data: Data) throws -> [ZipEntry] {
        guard let endOffset = findEndOfCentralDirectory(in: data) else {
            throw ReaderError.invalidArchive
        }

        let entriesCount = Int(data.uint16(at: endOffset + 10))
        let centralDirectoryOffset = Int(data.uint32(at: endOffset + 16))
        var offset = centralDirectoryOffset
        var entries: [ZipEntry] = []

        for _ in 0..<entriesCount {
            guard offset + 46 <= data.count, data.uint32(at: offset) == 0x02014B50 else {
                throw ReaderError.invalidArchive
            }

            let method = Int(data.uint16(at: offset + 10))
            let compressedSize = Int(data.uint32(at: offset + 20))
            let uncompressedSize = Int(data.uint32(at: offset + 24))
            let nameLength = Int(data.uint16(at: offset + 28))
            let extraLength = Int(data.uint16(at: offset + 30))
            let commentLength = Int(data.uint16(at: offset + 32))
            let localHeaderOffset = Int(data.uint32(at: offset + 42))
            let nameStart = offset + 46
            let nameEnd = nameStart + nameLength
            guard nameEnd <= data.count else { throw ReaderError.invalidArchive }

            let nameData = data.subdata(in: nameStart..<nameEnd)
            let path = String(data: nameData, encoding: .utf8)
                ?? String(data: nameData, encoding: .windowsCP1251)
                ?? ""
            entries.append(
                ZipEntry(
                    path: path,
                    compressionMethod: method,
                    compressedSize: compressedSize,
                    uncompressedSize: uncompressedSize,
                    localHeaderOffset: localHeaderOffset
                )
            )
            offset = nameEnd + extraLength + commentLength
        }

        return entries
    }

    private static func findEndOfCentralDirectory(in data: Data) -> Int? {
        guard data.count >= 22 else { return nil }
        let minOffset = max(0, data.count - 22 - 0xFFFF)
        var offset = data.count - 22
        while offset >= minOffset {
            if data.uint32(at: offset) == 0x06054B50 {
                return offset
            }
            offset -= 1
        }
        return nil
    }

    private static func extract(entry: ZipEntry, from data: Data) throws -> Data {
        let offset = entry.localHeaderOffset
        guard offset + 30 <= data.count, data.uint32(at: offset) == 0x04034B50 else {
            throw ReaderError.invalidArchive
        }

        let nameLength = Int(data.uint16(at: offset + 26))
        let extraLength = Int(data.uint16(at: offset + 28))
        let start = offset + 30 + nameLength + extraLength
        let end = start + entry.compressedSize
        guard start <= end, end <= data.count else { throw ReaderError.invalidArchive }

        let compressedData = data.subdata(in: start..<end)
        switch entry.compressionMethod {
        case 0:
            return compressedData
        case 8:
            return try inflateRawDeflate(compressedData, expectedSize: entry.uncompressedSize)
        default:
            throw ReaderError.unsupportedCompression(entry.compressionMethod)
        }
    }

    private static func inflateRawDeflate(_ data: Data, expectedSize: Int) throws -> Data {
        var stream = z_stream()
        let initStatus = inflateInit2_(
            &stream,
            -MAX_WBITS,
            ZLIB_VERSION,
            Int32(MemoryLayout<z_stream>.size)
        )
        guard initStatus == Z_OK else {
            throw ReaderError.decompressionFailed(initStatus)
        }
        defer { inflateEnd(&stream) }

        var output = Data(count: Swift.max(Swift.max(expectedSize, data.count * 2), 1024))
        let status: Int32 = data.withUnsafeBytes { inputBuffer in
            guard let inputBase = inputBuffer.bindMemory(to: Bytef.self).baseAddress else {
                return Z_BUF_ERROR
            }
            stream.next_in = UnsafeMutablePointer<Bytef>(mutating: inputBase)
            stream.avail_in = uInt(data.count)

            var inflateStatus: Int32 = Z_OK
            while inflateStatus == Z_OK {
                let written = Int(stream.total_out)
                if written == output.count {
                    output.append(Data(count: output.count))
                }
                let available = output.count - written
                output.withUnsafeMutableBytes { outputBuffer in
                    guard let outputBase = outputBuffer.bindMemory(to: Bytef.self).baseAddress else {
                        inflateStatus = Z_BUF_ERROR
                        return
                    }
                    stream.next_out = outputBase.advanced(by: written)
                    stream.avail_out = uInt(available)
                    inflateStatus = inflate(&stream, Z_NO_FLUSH)
                }
            }
            return inflateStatus
        }

        guard status == Z_STREAM_END else {
            throw ReaderError.decompressionFailed(status)
        }
        let outputSize = Int(stream.total_out)
        if outputSize < output.count {
            output.removeSubrange(outputSize..<output.count)
        }
        return output
    }
}

private struct ZipEntry {
    let path: String
    let compressionMethod: Int
    let compressedSize: Int
    let uncompressedSize: Int
    let localHeaderOffset: Int
}

private extension Data {
    func uint16(at offset: Int) -> UInt16 {
        UInt16(self[offset])
            | (UInt16(self[offset + 1]) << 8)
    }

    func uint32(at offset: Int) -> UInt32 {
        UInt32(self[offset])
            | (UInt32(self[offset + 1]) << 8)
            | (UInt32(self[offset + 2]) << 16)
            | (UInt32(self[offset + 3]) << 24)
    }
}
