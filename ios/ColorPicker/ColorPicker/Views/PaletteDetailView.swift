import SwiftUI

struct PaletteDetailView: View {
    let entry: ColorEntry

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                RoundedRectangle(cornerRadius: 24)
                    .fill(entry.color)
                    .frame(height: 220)
                    .overlay(
                        VStack(spacing: 8) {
                            Text(entry.name)
                                .font(.title2.bold())
                            Text(entry.hex)
                                .font(.headline)
                        }
                        .foregroundStyle(entry.color.accessibleTextColor)
                    )

                VStack(alignment: .leading, spacing: 16) {
                    DetailRow(label: "RGB", value: entry.color.rgbString)
                    DetailRow(label: "Contrast", value: entry.color.contrastRatioString)
                    DetailRow(label: "Accessibility", value: entry.color.accessibilitySummary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
            }
            .padding()
        }
        .navigationTitle(entry.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct DetailRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
                .font(.body)
        }
    }
}

#Preview {
    PaletteDetailView(entry: ColorEntry(name: "Azure", hex: "#007AFF"))
}
