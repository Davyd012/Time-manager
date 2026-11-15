import Foundation
import Combine
import SwiftUI
import UniformTypeIdentifiers
import UIKit
import TimnorShared
import Security
import Network

struct TimnorContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var connectivityMonitor = TimnorConnectivityMonitor()
    @State private var isExportPresented = false
    @State private var isXlsxExportPresented = false
    @State private var isImportPresented = false
    @State private var isSharePresented = false
    @State private var isRestoreConfirmationPresented = false
    @State private var backupDocument = TimnorBackupDocument(contents: "")
    @State private var xlsxDocument = TimnorXlsxDocument(data: Data())
    @State private var shareURL: URL?
    @State private var shareTextItemSource: TimnorShareTextItemSource?
    @State private var pendingBackupContents: String?
    @State private var restoreSummary = ""
    @State private var statusMessage: String?
    @State private var sharedDocumentAction: SharedDocumentAction?
    @State private var sharedRestoreRequested = false
    @State private var sharedSharePending = false
    @State private var backupFileName = "timnor-backup"
    @State private var spreadsheetFileName = "Timer"
    @State private var documentEffectHandler = TimnorIosDocumentEffectHandler()

    var body: some View {
        TimnorComposeHost(
            documentHandler: documentEffectHandler,
            sessionStorage: IosPlannorSessionStorage(),
        )
        .onChange(of: scenePhase) { phase in
            if phase == .active {
                IosPlannorSyncBridge.shared.retrySync()
            }
        }
        .onChange(of: connectivityMonitor.reconnectGeneration) { _ in
            IosPlannorSyncBridge.shared.retrySync()
        }
        .fileExporter(
            isPresented: $isExportPresented,
            document: backupDocument,
            contentType: .json,
            defaultFilename: backupFileName,
        ) { result in
            let sharedAction = sharedDocumentAction
            sharedDocumentAction = nil
            switch result {
            case .success:
                statusMessage = "Sikkerhetskopi lagret"
                if sharedAction == .backup {
                    IosTimnorSettingsBridge.shared.reportBackupSaveResult(succeeded: true)
                }
            case .failure(let error):
                statusMessage = "Kunne ikke lagre sikkerhetskopien"
                if sharedAction == .backup {
                    if (error as NSError).code == NSUserCancelledError {
                        IosTimnorSettingsBridge.shared.backupSaveCancelled()
                    } else {
                        IosTimnorSettingsBridge.shared.reportBackupSaveResult(succeeded: false)
                    }
                }
            }
        }
        .fileExporter(
            isPresented: $isXlsxExportPresented,
            document: xlsxDocument,
            contentType: TimnorXlsxDocument.xlsxType,
            defaultFilename: spreadsheetFileName,
        ) { result in
            let sharedAction = sharedDocumentAction
            sharedDocumentAction = nil
            switch result {
            case .success:
                statusMessage = "Excel-fil lagret"
                if sharedAction == .spreadsheet {
                    IosTimnorSettingsBridge.shared.reportSpreadsheetSaveResult(succeeded: true)
                }
            case .failure(let error):
                statusMessage = "Kunne ikke lagre Excel-filen"
                if sharedAction == .spreadsheet {
                    if (error as NSError).code == NSUserCancelledError {
                        IosTimnorSettingsBridge.shared.spreadsheetSaveCancelled()
                    } else {
                        IosTimnorSettingsBridge.shared.reportSpreadsheetSaveResult(succeeded: false)
                    }
                }
            }
        }
        .fileImporter(isPresented: $isImportPresented, allowedContentTypes: [.json]) { result in
            let sharedRequest = sharedRestoreRequested
            sharedRestoreRequested = false
            switch result {
            case .success(let url):
                if sharedRequest {
                    openSharedBackup(from: url)
                } else {
                    restoreBackup(from: url)
                }
            case .failure(let error):
                if sharedRequest {
                    if (error as NSError).code != NSUserCancelledError {
                        IosTimnorSettingsBridge.shared.backupReadFailed()
                    }
                } else {
                    statusMessage = "Kunne ikke åpne sikkerhetskopien"
                }
            }
        }
        .alert("Gjenopprette sikkerhetskopi?", isPresented: $isRestoreConfirmationPresented) {
            Button("Avbryt", role: .cancel) { pendingBackupContents = nil }
            Button("Erstatt data", role: .destructive) {
                if let pendingBackupContents {
                    restoreBackup(contents: pendingBackupContents)
                }
            }
        } message: {
            Text("Sikkerhetskopien inneholder \(restoreSummary). Nåværende prosjekter og registreringer erstattes.")
        }
        .sheet(isPresented: $isSharePresented) {
            if let shareTextItemSource {
                TimnorActivityView(activityItems: [shareTextItemSource]) { completed, error in
                    if sharedSharePending {
                        sharedSharePending = false
                        if completed {
                            IosTimnorSettingsBridge.shared.reportShareResult(succeeded: true)
                        } else if error == nil {
                            IosTimnorSettingsBridge.shared.shareCancelled()
                        } else {
                            IosTimnorSettingsBridge.shared.reportShareResult(succeeded: false)
                        }
                    }
                }
            } else if let shareURL {
                TimnorActivityView(activityItems: [shareURL]) { completed, error in
                    if sharedSharePending {
                        sharedSharePending = false
                        if completed {
                            IosTimnorSettingsBridge.shared.reportShareResult(succeeded: true)
                        } else if error == nil {
                            IosTimnorSettingsBridge.shared.shareCancelled()
                        } else {
                            IosTimnorSettingsBridge.shared.reportShareResult(succeeded: false)
                        }
                    }
                }
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .timnorSaveBackupRequested)) { notification in
            guard let contents = notification.userInfo?["contents"] as? String else {
                IosTimnorSettingsBridge.shared.reportBackupSaveResult(succeeded: false)
                return
            }
            backupDocument = TimnorBackupDocument(contents: contents)
            backupFileName = (notification.userInfo?["fileName"] as? String ?? "timnor-backup.json")
                .replacingOccurrences(of: ".json", with: "")
            sharedDocumentAction = .backup
            isExportPresented = true
        }
        .onReceive(NotificationCenter.default.publisher(for: .timnorShareBackupRequested)) { notification in
            guard let contents = notification.userInfo?["contents"] as? String else {
                IosTimnorSettingsBridge.shared.reportShareResult(succeeded: false)
                return
            }
            let fileName = notification.userInfo?["fileName"] as? String ?? "timnor-backup.json"
            do {
                let url = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)
                try Data(contents.utf8).write(to: url, options: .atomic)
                shareTextItemSource = nil
                shareURL = url
                sharedSharePending = true
                isSharePresented = true
            } catch {
                IosTimnorSettingsBridge.shared.reportShareResult(succeeded: false)
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .timnorShareTextRequested)) { notification in
            guard let subject = notification.userInfo?["subject"] as? String,
                  let contents = notification.userInfo?["contents"] as? String else {
                IosTimnorSettingsBridge.shared.reportShareResult(succeeded: false)
                return
            }
            shareURL = nil
            shareTextItemSource = TimnorShareTextItemSource(subject: subject, contents: contents)
            sharedSharePending = true
            isSharePresented = true
        }
        .onReceive(NotificationCenter.default.publisher(for: .timnorOpenBackupRequested)) { _ in
            sharedRestoreRequested = true
            isImportPresented = true
        }
        .onReceive(NotificationCenter.default.publisher(for: .timnorSaveSpreadsheetRequested)) { notification in
            guard let contents = notification.userInfo?["contentsBase64"] as? String,
                  let data = Data(base64Encoded: contents) else {
                IosTimnorSettingsBridge.shared.reportSpreadsheetSaveResult(succeeded: false)
                return
            }
            xlsxDocument = TimnorXlsxDocument(data: data)
            spreadsheetFileName = (notification.userInfo?["fileName"] as? String ?? "Timer.xlsx")
                .replacingOccurrences(of: ".xlsx", with: "")
            sharedDocumentAction = .spreadsheet
            isXlsxExportPresented = true
        }
    }

    private func prepareBackupExport() async {
        do {
            let contents = try await exportBackupContents()
            backupDocument = TimnorBackupDocument(contents: contents)
            isExportPresented = true
        } catch {
            statusMessage = "Kunne ikke opprette sikkerhetskopien"
        }
    }

    private func prepareSpreadsheetExport() {
        guard let bytes = Data(base64Encoded: IosTimnorPlatform.shared.exportCurrentMonthXlsxBase64()) else {
            statusMessage = "Kunne ikke opprette Excel-filen"
            return
        }
        xlsxDocument = TimnorXlsxDocument(data: bytes)
        isXlsxExportPresented = true
    }

    private func exportBackupContents() async throws -> String {
        try await withCheckedThrowingContinuation { continuation in
            IosTimnorPlatform.shared.exportBackupV1 { contents, error in
                if let error {
                    continuation.resume(throwing: error)
                } else if let contents {
                    continuation.resume(returning: contents)
                } else {
                    continuation.resume(throwing: CocoaError(.fileReadUnknown))
                }
            }
        }
    }

    private func prepareBackupShare() async {
        do {
            let contents = try await exportBackupContents()
            let url = FileManager.default.temporaryDirectory.appendingPathComponent("timnor-backup.json")
            try Data(contents.utf8).write(to: url, options: .atomic)
            shareURL = url
            isSharePresented = true
        } catch {
            statusMessage = "Kunne ikke opprette sikkerhetskopien"
        }
    }

    private func openSharedBackup(from url: URL) {
        let didAccess = url.startAccessingSecurityScopedResource()
        defer {
            if didAccess { url.stopAccessingSecurityScopedResource() }
        }
        do {
            let contents = try String(contentsOf: url, encoding: .utf8)
            IosTimnorSettingsBridge.shared.backupContentsSelected(contents: contents)
        } catch {
            IosTimnorSettingsBridge.shared.backupReadFailed()
        }
    }

    private func restoreBackup(from url: URL) {
        let didAccess = url.startAccessingSecurityScopedResource()
        defer {
            if didAccess { url.stopAccessingSecurityScopedResource() }
        }
        do {
            let contents = try String(contentsOf: url, encoding: .utf8)
            IosTimnorPlatform.shared.previewBackupV1(contents: contents) { summary, error in
                guard error == nil, let summary else {
                    statusMessage = "Ugyldig sikkerhetskopi"
                    return
                }
                let counts = summary.split(separator: "|")
                guard counts.count == 2 || counts.count == 3 else {
                    statusMessage = "Ugyldig sikkerhetskopi"
                    return
                }
                pendingBackupContents = contents
                let sharedRecordSummary = counts.count == 3 && String(counts[2]) != "0"
                    ? " og \(counts[2]) delte synkroniseringsposter"
                    : ""
                restoreSummary = "\(counts[0]) prosjekter og \(counts[1]) registreringer\(sharedRecordSummary)"
                isRestoreConfirmationPresented = true
            }
        } catch {
            statusMessage = "Kunne ikke lese sikkerhetskopien"
        }
    }

    private func restoreBackup(contents: String) {
        IosTimnorPlatform.shared.restoreBackupV1(contents: contents) { error in
            statusMessage = error == nil ? "Sikkerhetskopi gjenopprettet" : "Gjenopprettingen mislyktes"
            pendingBackupContents = nil
        }
    }
}

