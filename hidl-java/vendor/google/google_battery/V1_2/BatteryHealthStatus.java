package vendor.google.google_battery.V1_2;


public final class BatteryHealthStatus {
    public static final byte UNKNOWN = -1 /* -1 */;
    public static final byte NOMINAL = 0;
    public static final byte MARGINAL = 1;
    public static final byte NEEDS_REPLACEMENT = 2;
    public static final byte FAILED = 3;
    public static final String toString(byte o) {
        if (o == UNKNOWN) {
            return "UNKNOWN";
        }
        if (o == NOMINAL) {
            return "NOMINAL";
        }
        if (o == MARGINAL) {
            return "MARGINAL";
        }
        if (o == NEEDS_REPLACEMENT) {
            return "NEEDS_REPLACEMENT";
        }
        if (o == FAILED) {
            return "FAILED";
        }
        return "0x" + Integer.toHexString(Byte.toUnsignedInt((byte)(o)));
    }

    public static final String dumpBitfield(byte o) {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        byte flipped = 0;
        if ((o & UNKNOWN) == UNKNOWN) {
            list.add("UNKNOWN");
            flipped |= UNKNOWN;
        }
        list.add("NOMINAL"); // NOMINAL == 0
        if ((o & MARGINAL) == MARGINAL) {
            list.add("MARGINAL");
            flipped |= MARGINAL;
        }
        if ((o & NEEDS_REPLACEMENT) == NEEDS_REPLACEMENT) {
            list.add("NEEDS_REPLACEMENT");
            flipped |= NEEDS_REPLACEMENT;
        }
        if ((o & FAILED) == FAILED) {
            list.add("FAILED");
            flipped |= FAILED;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString(Byte.toUnsignedInt((byte)(o & (~flipped)))));
        }
        return String.join(" | ", list);
    }

};

