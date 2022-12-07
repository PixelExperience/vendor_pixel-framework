package vendor.google.google_battery;

@VintfStability
@Backing(type="int")
enum DockDefendStatus {
    OVERRIDE_DISABLED = -2,
    SETTINGS_DISABLED = -1,
    ENABLED = 0,
    TRIGGERED = 1,
}
