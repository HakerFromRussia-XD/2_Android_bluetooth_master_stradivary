import Foundation

@objc public class DataManager: NSObject {

    private static var documentDirectoryOverride: URL?
    private static var saveObjectStringCache: [String: SaveObjectString]?
    private static var saveObjectStringCacheDirectory: URL?
    private static let cacheQueue = DispatchQueue(label: "com.motorica.oldstart.datamanager.cache")

    // get Document Directory
    static fileprivate func getDocumentDirectory () -> URL {
        if let override = documentDirectoryOverride {
            return override
        }
        if let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            return url
        }else{
            fatalError("Unable to access document directory")
        }
    }

    // Save any kind of codable objects
    static func save <T:Encodable> (_ object:T, with fileName:String) {
        let url = getDocumentDirectory().appendingPathComponent(fileName, isDirectory: false)

        let encoder = JSONEncoder()

        do {
            try FileManager.default.createDirectory(at: getDocumentDirectory(), withIntermediateDirectories: true, attributes: nil)
            let data = try encoder.encode(object)
            try data.write(to: url, options: .atomic)
            updateSaveObjectStringCacheAfterSave(object, fileName: fileName)

        }catch{
            fatalError(error.localizedDescription)
        }

    }


    // Load any kind of codable objects
    static func load <T:Decodable> (_ fileName:String, with type:T.Type) -> T {
        let url = getDocumentDirectory().appendingPathComponent(fileName, isDirectory: false)
        if !FileManager.default.fileExists(atPath: url.path) {
            fatalError("File not found at path \(url.path)")
        }

        if let data = FileManager.default.contents(atPath: url.path) {
            do {
                let model = try JSONDecoder().decode(type, from: data)
                return model
            }catch{
                print("================")
                print("Описание ошибки ---->",String(describing: error),"<----")
                print("================")
                fatalError(error.localizedDescription)
            }

        }else{
            fatalError("Data unavailable at path \(url.path)")
        }

    }


    // Load data from a file
    static func loadData (_ fileName:String) -> Data? {
        let url = getDocumentDirectory().appendingPathComponent(fileName, isDirectory: false)
        if !FileManager.default.fileExists(atPath: url.path) {
            fatalError("File not found at path \(url.path)")
        }

        if let data = FileManager.default.contents(atPath: url.path) {
            return data

        }else{
            fatalError("Data unavailable at path \(url.path)")
        }

    }

    // Load all files from a directory
    static func loadAll <T:Decodable> (_ type:T.Type) -> [T] {
        if type == SaveObjectString.self {
            return loadAllSaveObjectStrings() as! [T]
        }

        let directory = getDocumentDirectory()
        guard let fileURLs = regularFileURLs(in: directory) else {
            return []
        }

        var modelObjects = [T]()
        let decoder = JSONDecoder()
        for fileURL in fileURLs {
            do {
                let data = try Data(contentsOf: fileURL)
                let model = try decoder.decode(type, from: data)
                modelObjects.append(model)
            } catch {
                logSkippedFile(fileURL, error: error)
            }
        }
        return modelObjects
    }


    // Delete a file
    static func delete (_ fileName:String) {
        let url = getDocumentDirectory().appendingPathComponent(fileName, isDirectory: false)

        if FileManager.default.fileExists(atPath: url.path) {
            do {
                try FileManager.default.removeItem(at: url)
                updateSaveObjectStringCacheAfterDelete(fileName)
            }catch{
                fatalError(error.localizedDescription)
            }
        }
    }

    static func setDocumentDirectoryOverrideForTesting(_ url: URL?) {
        cacheQueue.sync {
            documentDirectoryOverride = url
            saveObjectStringCache = nil
            saveObjectStringCacheDirectory = nil
        }
    }

    static func resetCacheForTesting() {
        cacheQueue.sync {
            saveObjectStringCache = nil
            saveObjectStringCacheDirectory = nil
        }
    }

    private static func loadAllSaveObjectStrings() -> [SaveObjectString] {
        let directory = getDocumentDirectory()
        if let cached = cachedSaveObjectStrings(for: directory) {
            return cached
        }

        guard let fileURLs = regularFileURLs(in: directory) else {
            replaceSaveObjectStringCache([:], directory: directory)
            return []
        }

        let decoder = JSONDecoder()
        var objectsByKey = [String: SaveObjectString]()
        for fileURL in fileURLs {
            do {
                let data = try Data(contentsOf: fileURL)
                let model = try decoder.decode(SaveObjectString.self, from: data)
                objectsByKey[model.key] = model
            } catch {
                logSkippedFile(fileURL, error: error)
            }
        }
        replaceSaveObjectStringCache(objectsByKey, directory: directory)
        return sortedSaveObjectStrings(objectsByKey)
    }

    private static func cachedSaveObjectStrings(for directory: URL) -> [SaveObjectString]? {
        return cacheQueue.sync {
            guard saveObjectStringCacheDirectory == directory, let cache = saveObjectStringCache else {
                return nil
            }
            return sortedSaveObjectStrings(cache)
        }
    }

    private static func replaceSaveObjectStringCache(_ cache: [String: SaveObjectString], directory: URL) {
        cacheQueue.sync {
            saveObjectStringCache = cache
            saveObjectStringCacheDirectory = directory
        }
    }

    private static func updateSaveObjectStringCacheAfterSave<T>(_ object: T, fileName: String) {
        guard let saveObjectString = object as? SaveObjectString else {
            return
        }
        let directory = getDocumentDirectory()
        cacheQueue.sync {
            if saveObjectStringCacheDirectory != directory {
                saveObjectStringCache = nil
                saveObjectStringCacheDirectory = directory
            }
            if saveObjectStringCache != nil {
                saveObjectStringCache?[saveObjectString.key] = saveObjectString
                if saveObjectString.key != fileName {
                    saveObjectStringCache?[fileName] = nil
                }
            }
        }
    }

    private static func updateSaveObjectStringCacheAfterDelete(_ fileName: String) {
        let directory = getDocumentDirectory()
        cacheQueue.sync {
            guard saveObjectStringCacheDirectory == directory else {
                saveObjectStringCache = nil
                saveObjectStringCacheDirectory = directory
                return
            }
            saveObjectStringCache?[fileName] = nil
        }
    }

    private static func regularFileURLs(in directory: URL) -> [URL]? {
        do {
            return try FileManager.default.contentsOfDirectory(
                at: directory,
                includingPropertiesForKeys: [.isRegularFileKey],
                options: [.skipsHiddenFiles]
            )
            .filter { fileURL in
                let values = try? fileURL.resourceValues(forKeys: [.isRegularFileKey])
                return values?.isRegularFile == true
            }
            .sorted { $0.lastPathComponent < $1.lastPathComponent }
        } catch {
            NSLog("[INDY_FIX] DataManager loadAll could not list %@: %@", directory.path, String(describing: error))
            return nil
        }
    }

    private static func sortedSaveObjectStrings(_ cache: [String: SaveObjectString]) -> [SaveObjectString] {
        return cache.values.sorted { $0.key < $1.key }
    }

    private static func logSkippedFile(_ url: URL, error: Error) {
        NSLog("[INDY_FIX] DataManager loadAll skipped %@: %@", url.lastPathComponent, String(describing: error))
    }



}
