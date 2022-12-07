///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package vendor.google.google_battery;
@Backing(type="int") @VintfStability
enum ChargingStatus {
  UNKNOWN = -1,
  HEALTH_COLD = 10,
  HEALTH_HOT = 11,
  SYSTEM_THERMAL = 20,
  SYSTEM_LOAD = 21,
  ADAPTER_AUTH = 30,
  ADAPTER_POWER = 31,
  ADAPTER_QUALITY = 32,
  DEFENDER_TEMP = 40,
  DEFENDER_DWELL = 41,
  DEFENDER_TRICKLE = 42,
  DEFENDER_DOCK = 43,
  NOT_CHARGING = 100,
  CHARGING = 200,
}
