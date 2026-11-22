import CryptoKit
import Foundation
import Security

/// The app data file, encrypted with AES-256-GCM. The key lives in the Keychain, only on this
/// device and only while it is unlocked. The file is excluded from iCloud and iTunes backups.
final class SecureStore {
    enum StoreError: Error { case keychain(OSStatus), corrupted }

    private let fileURL: URL
    private let keyAccount: String

    init(fileName: String = "ondea.dat", keyAccount: String = "ondea_data_key") {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        fileURL = dir.appendingPathComponent(fileName)
        self.keyAccount = keyAccount
    }

    func read() throws -> String? {
        guard FileManager.default.fileExists(atPath: fileURL.path) else { return nil }
        let sealed = try Data(contentsOf: fileURL)
        guard let box = try? AES.GCM.SealedBox(combined: sealed) else { throw StoreError.corrupted }
        let plain = try AES.GCM.open(box, using: try key())
        return String(decoding: plain, as: UTF8.self)
    }

    func write(_ json: String) throws {
        let sealed = try AES.GCM.seal(Data(json.utf8), using: try key())
        guard let combined = sealed.combined else { throw StoreError.corrupted }
        try combined.write(to: fileURL, options: [.atomic, .completeFileProtection])
        var url = fileURL
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        try url.setResourceValues(values)
    }

    /// Deletes the data file and its key.
    func wipe() {
        try? FileManager.default.removeItem(at: fileURL)
        SecItemDelete(query() as CFDictionary)
    }

    private func query() -> [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: "app.ondea",
         kSecAttrAccount as String: keyAccount]
    }

    private func key() throws -> SymmetricKey {
        var find = query()
        find[kSecReturnData as String] = true
        var item: CFTypeRef?
        let status = SecItemCopyMatching(find as CFDictionary, &item)
        if status == errSecSuccess, let data = item as? Data {
            return SymmetricKey(data: data)
        }
        guard status == errSecItemNotFound else { throw StoreError.keychain(status) }
        let newKey = SymmetricKey(size: .bits256)
        var add = query()
        add[kSecValueData as String] = newKey.withUnsafeBytes { Data($0) }
        add[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        let added = SecItemAdd(add as CFDictionary, nil)
        guard added == errSecSuccess else { throw StoreError.keychain(added) }
        return newKey
    }
}
