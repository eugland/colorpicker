import SwiftUI

struct CameraView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Image(systemName: "camera.viewfinder")
                    .font(.system(size: 48))
                    .foregroundStyle(.secondary)

                Text("Live camera sampling")
                    .font(.title3.weight(.semibold))

                Text("Use the camera to point at a color and capture its value.\nThis placeholder mirrors the Android live camera feature.")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
            }
            .padding()
            .navigationTitle("Camera")
        }
    }
}

#Preview {
    CameraView()
}
