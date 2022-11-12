package vendor.google.google_battery.V1_0;


public final class Result {
    public static final byte OK = 0;
    public static final byte ERROR_INVALID_PARAMETER = 1 /* ::vendor::google::google_battery::V1_0::Result.OK implicitly + 1 */;
    public static final byte ERROR_IO = 2 /* ::vendor::google::google_battery::V1_0::Result.ERROR_INVALID_PARAMETER implicitly + 1 */;
    public static final String toString(byte o) {
        if (o == OK) {
            return "OK";
        }
        if (o == ERROR_INVALID_PARAMETER) {
            return "ERROR_INVALID_PARAMETER";
        }
        if (o == ERROR_IO) {
            return "ERROR_IO";
        }
        return "0x" + Integer.toHexString(Byte.toUnsignedInt((byte)(o)));
    }

    public static final String dumpBitfield(byte o) {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        byte flipped = 0;
        list.add("OK"); // OK == 0
        if ((o & ERROR_INVALID_PARAMETER) == ERROR_INVALID_PARAMETER) {
            list.add("ERROR_INVALID_PARAMETER");
            flipped |= ERROR_INVALID_PARAMETER;
        }
        if ((o & ERROR_IO) == ERROR_IO) {
            list.add("ERROR_IO");
            flipped |= ERROR_IO;
        }
        if (o != flipped) {
            list.add("0x" + Integer.toHexString(Byte.toUnsignedInt((byte)(o & (~flipped)))));
        }
        return String.join(" | ", list);
    }

};

