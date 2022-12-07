package vendor.google.google_battery;

@VintfStability
@Backing(type="int")
enum BatteryHealthStatus {
    UNKNOWN = -1,
    NOMINAL = 0,
    MARGINAL = 1,
    NEEDS_REPLACEMENT = 2,
    FAILED = 3,
}
