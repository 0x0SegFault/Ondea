import SwiftUI

/// Same meaning colours as the Android app.
enum Palette {
    static let rose = Color(red: 0.76, green: 0.25, blue: 0.36)
    static let period = Color(red: 0.88, green: 0.29, blue: 0.40)
    static let fertile = Color(red: 0.25, green: 0.71, blue: 0.64)
    static let peak = Color(red: 0.08, green: 0.53, blue: 0.48)
    static let ovulation = Color(red: 0.54, green: 0.36, blue: 0.82)
    static let neutral = Color(red: 0.95, green: 0.89, blue: 0.90)

    static func tone(_ tone: String) -> Color {
        switch tone {
        case "PERIOD": return period
        case "OVULATION": return ovulation
        case "PEAK": return peak
        case "FERTILE": return fertile
        default: return rose
        }
    }

    /// Colour of one day on the ring, from the shared day kind.
    static func day(_ kind: String) -> Color {
        switch kind {
        case "period": return period
        case "ovulation": return ovulation
        case "ovulationPredicted": return ovulation.opacity(0.55)
        case "peak": return peak
        case "fertile": return fertile
        case "periodPredicted": return period.opacity(0.4)
        case "peakPredicted": return peak.opacity(0.5)
        case "fertilePredicted": return fertile.opacity(0.45)
        default: return neutral
        }
    }
}

/// White rounded card, like SectionCard on Android.
struct Card<Content: View>: View {
    var title: String?
    var systemImage: String?
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            if let title {
                Label(title, systemImage: systemImage ?? "circle").font(.headline)
            }
            content
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 24))
        .shadow(color: .black.opacity(0.05), radius: 3, y: 1)
    }
}
