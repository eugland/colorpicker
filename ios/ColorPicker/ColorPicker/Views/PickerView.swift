import SwiftUI

struct PickerView: View {
    @EnvironmentObject private var library: ColorLibrary
    @State private var selectedColor: Color = .blue
    @State private var selectionName: String = "Custom"

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                RoundedRectangle(cornerRadius: 24)
                    .fill(selectedColor)
                    .frame(height: 180)
                    .overlay(
                        VStack(spacing: 8) {
                            Text(selectionName)
                                .font(.headline)
                                .foregroundStyle(selectedColor.accessibleTextColor)
                            Text(selectedColor.hexString)
                                .font(.subheadline)
                                .foregroundStyle(selectedColor.accessibleTextColor)
                        }
                    )

                ColorPicker("Pick a color", selection: $selectedColor)
                    .onChange(of: selectedColor) { _, newValue in
                        let match = library.nearestColor(to: newValue)
                        selectionName = match?.name ?? "Custom"
                    }
                    .padding(.horizontal)

                if let match = library.nearestColor(to: selectedColor) {
                    ColorMatchCard(entry: match)
                }

                Spacer()
            }
            .padding()
            .navigationTitle("Pick Color")
        }
    }
}

private struct ColorMatchCard: View {
    let entry: ColorEntry

    var body: some View {
        HStack(spacing: 16) {
            Circle()
                .fill(entry.color)
                .frame(width: 56, height: 56)

            VStack(alignment: .leading, spacing: 4) {
                Text(entry.name)
                    .font(.headline)
                Text(entry.hex)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Spacer()
        }
        .padding()
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
    }
}

#Preview {
    PickerView()
        .environmentObject(ColorLibrary())
}
