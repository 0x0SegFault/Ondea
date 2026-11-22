import SwiftUI

/// The cycle as a ring of day segments coloured by phase, with a marker on today.
struct CycleRing: View {
    let days: [String]
    let cycleDay: Int

    var body: some View {
        Canvas { context, size in
            draw(in: &context, size: size)
        }
        .accessibilityHidden(true)
    }

    private func draw(in context: inout GraphicsContext, size: CGSize) {
        let count = max(days.count, 1)
        let side: CGFloat = min(size.width, size.height)
        let stroke: CGFloat = side * 0.075
        let radius: CGFloat = (side - stroke) / 2 - stroke * 0.4
        let centre = CGPoint(x: size.width / 2, y: size.height / 2)
        let sweep: Double = 360.0 / Double(count)
        let gap: Double = count > 40 ? 0.8 : 1.6

        for (index, kind) in days.enumerated() {
            let from: Double = -90 + Double(index) * sweep + gap / 2
            let to: Double = -90 + Double(index + 1) * sweep - gap / 2
            var path = Path()
            path.addArc(center: centre, radius: radius,
                        startAngle: .degrees(from), endAngle: .degrees(to), clockwise: false)
            context.stroke(path, with: .color(Palette.day(kind)), lineWidth: stroke)
        }

        let angle: Double = (-90 + (Double(cycleDay) - 0.5) * sweep) * Double.pi / 180
        let markerX: CGFloat = centre.x + radius * CGFloat(cos(angle))
        let markerY: CGFloat = centre.y + radius * CGFloat(sin(angle))
        let markerRadius: CGFloat = stroke * 0.85
        let rect = CGRect(x: markerX - markerRadius, y: markerY - markerRadius,
                          width: 2 * markerRadius, height: 2 * markerRadius)
        let dot = Path(ellipseIn: rect)
        context.fill(dot, with: .color(.white))
        context.stroke(dot, with: .color(Palette.rose), lineWidth: stroke * 0.28)
    }
}
