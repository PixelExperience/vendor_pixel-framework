package vendor.google.google_battery.V1_2;


public final class ChargingStatus {
    public static final byte DISCHARGING = 0;
    public static final byte HEALTH_COLD = 10;
    public static final byte HEALTH_HOT = 11;
    public static final byte SYSTEM_THERMAL = 20;
    public static final byte SYSTEM_LOAD = 21;
    public static final byte ADAPTER_POWER = 30;
    public static final byte ADAPTER_QUALITY = 31;
    public static final byte ADAPTER_AUTH = 32;
    public static final byte NORMAL = 100;
    public static final String toString(byte o) {
        if (o == DISCHARGING) {
            return "DISCHARGING";
        }
        if (o == HEALTH_COLD) {
            return "HEALTH_COLD";
        }
        if (o == HEALTH_HOT) {
            return "HEALTH_HOT";
        }
        if (o == SYSTEM_THERMAL) {
            return "SYSTEM_THERMAL";
        }
        if (o == SYSTEM_LOAD) {
            return "SYSTEM_LOAD";
        }
        if (o == ADAPTER_POWER) {
            return "ADAPTER_POWER";
        }
        if (o == ADAPTER_QUALITY) {
            return "ADAPTER_QUALITY";
        }
        if (o == ADAPTER_AUTH) {
            return "ADAPTER_AUTH";
        }
        if (o == NORMAL) {
            return "NORMAL";
        }
        return "0x" + Integer.toHexString(Byte.toUnsignedInt((byte)(o)));
    }

    public static final String dumpBitfield(byte o) {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        byte flipped = 0;
        list.add("DISCHARGING"); // DISCHARGING == 0
        if ((o & HEALTH_COLD) == HEALTH_COLD) {
            list.add("HEALTH_COLD");
            flipped |= HEALTH_COLD;
        }
        if ((o & HEALTH_HOT) == HEALTH_HOT) {
            list.add("HEALTH_HOT");
            flipped |= HEALTH_HOT;
        }
        if ((o & SYSTEM_THERMAL) == SYSTEM_THERMAL) {
            list.add("SYSTEM_THERMAL");
            flipped |= SYSTEM_THERMAL;
        }
        if ((o & SYSTEM_LOAD) == SYSTEM_LOAD) {
            list.add("SYSTEM_LOAD");
            flipped |= SYSTEM_LOAD;
        }
        if ((o & ADAPTER_POWER) == ADAPTER_POWER) {
            list.add("ADAPTER_POWER");
            flipped |= ADAPTER_POWER;
        }
        if ((o & ADAPTER_QUALITY) == ADAPTER_QUALITY) {
            list.add("ADAPTER_QUALITY");
            flipped |= ADAPTER_QUALITY;
        }
        if ((o & ADAPTER_AUTH) == ADAPTER_AUTH) {
            list.add("ADAPTER_AUTH");
            flipped |= ADAPTER_AUTH;
        }
        if ((o & NORMAL) == NORMAL) {
            list.add("NORMAL");
            flipped |= NORMAL;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString(Byte.toUnsignedInt((byte)(o & (~flipped)))));
        }
        return String.join(" | ", list);
    }

};

