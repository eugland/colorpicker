import SwiftUI
import UIKit

extension Color {
    var hexString: String {
        let uiColor = UIColor(self)
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0
        uiColor.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

        let r = Int(round(red * 255))
        let g = Int(round(green * 255))
        let b = Int(round(blue * 255))
        return String(format: "#%02X%02X%02X", r, g, b)
    }

    var accessibleTextColor: Color {
        let components = rgbComponents
        let luminance = (0.299 * components.red) + (0.587 * components.green) + (0.114 * components.blue)
        return luminance > 0.6 ? .black : .white
    }
}

extension Color {
    var rgbString: String {
        let components = rgbComponents
        let r = Int(round(components.red * 255))
        let g = Int(round(components.green * 255))
        let b = Int(round(components.blue * 255))
        return "\(r), \(g), \(b)"
    }

    var contrastRatioString: String {
        let ratio = contrastRatio(with: accessibleTextColor)
        return String(format: "%.2f:1", ratio)
    }

    var accessibilitySummary: String {
        let ratio = contrastRatio(with: accessibleTextColor)
        return ratio >= 4.5 ? "AA" : "Needs contrast"
    }

    func contrastRatio(with other: Color) -> Double {
        let l1 = relativeLuminance + 0.05
        let l2 = other.relativeLuminance + 0.05
        return max(l1, l2) / min(l1, l2)
    }

    var relativeLuminance: Double {
        let components = rgbComponents
        func adjust(_ value: Double) -> Double {
            value <= 0.03928 ? value / 12.92 : pow((value + 0.055) / 1.055, 2.4)
        }
        let r = adjust(components.red)
        let g = adjust(components.green)
        let b = adjust(components.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