@MainActor
private final class TimnorConnectivityMonitor: ObservableObject {
    @Published private(set) var reconnectGeneration = 0
    private let monitor = NWPathMonitor()
    private var isConnected = false

    init() {
        monitor.pathUpdateHandler = { [weak self] path in
            let connected = path.status == .satisfied
            DispatchQueue.main.async {
                guard let self else { return }
                if connected && !self.isConnected {
                    self.reconnectGeneration += 1
                }
                self.isConnected = connected
            }
        }
        monitor.start(queue: DispatchQueue(label: "no.timnor.ios.connectivity"))
    }

    deinit {
        monitor.cancel()
    }
}

private struct TimnorComposeHost: UIViewControllerRepresentable {
    let documentHandler: TimnorIosDocumentEffectHandler
    let sessionStorage: IosPlannorSessionStorage

    func makeUIViewController(context: Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController(
            documentHandler: documentHandler,
            sessionStorage: sessionStorage,
        )
        let edgeBack = UIScreenEdgePanGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.handleEdgeBack(_:)),
        )
        edgeBack.edges = controller.view.effectiveUserInterfaceLayoutDirection == .rightToLeft ? .right : .left
        edgeBack.delegate = context.coordinator
        controller.view.addGestureRecognizer(edgeBack)
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    @MainActor
    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        @objc func handleEdgeBack(_ gesture: UIScreenEdgePanGestureRecognizer) {
            guard gesture.state == .ended else { return }
            let translation = gesture.translation(in: gesture.view).x
            let velocity = gesture.velocity(in: gesture.view).x
            let fromLeftEdge = gesture.edges.contains(.left)
            let passedThreshold = fromLeftEdge
                ? translation >= 72 || velocity >= 500
                : translation <= -72 || velocity <= -500
            if passedThreshold {
                IosTimnorNavigationBridge.shared.handleBack()
            }
        }

        func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
            IosTimnorNavigationBridge.shared.canHandleBack()
        }
    }
}

