import SwiftUI

@main
struct TimnorApp: App {
    @UIApplicationDelegateAdaptor(TimnorStopwatchNotifications.self) private var stopwatchNotifications
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            TimnorContentView()
                .onAppear { stopwatchNotifications.setAppActive(scenePhase == .active) }
                .onChange(of: scenePhase) { phase in
                    stopwatchNotifications.setAppActive(phase == .active)
                }
        }
    }
}
