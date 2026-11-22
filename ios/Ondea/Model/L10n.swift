import Foundation

/// Texts come from the shared translations (generated from the Android strings).
enum L10n {
    static func t(_ key: String) -> String {
        NSLocalizedString(key, comment: "")
    }

    static func t(_ key: String, _ args: CVarArg...) -> String {
        String(format: NSLocalizedString(key, comment: ""), locale: Locale.current, arguments: args)
    }

    /// Plural texts: the first number argument selects the form unless the strings file says otherwise.
    static func plural(_ key: String, _ args: CVarArg...) -> String {
        String(format: NSLocalizedString(key, comment: ""), locale: Locale.current, arguments: args)
    }
}

/// Dates as epoch days (days since 1970-01-01), the format of the shared code.
enum EpochDay {
    private static let utc: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        return calendar
    }()

    /// Today in the phone's time zone.
    static func today() -> Int64 {
        let parts = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        return from(year: parts.year!, month: parts.month!, day: parts.day!)
    }

    static func from(date: Date) -> Int64 {
        let parts = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return from(year: parts.year!, month: parts.month!, day: parts.day!)
    }

    static func from(year: Int, month: Int, day: Int) -> Int64 {
        let date = utc.date(from: DateComponents(year: year, month: month, day: day))!
        return Int64(floor(date.timeIntervalSince1970 / 86_400))
    }

    /// Midnight UTC of the epoch day, for formatting with a UTC formatter.
    static func date(_ day: Int64) -> Date {
        Date(timeIntervalSince1970: TimeInterval(day) * 86_400)
    }

    /// Local calendar date (for DatePicker) of an epoch day.
    static func localDate(_ day: Int64) -> Date {
        let parts = utc.dateComponents([.year, .month, .day], from: date(day))
        return Calendar.current.date(from: parts)!
    }

    static func format(_ day: Int64, template: String) -> String {
        let formatter = DateFormatter()
        formatter.timeZone = TimeZone(identifier: "UTC")
        formatter.locale = Locale.current
        formatter.setLocalizedDateFormatFromTemplate(template)
        return formatter.string(from: date(day))
    }

    static func dayMonth(_ day: Int64) -> String { format(day, template: "dMMM") }
    static func long(_ day: Int64) -> String { format(day, template: "EEEEdMMMM").capitalized(with: Locale.current) }
}
