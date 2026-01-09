import Foundation
import SwiftUI

struct ColorEntry: Identifiable, Codable, Hashable {
    let id: UUID
    let name: String
    let hex: String

    init(id: UUID = UUID(), name: String, hex: String) {
        self.id = id
        self.name = name
        self.hex = hex
    }

    var color: Color {
        Color(hex: hex)
    }

    private enum CodingKeys: String, CodingKey {
        case name
        case hex
    }
}

extension Color {
    init(hex: String) {
        let sanitized = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var value: UInt64 = 0
        Scanner(string: sanitized).scanHexInt64(&value)

        let r, g, b: Double
        switch sanitized.count {
        case 6:
            r = Double((value & 0xFF0000) >> 16) / 255.0
            g = Double((value & 0x00FF00) >> 8) / 255.0
            b = Double(value & 0x0000FF) / 255.0
        case 3:
            r = Double((value & 0xF00) >> 8) / 15.0
            g = Double((value & 0x0F0) >> 4) / 15.0
            b = Double(value & 0x00F) / 15.0
        default:
            r = 0.5
            g = 0.5
            b = 0.5
        }

        self.init(.sRGB, red: r, green: g, blue: b, opacity: 1.0)
    }
}
