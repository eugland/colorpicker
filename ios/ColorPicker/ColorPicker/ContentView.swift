import SwiftUI

struct ContentView: View {
    @StateObject private var library = ColorLibrary()

    var body: some View {
        TabView {
            PickerView()
                .tabItem {
                    Label("Pick", systemImage: "eyedropper")
                }

            PaletteView()
                .tabItem {
                    Label("Palettes", systemImage: "square.grid.2x2")
                }

            CameraView()
                .tabItem {
                    Label("Camera", systemImage: "camera")
                }

            ExploreView()
                .tabItem {
                    Label("Explore", systemImage: "sparkles")
                }
        }
        .environmentObject(library)
    }
}

#Preview {
    ContentView()
}