private final class IosPlannorSessionStorage: NSObject, PlannorSessionStorage {
    private struct SessionValue: Codable {
        let accessToken: String
        let refreshToken: String
        let accountId: Int32
        let username: String
    }

    private var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Bundle.main.bundleIdentifier ?? "no.timnor.ios",
            kSecAttrAccount as String: "plannor-session",
        ]
    }

    func read() -> PlannorSession? {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data,
              let stored = try? JSONDecoder().decode(SessionValue.self, from: data) else {
            return nil
        }
        return PlannorSession(
            accessToken: stored.accessToken,
            refreshToken: stored.refreshToken,
            accountId: stored.accountId,
            username: stored.username,
        )
    }

    func write(session: PlannorSession) {
        let stored = SessionValue(
            accessToken: session.accessToken,
            refreshToken: session.refreshToken,
            accountId: session.accountId,
            username: session.username,
        )
        guard let data = try? JSONEncoder().encode(stored) else { preconditionFailure("Unable to encode Plannor session") }
        SecItemDelete(baseQuery as CFDictionary)
        var item = baseQuery
        item[kSecValueData as String] = data
        item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        precondition(SecItemAdd(item as CFDictionary, nil) == errSecSuccess, "Unable to save Plannor session to Keychain")
    }

    func clear() {
        let status = SecItemDelete(baseQuery as CFDictionary)
        precondition(status == errSecSuccess || status == errSecItemNotFound, "Unable to clear Plannor session from Keychain")
    }
}

