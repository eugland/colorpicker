import SwiftUI

struct ExploreView: View {
    @EnvironmentObject private var library: ColorLibrary

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    InfoCard(
                        title: "Color inspiration",
                        subtitle: "Browse curated shades that match today’s mood.",
                        icon: "sparkles"
                    )

                    InfoCard(
                        title: "Palette tips",
                        subtitle: "Pair warm and cool tones for contrast, or stay monochrome for calm.",
                        icon: "paintpalette"
                    )

                    if let featured = library.colors.randomElement() {
                        FeaturedColor(entry: featured)
                    }
                }
                .padding()
            }
            .navigationTitle("Explore")
        }
    }
}

private struct InfoCard: View {
    let title: String
    let subtitle: String
    let icon: String

    var body: some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 28))
                .foregroundStyle(.accent)
                .frame(width: 44, height: 44)
                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12))

            VStack(alignment: .leading, spacing: 6) {
                Text(title)
                    .font(.headline)
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Spacer()
        }
        .padding()
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
    }
}

private struct FeaturedColor: View {
    let entry: ColorEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Featured shade")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)

            HStack(spacing: 16) {
                RoundedRectangle(cornerRadius: 12)
                    .fill(entry.color)
                    .frame(width: 68, height: 68)

                VStack(alignment: .leading, spacing: 4) {
                    Text(entry.name)
                        .font(.headline)
                    Text(entry.hex)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                Spacer()
            }
        }
        .padding()
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 16))
    }
}

#Preview {
    ExploreView()
        .environmentObject(ColorLibrary())
}
