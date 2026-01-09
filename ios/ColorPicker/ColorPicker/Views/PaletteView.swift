import SwiftUI

struct PaletteView: View {
    @EnvironmentObject private var library: ColorLibrary
    @State private var searchText = ""

    var body: some View {
        NavigationStack {
            List(filteredColors) { entry in
                NavigationLink(value: entry) {
                    HStack(spacing: 16) {
                        RoundedRectangle(cornerRadius: 10)
                            .fill(entry.color)
                            .frame(width: 44, height: 44)

                        VStack(alignment: .leading, spacing: 4) {
                            Text(entry.name)
                                .font(.headline)
                            Text(entry.hex)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("Palettes")
            .searchable(text: $searchText, placement: .navigationBarDrawer(displayMode: .automatic))
            .navigationDestination(for: ColorEntry.self) { entry in
                PaletteDetailView(entry: entry)
            }
        }
    }

    private var filteredColors: [ColorEntry] {
        guard !searchText.isEmpty else { return library.colors }
        return library.colors.filter { entry in
            entry.name.localizedCaseInsensitiveContains(searchText) ||
                entry.hex.localizedCaseInsensitiveContains(searchText)
        }
    }
}

#Preview {
    PaletteView()
        .environmentObject(ColorLibrary())
}
