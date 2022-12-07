package vendor.google.google_battery;

@VintfStability
@Backing(type="int")
enum Feature {
    ADAPTIVE_CHARGING = 0,
    TEMP_DEFEND = 1,
    TRICKLE_DEFEND = 2,
    DWELL_DEFEND = 3,
    DREAM_DEFEND = 4,
    DC_CHARGING = 5,
    AGE_ADJUSTED_CHARGE_RATE = 6,
    DOCK_DEFEND = 7,
    BATTERY_HEALTH_INDEX = 8,
    CHARGING_SPEED_INDICATOR = 9,
    FEATURE_MAX,
}
