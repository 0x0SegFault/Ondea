import SwiftUI

/// The cycle as a ring of day segments coloured by phase, with a marker on today.
struct CycleRing: View {
    let days: [String]
    let cycleDay: Int

    var body: some View {
        Canvas { context, size in
            let count = max(days.count, 1)
            let side = min(size.width, size.height)
            let stroke = side * 0.075
            let radius = (side - stroke) / 2 - stroke * 0.4
            let centre = CGPoint(x: size.width / 2, y: size.height / 2)
            let sweep = 360.0 / Double(count)
            let gap = count > 40 ? 0.8 : 1.6
            for (i, kind) in days.enumerated() {
                let startAngle = Angle(degrees: -90 + Double(i) * sweep + gap / 2)
                let endAngle = Angle(degrees: -90 + Double(i + 1) * sweep - gap / 2)
                var path = Path()
                path.addArc(center: centre, radius: radius, startAngle: startAngle, endAngle: endAngle, clockwise: false)
                context.stroke(path, with: .color(Palette.day(kind)), lineWidth: stroke)
            }
            let angle = (-90 + (Double(cycleDay) - 0.5) * sweep) * .pi / 180
            let marker = CGPoint(x: centre.x + radius * cos(angle), y: centre.y + radius * sin(angle))
            let r = stroke * 0.85
            let dot = Path(ellipseIn: CGRect(x: marker.x - r, y: marker.y - r, width: 2 * r, height: 2 * r))
            context.fill(dot, with: .color(.white))
            context.stroke(dot, with: .color(Palette.rose), lineWidth: stroke * 0.28)
        }
        .accessibilityHidden(true)
    }
}
