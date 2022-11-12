package vendor.google.google_battery.V1_2;


public final class ChargingType {
    public static final byte NONE = 0;
    public static final byte FAULT = 1;
    public static final byte JEITA = 2;
    public static final byte LONG_LIFE = 3;
    public static final byte ADAPTIVE = 4;
    public static final byte NORMAL = 5;
    public static final String toString(byte o) {
        if (o == NONE) {
            return "NONE";
        }
        if (o == FAULT) {
            return "FAULT";
        }
        if (o == JEITA) {
            return "JEITA";
        }
        if (o == LONG_LIFE) {
            return "LONG_LIFE";
        }
        if (o == ADAPTIVE) {
            return "ADAPTIVE";
        }
        if (o == NORMAL) {
            return "NORMAL";
        }
        return "0x" + Integer.toHexString(Byte.toUnsignedInt((byte)(o)));
    }

    public static final String dumpBitfield(byte o) {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        byte flipped = 0;
        list.add("NONE"); // NONE == 0
        if ((o & FAULT) == FAULT) {
            list.add("FAULT");
            flipped |= FAULT;
        }
        if ((o & JEITA) == JEITA) {
            list.add("JEITA");
            flipped |= JEITA;
        }
        if ((o & LONG_LIFE) == LONG_LIFE) {
            list.add("LONG_LIFE");
            flipped |= LONG_LIFE;
        }
        if ((o & ADAPTIVE) == ADAPTIVE) {
            list.add("ADAPTIVE");
            flipped |= ADAPTIVE;
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

