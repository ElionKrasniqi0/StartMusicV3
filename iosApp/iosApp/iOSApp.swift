import AVFoundation
import SwiftUI
import UIKit
import YMusicApp

// StartMusicV3 iOS shell: SwiftUI hosts MainViewControllerKt.MainViewController() from the
// :iosAppKotlin YMusicApp framework. Shared Compose App over AVFoundation local + MSE YouTube.

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // Let audio keep playing when the app is backgrounded/locked.
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
        IosPipHostKt.iosSetPipPrepare {
            IosPip.shared.prepare()
        }
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea()
                .onAppear { IosPip.shared.prepare() }
                .onOpenURL { url in IosLinksKt.iosOpenUrl(url: url.absoluteString) }
                .onChange(of: scenePhase) { phase in
                    switch phase {
                    case .inactive:
                        // Start while still presenting so PiP is not a still of the last frame.
                        IosPip.shared.startIfEligible()
                    case .background:
                        IosPip.shared.noteLeftWithoutPip()
                    case .active:
                        IosPipHostKt.iosOnEnterForeground()
                        IosPip.shared.prepare()
                        DispatchQueue.main.async {
                            IosPip.shared.recoverIfNeeded()
                        }
                    default:
                        break
                    }
                }
        }
    }
}
