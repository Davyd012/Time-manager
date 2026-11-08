import Foundation
import SwiftUI
import UniformTypeIdentifiers
import UIKit
import TimnorShared

struct TimnorContentView: View {
    @State private var isExportPresented = false
    @State private var isXlsxExportPresented = false
    @State private var isImportPresented = false
    @State private var isSharePresented = false
    @State private var isRestoreConfirmationPresented = false
    @State private var backupDocument = TimnorBackupDocument(contents: "")
    @State private var xlsxDocument = TimnorXlsxDocument(data: Data())
    @State private var shareURL: URL?
    @State private var pendingBackupContents: String?
    @State private var restoreSummary = ""
    @State private var statusMessage: String?

    var body: some View {
        PlatformNavigation {
            TimnorComposeHost()
                .navigationTitle("Timnor")
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Menu {
                            Button("Eksporter sikkerhetskopi") {
                                Task { await prepareBackupExport() }
                            }
                            Button("Eksporter Excel") {
                                prepareSpreadsheetExport()
                            }
                            Button("Del sikkerhetskopi") {
                                Task { await prepareBackupShare() }
                            }
                            Button("Gjenopprett sikkerhetskopi") {
                                isImportPresented = true
                            }
                        } label: {
                            Label("Sikkerhetskopi", systemImage: "ellipsis.circle")
                        }
                        .accessibilityLabel("Sikkerhetskopi")
                    }
                }
                .safeAreaInset(edge: .bottom) {
                    if let statusMessage {
                        Text(statusMessage)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .padding()
                    }
                }
        }
        .fileExporter(
            isPresented: $isExportPresented,
            document: backupDocument,
            contentType: .json,
            defaultFilename: "timnor-backup",
        ) { result in
            switch result {
            case .success:
                statusMessage = "Sikkerhetskopi lagret"
            case .failure:
                statusMessage = "Kunne ikke lagre sikkerhetskopien"
            }
        }
        .fileExporter(
            isPresented: $isXlsxExportPresented,
            document: xlsxDocument,
            contentType: TimnorXlsxDocument.xlsxType,
            defaultFilename: "Timer",
        ) { result in
            switch result {
            case .success:
                statusMessage = "Excel-fil lagret"
            case .failure:
                statusMessage = "Kunne ikke lagre Excel-filen"
            }
        }
        .fileImporter(isPresented: $isImportPresented, allowedContentTypes: [.json]) { result in
            switch result {
            case .success(let url):
                restoreBackup(from: url)
            case .failure:
                statusMessage = "Kunne ikke åpne sikkerhetskopien"
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
            if let shareURL { TimnorActivityView(activityItems: [shareURL]) }
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
                guard counts.count == 2 else {
                    statusMessage = "Ugyldig sikkerhetskopi"
                    return
                }
                pendingBackupContents = contents
                restoreSummary = "\(counts[0]) prosjekter og \(counts[1]) registreringer"
                isRestoreConfirmationPresented = true
            }
        } catch {
            statusMessage = "Kunne ikke lese sikkerhetskopien"
        }
    }

    private func restoreBackup(contents: String) {
        IosTimnorPlatform.shared.restoreBackupV1(contents: contents) { _, error in
            statusMessage = error == nil ? "Sikkerhetskopi gjenopprettet" : "Gjenopprettingen mislyktes"
            pendingBackupContents = nil
        }
    }
}

private struct PlatformNavigation<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        if #available(iOS 16.0, *) {
            NavigationStack { content }
        } else {
            NavigationView { content }
                .navigationViewStyle(.stack)
        }
    }
}

private struct TimnorComposeHost: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private struct TimnorActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
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
