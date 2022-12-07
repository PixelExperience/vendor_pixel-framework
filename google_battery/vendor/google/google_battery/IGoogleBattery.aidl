package vendor.google.google_battery;

import vendor.google.google_battery.BatteryHealthStatus;
import vendor.google.google_battery.ChargingStage;
import vendor.google.google_battery.ChargingStatus;
import vendor.google.google_battery.ChargingType;
import vendor.google.google_battery.DockDefendStatus;
import vendor.google.google_battery.Feature;

@VintfStability
interface IGoogleBattery {
    const int RESULT_IO_ERROR = 1;

    void setEnable(in Feature feature, in boolean enabled);

    void setChargingDeadline(in int seconds);

    ChargingStage getChargingStageAndDeadline();

    void setHealthAlwaysOn(in int soc);

    int getProperty(in Feature feature, in int property);

    void setProperty(in Feature feature, in int property, in int value);

    void clearBatteryDefender();

    int getAdapterId();

    int getAdapterType();

    int getChargingSpeed();

    ChargingStatus getChargingStatus();

    ChargingType getChargingType();

    int getHealthCapacityIndex();

    int getHealthImpedanceIndex();

    int getHealthIndex();

    BatteryHealthStatus getHealthStatus();

    DockDefendStatus getDockDefendStatus();
}
