import Foundation
import OSLog
import TimnorShared
import UIKit
import UserNotifications

@MainActor
final class TimnorStopwatchNotifications: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    private let center = UNUserNotificationCenter.current()
    private let timer = IosStopwatchPlatform.shared
    private let logger = Logger(subsystem: "no.timnor.ios", category: "Stopwatch notifications")
    private var desired: IosStopwatchNotificationState?
    private var needsUpdate = false
    private var canRequestPermission = false
    private var updateTask: Task<Void, Never>?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        center.delegate = self
        center.setNotificationCategories([
            category(StopwatchNotification.running, action: StopwatchNotification.pause, titleKey: "stopwatch.pause"),
            category(StopwatchNotification.paused, action: StopwatchNotification.resume, titleKey: "stopwatch.resume"),
        ])
        timer.setNotificationObserver { [weak self] state in
            self?.desired = state
            self?.refresh(requestPermission: state?.isRunning == true && UIApplication.shared.applicationState == .active)
        }
        return true
    }

    func setAppActive(_ active: Bool) {
        timer.setAppActive(active: active)
        if active {
            refresh(requestPermission: desired?.isRunning == true)
        }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        notification.request.identifier == StopwatchNotification.identifier ? [.list] : []
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let request = response.notification.request
        let action = response.actionIdentifier
        guard request.identifier == StopwatchNotification.identifier,
              let revision = request.content.userInfo[StopwatchNotification.revisionKey] as? String,
              action == StopwatchNotification.pause || action == StopwatchNotification.resume else { return }
        await applyAction(revision: revision, resume: action == StopwatchNotification.resume)
    }

    private func applyAction(revision: String, resume: Bool) async {
        timer.handleNotificationAction(revision: revision, resume: resume)
        await refresh().value
    }

    @discardableResult
    private func refresh(requestPermission: Bool = false) -> Task<Void, Never> {
        needsUpdate = true
        canRequestPermission = canRequestPermission || requestPermission
        if let updateTask { return updateTask }

        let task = Task {
            defer { updateTask = nil }
            while needsUpdate {
                needsUpdate = false
                let requestPermission = canRequestPermission
                canRequestPermission = false
                do {
                    try await updateNotification(requestPermission: requestPermission)
                } catch {
                    logger.error("Could not update stopwatch notification: \(error.localizedDescription, privacy: .public)")
                }
            }
        }
        updateTask = task
        return task
    }

    private func updateNotification(requestPermission: Bool) async throws {
        let identifier = StopwatchNotification.identifier
        guard let state = desired else {
            center.removePendingNotificationRequests(withIdentifiers: [identifier])
            center.removeDeliveredNotifications(withIdentifiers: [identifier])
            return
        }

        let settings = await center.notificationSettings()
        guard state.revision == desired?.revision else { return }
        switch settings.authorizationStatus {
        case .notDetermined:
            guard requestPermission, try await center.requestAuthorization(options: [.alert]) else { return }
        case .authorized, .provisional, .ephemeral:
            break
        default:
            return
        }

        let delivered = await center.deliveredNotifications()
        guard state.revision == desired?.revision else { return }
        if delivered.contains(where: {
            $0.request.identifier == identifier &&
                $0.request.content.userInfo[StopwatchNotification.revisionKey] as? String == state.revision
        }) { return }

        let content = UNMutableNotificationContent()
        content.title = NSLocalizedString("stopwatch.title", comment: "Stopwatch notification title")
        content.body = NSLocalizedString(
            state.isRunning ? "stopwatch.running" : "stopwatch.paused",
            comment: "Stopwatch notification status"
        )
        content.categoryIdentifier = state.isRunning ? StopwatchNotification.running : StopwatchNotification.paused
        content.userInfo = [StopwatchNotification.revisionKey: state.revision]
        content.interruptionLevel = .passive
        try await center.add(UNNotificationRequest(identifier: identifier, content: content, trigger: nil))
    }

    private func category(_ identifier: String, action: String, titleKey: String) -> UNNotificationCategory {
        UNNotificationCategory(
            identifier: identifier,
            actions: [UNNotificationAction(
                identifier: action,
                title: NSLocalizedString(titleKey, comment: "Stopwatch notification action"),
                options: []
            )],
            intentIdentifiers: []
        )
    }
}

private enum StopwatchNotification {
    static let identifier = "timnor.stopwatch"
    static let running = "timnor.stopwatch.running"
    static let paused = "timnor.stopwatch.paused"
    static let pause = "timnor.stopwatch.pause"
    static let resume = "timnor.stopwatch.resume"
    static let revisionKey = "revision"
}
