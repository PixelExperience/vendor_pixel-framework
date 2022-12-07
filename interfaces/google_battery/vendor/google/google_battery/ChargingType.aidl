package vendor.google.google_battery;

@VintfStability
@Backing(type="int")
enum ChargingType {
    UNKNOWN = -1,
    NONE = 0, // Disconnected
    FAULT = 1, // Internal Failures
    JEITA = 2, // HW limits
    LONG_LIFE = 3, // Defender Conditions
    ADAPTIVE = 4, // Adaptive Charging
    NORMAL = 5,
}
