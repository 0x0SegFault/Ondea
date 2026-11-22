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
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "UTC")!
        return c
    }()

    /// Today in the phone's time zone.
    static func today() -> Int64 {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        return from(year: c.year!, month: c.month!, day: c.day!)
    }

    static func from(date: Date) -> Int64 {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return from(year: c.year!, month: c.month!, day: c.day!)
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
        let c = utc.dateComponents([.year, .month, .day], from: date(day))
        return Calendar.current.date(from: c)!
    }

    static func format(_ day: Int64, template: String) -> String {
        let f = DateFormatter()
        f.timeZone = TimeZone(identifier: "UTC")
        f.locale = Locale.current
        f.setLocalizedDateFormatFromTemplate(template)
        return f.string(from: date(day))
    }

    static func dayMonth(_ day: Int64) -> String { format(day, template: "dMMM") }
    static func long(_ day: Int64) -> String { format(day, template: "EEEEdMMMM").capitalized(with: Locale.current) }
}
