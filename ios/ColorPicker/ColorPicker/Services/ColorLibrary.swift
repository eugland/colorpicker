import SwiftUI
import UIKit

final class ColorLibrary: ObservableObject {
    @Published var colors: [ColorEntry] = []

    init() {
        loadColors()
    }

    func loadColors() {
        guard let url = Bundle.main.url(forResource: "colors", withExtension: "json") else {
            colors = []
            return
        }

        do {
            let data = try Data(contentsOf: url)
            let decoded = try JSONDecoder().decode([ColorEntry].self, from: data)
            colors = decoded
        } catch {
            colors = []
        }
    }

    func nearestColor(to color: Color) -> ColorEntry? {
        guard !colors.isEmpty else { return nil }

        let target = color.rgbComponents
        return colors.min { lhs, rhs in
            let lhsDelta = lhs.color.rgbComponents.distance(to: target)
            let rhsDelta = rhs.color.rgbComponents.distance(to: target)
            return lhsDelta < rhsDelta
        }
    }
}

struct RGBComponents: Hashable {
    let red: Double
    let green: Double
    let blue: Double

    func distance(to other: RGBComponents) -> Double {
        let dr = red - other.red
        let dg = green - other.green
        let db = blue - other.blue
        return dr * dr + dg * dg + db * db
    }
}

extension Color {
    var rgbComponents: RGBComponents {
        let uiColor = UIColor(self)
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0
        uiColor.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        return RGBComponents(red: Double(red), green: Double(green), blue: Double(blue))
    }
}