private struct TimnorActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]
    let onCompletion: (Bool, Error?) -> Void

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
        controller.completionWithItemsHandler = { _, completed, _, error in onCompletion(completed, error) }
        return controller
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

private final class TimnorShareTextItemSource: NSObject, UIActivityItemSource {
    private let subject: String
    private let contents: String

    init(subject: String, contents: String) {
        self.subject = subject
        self.contents = contents
    }

    func activityViewControllerPlaceholderItem(_ activityViewController: UIActivityViewController) -> Any {
        contents
    }

    func activityViewController(
        _ activityViewController: UIActivityViewController,
        itemForActivityType activityType: UIActivity.ActivityType?
    ) -> Any? {
        contents
    }

    func activityViewController(
        _ activityViewController: UIActivityViewController,
        subjectForActivityType activityType: UIActivity.ActivityType?
    ) -> String {
        subject
    }
}

private enum SharedDocumentAction: Equatable {
    case backup
    case spreadsheet
}

private final class TimnorIosDocumentEffectHandler: NSObject, IosTimnorDocumentEffectHandler {
    func saveBackup(contents: String, fileName: String) {
        NotificationCenter.default.post(
            name: .timnorSaveBackupRequested,
            object: nil,
            userInfo: ["contents": contents, "fileName": fileName],
        )
    }

    func shareBackup(contents: String, fileName: String) {
        NotificationCenter.default.post(
            name: .timnorShareBackupRequested,
            object: nil,
            userInfo: ["contents": contents, "fileName": fileName],
        )
    }

    func shareText(subject: String, contents: String) {
        NotificationCenter.default.post(
            name: .timnorShareTextRequested,
            object: nil,
            userInfo: ["subject": subject, "contents": contents],
        )
    }

    func openBackup() {
        NotificationCenter.default.post(name: .timnorOpenBackupRequested, object: nil)
    }

    func saveSpreadsheetBase64(contentsBase64: String, fileName: String) {
        NotificationCenter.default.post(
            name: .timnorSaveSpreadsheetRequested,
            object: nil,
            userInfo: ["contentsBase64": contentsBase64, "fileName": fileName],
        )
    }
}

private extension Notification.Name {
    static let timnorSaveBackupRequested = Notification.Name("timnor.saveBackupRequested")
    static let timnorShareBackupRequested = Notification.Name("timnor.shareBackupRequested")
    static let timnorShareTextRequested = Notification.Name("timnor.shareTextRequested")
    static let timnorOpenBackupRequested = Notification.Name("timnor.openBackupRequested")
    static let timnorSaveSpreadsheetRequested = Notification.Name("timnor.saveSpreadsheetRequested")
}

private struct TimnorBackupDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }

    let contents: String

    init(contents: String) {
        self.contents = contents
    }

    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents,
              let contents = String(data: data, encoding: .utf8) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        self.contents = contents
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(contents.utf8))
    }
}

private struct TimnorXlsxDocument: FileDocument {
    static var xlsxType: UTType { UTType(filenameExtension: "xlsx") ?? .data }
    static var readableContentTypes: [UTType] { [xlsxType] }

    let data: Data

    init(data: Data) {
        self.data = data
    }

    init(configuration: ReadConfiguration) throws {
        guard let contents = configuration.file.regularFileContents else {
            throw CocoaError(.fileReadCorruptFile)
        }
        data = contents
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}
