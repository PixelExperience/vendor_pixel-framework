package vendor.google.google_battery.V1_2;

public interface IGoogleBattery extends vendor.google.google_battery.V1_1.IGoogleBattery {
    /**
     * Fully-qualified interface name for this interface.
     */
    public static final String kInterfaceName = "vendor.google.google_battery@1.2::IGoogleBattery";

    /**
     * Does a checked conversion from a binder to this class.
     */
    /* package private */ static IGoogleBattery asInterface(android.os.IHwBinder binder) {
        if (binder == null) {
            return null;
        }

        android.os.IHwInterface iface =
                binder.queryLocalInterface(kInterfaceName);

        if ((iface != null) && (iface instanceof IGoogleBattery)) {
            return (IGoogleBattery)iface;
        }

        IGoogleBattery proxy = new IGoogleBattery.Proxy(binder);

        try {
            for (String descriptor : proxy.interfaceChain()) {
                if (descriptor.equals(kInterfaceName)) {
                    return proxy;
                }
            }
        } catch (android.os.RemoteException e) {
        }

        return null;
    }

    /**
     * Does a checked conversion from any interface to this class.
     */
    public static IGoogleBattery castFrom(android.os.IHwInterface iface) {
        return (iface == null) ? null : IGoogleBattery.asInterface(iface.asBinder());
    }

    @Override
    public android.os.IHwBinder asBinder();

    /**
     * This will invoke the equivalent of the C++ getService(std::string) if retry is
     * true or tryGetService(std::string) if retry is false. If the service is
     * available on the device and retry is true, this will wait for the service to
     * start.
     *
     * @throws NoSuchElementException if this service is not available
     */
    public static IGoogleBattery getService(String serviceName, boolean retry) throws android.os.RemoteException {
        return IGoogleBattery.asInterface(android.os.HwBinder.getService("vendor.google.google_battery@1.2::IGoogleBattery", serviceName, retry));
    }

    /**
     * Calls getService("default",retry).
     */
    public static IGoogleBattery getService(boolean retry) throws android.os.RemoteException {
        return getService("default", retry);
    }

    /**
     * @throws NoSuchElementException if this service is not available
     * @deprecated this will not wait for the interface to come up if it hasn't yet
     * started. See getService(String,boolean) instead.
     */
    @Deprecated
    public static IGoogleBattery getService(String serviceName) throws android.os.RemoteException {
        return IGoogleBattery.asInterface(android.os.HwBinder.getService("vendor.google.google_battery@1.2::IGoogleBattery", serviceName));
    }

    /**
     * @throws NoSuchElementException if this service is not available
     * @deprecated this will not wait for the interface to come up if it hasn't yet
     * started. See getService(boolean) instead.
     */
    @Deprecated
    public static IGoogleBattery getService() throws android.os.RemoteException {
        return getService("default");
    }

    /**
     * Clears all battery defenders if triggered. This will send a clear command
     * to all battery defender algorithms.
     */
    byte clearBatteryDefender()
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getAdapterTypeCallback {
        public void onValues(byte result, int value);
    }

    /**
     * Retrieves an adapter type integer. This is determined from different sources.
     *
     * For example, for USB, this may derived from PID/VID. Uniqueness is not guaranteed,
     * but different integers will provide a rough estimate of different adapters.
     *
     */
    void getAdapterType(getAdapterTypeCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getAdapterIdCallback {
        public void onValues(byte result, int value);
    }

    /**
     * Retrieves an adapter ID integer. This is determined from different sources, but
     * may not be available. This may only be available on first party adapters.
     *
     */
    void getAdapterId(getAdapterIdCallback _hidl_cb)
        throws android.os.RemoteException;
    /**
     * Sets battery health charging always-on at a given SOC. You must remember to set at boot
     * if this feature is enabled in settings.
     *
     */
    byte setHealthAlwaysOn(int soc)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getHealthIndexCallback {
        public void onValues(byte result, int value);
    }

    /**
     * A value in the range [0, 100] representing the health of the battery.
     *
     * Value may be negative for errors.
     *
     */
    void getHealthIndex(getHealthIndexCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getHealthPerfIndexCallback {
        public void onValues(byte result, int value);
    }

    /**
     * A value in the range [0, 100] representing the performance of the battery.
     *
     * Value may be negative for errors.
     *
     */
    void getHealthPerfIndex(getHealthPerfIndexCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getHealthStatusCallback {
        public void onValues(byte result, byte value);
    }

    /**
     * Returns the current battery health status.
     */
    void getHealthStatus(getHealthStatusCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getChargingSpeedCallback {
        public void onValues(byte result, int value);
    }

    /**
     * Ratio between battery actual average current and battery actual current demand.
     *
     * The values are have a range [0, 100], or negative for errors.
     */
    void getChargingSpeed(getChargingSpeedCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getChargingTypeCallback {
        public void onValues(byte result, byte value);
    }

    /**
     * Returns the current charging type.
     */
    void getChargingType(getChargingTypeCallback _hidl_cb)
        throws android.os.RemoteException;

    @java.lang.FunctionalInterface
    public interface getChargingStatusCallback {
        public void onValues(byte result, byte value);
    }

    /**
     * Returns the current charging status.
     */
    void getChargingStatus(getChargingStatusCallback _hidl_cb)
        throws android.os.RemoteException;
    /*
     * Provides run-time type information for this object.
     * For example, for the following interface definition:
     *     package android.hardware.foo@1.0;
     *     interface IParent {};
     *     interface IChild extends IParent {};
     * Calling interfaceChain on an IChild object must yield the following:
     *     ["android.hardware.foo@1.0::IChild",
     *      "android.hardware.foo@1.0::IParent"
     *      "android.hidl.base@1.0::IBase"]
     *
     * @return descriptors a vector of descriptors of the run-time type of the
     *         object.
     */
    java.util.ArrayList<String> interfaceChain()
        throws android.os.RemoteException;
    /*
     * Emit diagnostic information to the given file.
     *
     * Optionally overriden.
     *
     * @param fd      File descriptor to dump data to.
     *                Must only be used for the duration of this call.
     * @param options Arguments for debugging.
     *                Must support empty for default debug information.
     */
    void debug(android.os.NativeHandle fd, java.util.ArrayList<String> options)
        throws android.os.RemoteException;
    /*
     * Provides run-time type information for this object.
     * For example, for the following interface definition:
     *     package android.hardware.foo@1.0;
     *     interface IParent {};
     *     interface IChild extends IParent {};
     * Calling interfaceDescriptor on an IChild object must yield
     *     "android.hardware.foo@1.0::IChild"
     *
     * @return descriptor a descriptor of the run-time type of the
     *         object (the first element of the vector returned by
     *         interfaceChain())
     */
    String interfaceDescriptor()
        throws android.os.RemoteException;
    /*
     * Returns hashes of the source HAL files that define the interfaces of the
     * runtime type information on the object.
     * For example, for the following interface definition:
     *     package android.hardware.foo@1.0;
     *     interface IParent {};
     *     interface IChild extends IParent {};
     * Calling interfaceChain on an IChild object must yield the following:
     *     [(hash of IChild.hal),
     *      (hash of IParent.hal)
     *      (hash of IBase.hal)].
     *
     * SHA-256 is used as the hashing algorithm. Each hash has 32 bytes
     * according to SHA-256 standard.
     *
     * @return hashchain a vector of SHA-1 digests
     */
    java.util.ArrayList<byte[/* 32 */]> getHashChain()
        throws android.os.RemoteException;
    /*
     * This method trigger the interface to enable/disable instrumentation based
     * on system property hal.instrumentation.enable.
     */
    void setHALInstrumentation()
        throws android.os.RemoteException;
    /*
     * Registers a death recipient, to be called when the process hosting this
     * interface dies.
     *
     * @param recipient a hidl_death_recipient callback object
     * @param cookie a cookie that must be returned with the callback
     * @return success whether the death recipient was registered successfully.
     */
    boolean linkToDeath(android.os.IHwBinder.DeathRecipient recipient, long cookie)
        throws android.os.RemoteException;
    /*
     * Provides way to determine if interface is running without requesting
     * any functionality.
     */
    void ping()
        throws android.os.RemoteException;
    /*
     * Get debug information on references on this interface.
     * @return info debugging information. See comments of DebugInfo.
     */
    android.hidl.base.V1_0.DebugInfo getDebugInfo()
        throws android.os.RemoteException;
    /*
     * This method notifies the interface that one or more system properties
     * have changed. The default implementation calls
     * (C++)  report_sysprop_change() in libcutils or
     * (Java) android.os.SystemProperties.reportSyspropChanged,
     * which in turn calls a set of registered callbacks (eg to update trace
     * tags).
     */
    void notifySyspropsChanged()
        throws android.os.RemoteException;
    /*
     * Unregisters the registered death recipient. If this service was registered
     * multiple times with the same exact death recipient, this unlinks the most
     * recently registered one.
     *
     * @param recipient a previously registered hidl_death_recipient callback
     * @return success whether the death recipient was unregistered successfully.
     */
    boolean unlinkToDeath(android.os.IHwBinder.DeathRecipient recipient)
        throws android.os.RemoteException;

    public static final class Proxy implements IGoogleBattery {
        private android.os.IHwBinder mRemote;

        public Proxy(android.os.IHwBinder remote) {
            mRemote = java.util.Objects.requireNonNull(remote);
        }

        @Override
        public android.os.IHwBinder asBinder() {
            return mRemote;
        }

        @Override
        public String toString() {
            try {
                return this.interfaceDescriptor() + "@Proxy";
            } catch (android.os.RemoteException ex) {
                /* ignored; handled below. */
            }
            return "[class or subclass of " + IGoogleBattery.kInterfaceName + "]@Proxy";
        }

        @Override
        public final boolean equals(java.lang.Object other) {
            return android.os.HidlSupport.interfacesEqual(this, other);
        }

        @Override
        public final int hashCode() {
            return this.asBinder().hashCode();
        }

        // Methods from ::vendor::google::google_battery::V1_0::IGoogleBattery follow.
        @Override
        public byte setChargingDeadline(int seconds)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_0.IGoogleBattery.kInterfaceName);
            _hidl_request.writeInt32(seconds);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(1 /* setChargingDeadline */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getChargingStageAndDeadline(getChargingStageAndDeadlineCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_0.IGoogleBattery.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(2 /* getChargingStageAndDeadline */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                String _hidl_out_stage = _hidl_reply.readString();
                int _hidl_out_seconds = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_stage, _hidl_out_seconds);
            } finally {
                _hidl_reply.release();
            }
        }

        // Methods from ::vendor::google::google_battery::V1_1::IGoogleBattery follow.
        @Override
        public byte setProperty(int feature, int property, int value)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_1.IGoogleBattery.kInterfaceName);
            _hidl_request.writeInt32(feature);
            _hidl_request.writeInt32(property);
            _hidl_request.writeInt32(value);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(3 /* setProperty */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getProperty(int feature, int property, getPropertyCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_1.IGoogleBattery.kInterfaceName);
            _hidl_request.writeInt32(feature);
            _hidl_request.writeInt32(property);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(4 /* getProperty */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        // Methods from ::vendor::google::google_battery::V1_2::IGoogleBattery follow.
        @Override
        public byte clearBatteryDefender()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(5 /* clearBatteryDefender */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getAdapterType(getAdapterTypeCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(6 /* getAdapterType */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getAdapterId(getAdapterIdCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(7 /* getAdapterId */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public byte setHealthAlwaysOn(int soc)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);
            _hidl_request.writeInt32(soc);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(8 /* setHealthAlwaysOn */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                return _hidl_out_result;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getHealthIndex(getHealthIndexCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(9 /* getHealthIndex */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getHealthPerfIndex(getHealthPerfIndexCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(10 /* getHealthPerfIndex */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getHealthStatus(getHealthStatusCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(11 /* getHealthStatus */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                byte _hidl_out_value = _hidl_reply.readInt8();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getChargingSpeed(getChargingSpeedCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(12 /* getChargingSpeed */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                int _hidl_out_value = _hidl_reply.readInt32();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getChargingType(getChargingTypeCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(13 /* getChargingType */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                byte _hidl_out_value = _hidl_reply.readInt8();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void getChargingStatus(getChargingStatusCallback _hidl_cb)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(14 /* getChargingStatus */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                byte _hidl_out_result = _hidl_reply.readInt8();
                byte _hidl_out_value = _hidl_reply.readInt8();
                _hidl_cb.onValues(_hidl_out_result, _hidl_out_value);
            } finally {
                _hidl_reply.release();
            }
        }

        // Methods from ::android::hidl::base::V1_0::IBase follow.
        @Override
        public java.util.ArrayList<String> interfaceChain()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256067662 /* interfaceChain */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                java.util.ArrayList<String> _hidl_out_descriptors = _hidl_reply.readStringVector();
                return _hidl_out_descriptors;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void debug(android.os.NativeHandle fd, java.util.ArrayList<String> options)
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);
            _hidl_request.writeNativeHandle(fd);
            _hidl_request.writeStringVector(options);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256131655 /* debug */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public String interfaceDescriptor()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256136003 /* interfaceDescriptor */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                String _hidl_out_descriptor = _hidl_reply.readString();
                return _hidl_out_descriptor;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public java.util.ArrayList<byte[/* 32 */]> getHashChain()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256398152 /* getHashChain */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                java.util.ArrayList<byte[/* 32 */]> _hidl_out_hashchain =  new java.util.ArrayList<byte[/* 32 */]>();
                {
                    android.os.HwBlob _hidl_blob = _hidl_reply.readBuffer(16 /* size */);
                    {
                        int _hidl_vec_size = _hidl_blob.getInt32(0 /* offset */ + 8 /* offsetof(hidl_vec<T>, mSize) */);
                        android.os.HwBlob childBlob = _hidl_reply.readEmbeddedBuffer(
                                _hidl_vec_size * 32,_hidl_blob.handle(),
                                0 /* offset */ + 0 /* offsetof(hidl_vec<T>, mBuffer) */,true /* nullable */);

                        ((java.util.ArrayList<byte[/* 32 */]>) _hidl_out_hashchain).clear();
                        for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                            byte[/* 32 */] _hidl_vec_element = new byte[32];
                            {
                                long _hidl_array_offset_1 = _hidl_index_0 * 32;
                                childBlob.copyToInt8Array(_hidl_array_offset_1, (byte[/* 32 */]) _hidl_vec_element, 32 /* size */);
                                _hidl_array_offset_1 += 32 * 1;
                            }
                            ((java.util.ArrayList<byte[/* 32 */]>) _hidl_out_hashchain).add(_hidl_vec_element);
                        }
                    }
                }
                return _hidl_out_hashchain;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void setHALInstrumentation()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256462420 /* setHALInstrumentation */, _hidl_request, _hidl_reply, 1 /* oneway */);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public boolean linkToDeath(android.os.IHwBinder.DeathRecipient recipient, long cookie)
                throws android.os.RemoteException {
            return mRemote.linkToDeath(recipient, cookie);
        }
        @Override
        public void ping()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(256921159 /* ping */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public android.hidl.base.V1_0.DebugInfo getDebugInfo()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(257049926 /* getDebugInfo */, _hidl_request, _hidl_reply, 0 /* flags */);
                _hidl_reply.verifySuccess();
                _hidl_request.releaseTemporaryStorage();

                android.hidl.base.V1_0.DebugInfo _hidl_out_info = new android.hidl.base.V1_0.DebugInfo();
                ((android.hidl.base.V1_0.DebugInfo) _hidl_out_info).readFromParcel(_hidl_reply);
                return _hidl_out_info;
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public void notifySyspropsChanged()
                throws android.os.RemoteException {
            android.os.HwParcel _hidl_request = new android.os.HwParcel();
            _hidl_request.writeInterfaceToken(android.hidl.base.V1_0.IBase.kInterfaceName);

            android.os.HwParcel _hidl_reply = new android.os.HwParcel();
            try {
                mRemote.transact(257120595 /* notifySyspropsChanged */, _hidl_request, _hidl_reply, 1 /* oneway */);
                _hidl_request.releaseTemporaryStorage();
            } finally {
                _hidl_reply.release();
            }
        }

        @Override
        public boolean unlinkToDeath(android.os.IHwBinder.DeathRecipient recipient)
                throws android.os.RemoteException {
            return mRemote.unlinkToDeath(recipient);
        }
    }

    public static abstract class Stub extends android.os.HwBinder implements IGoogleBattery {
        @Override
        public android.os.IHwBinder asBinder() {
            return this;
        }

        @Override
        public final java.util.ArrayList<String> interfaceChain() {
            return new java.util.ArrayList<String>(java.util.Arrays.asList(
                    vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName,
                    vendor.google.google_battery.V1_1.IGoogleBattery.kInterfaceName,
                    vendor.google.google_battery.V1_0.IGoogleBattery.kInterfaceName,
                    android.hidl.base.V1_0.IBase.kInterfaceName));

        }

        @Override
        public void debug(android.os.NativeHandle fd, java.util.ArrayList<String> options) {
            return;

        }

        @Override
        public final String interfaceDescriptor() {
            return vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName;

        }

        @Override
        public final java.util.ArrayList<byte[/* 32 */]> getHashChain() {
            return new java.util.ArrayList<byte[/* 32 */]>(java.util.Arrays.asList(
                    new byte[/* 32 */]{43,32,-122,65,-12,-100,73,-62,-54,-68,39,19,-30,-11,0,-118,-112,35,1,-10,-115,-10,-71,-12,43,-48,55,99,-111,10,-35,42} /* 2b208641f49c49c2cabc2713e2f5008a902301f68df6b9f42bd03763910add2a */,
                    new byte[/* 32 */]{-105,-82,90,107,-81,-74,72,-91,70,3,-52,-98,109,-82,45,-4,118,124,41,17,-97,-100,115,-43,-6,-69,66,67,30,23,99,-20} /* 97ae5a6bafb648a54603cc9e6dae2dfc767c29119f9c73d5fabb42431e1763ec */,
                    new byte[/* 32 */]{25,120,59,61,-7,79,-11,66,52,40,30,-64,74,38,109,15,90,-78,53,-83,74,28,-38,11,-49,5,102,-82,109,-74,84,90} /* 19783b3df94ff54234281ec04a266d0f5ab235ad4a1cda0bcf0566ae6db6545a */,
                    new byte[/* 32 */]{-20,127,-41,-98,-48,45,-6,-123,-68,73,-108,38,-83,-82,62,-66,35,-17,5,36,-13,-51,105,87,19,-109,36,-72,59,24,-54,76} /* ec7fd79ed02dfa85bc499426adae3ebe23ef0524f3cd6957139324b83b18ca4c */));

        }

        @Override
        public final void setHALInstrumentation() {

        }

        @Override
        public final boolean linkToDeath(android.os.IHwBinder.DeathRecipient recipient, long cookie) {
            return true;

        }

        @Override
        public final void ping() {
            return;

        }

        @Override
        public final android.hidl.base.V1_0.DebugInfo getDebugInfo() {
            android.hidl.base.V1_0.DebugInfo info = new android.hidl.base.V1_0.DebugInfo();
            info.pid = android.os.HidlSupport.getPidIfSharable();
            info.ptr = 0;
            info.arch = android.hidl.base.V1_0.DebugInfo.Architecture.UNKNOWN;
            return info;

        }

        @Override
        public final void notifySyspropsChanged() {
            android.os.HwBinder.enableInstrumentation();

        }

        @Override
        public final boolean unlinkToDeath(android.os.IHwBinder.DeathRecipient recipient) {
            return true;

        }

        @Override
        public android.os.IHwInterface queryLocalInterface(String descriptor) {
            if (kInterfaceName.equals(descriptor)) {
                return this;
            }
            return null;
        }

        public void registerAsService(String serviceName) throws android.os.RemoteException {
            registerService(serviceName);
        }

        @Override
        public String toString() {
            return this.interfaceDescriptor() + "@Stub";
        }

        @Override
        public void onTransact(int _hidl_code, android.os.HwParcel _hidl_request, final android.os.HwParcel _hidl_reply, int _hidl_flags)
                throws android.os.RemoteException {
            switch (_hidl_code) {
                case 1 /* setChargingDeadline */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_0.IGoogleBattery.kInterfaceName);

                    int seconds = _hidl_request.readInt32();
                    byte _hidl_out_result = setChargingDeadline(seconds);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt8(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 2 /* getChargingStageAndDeadline */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_0.IGoogleBattery.kInterfaceName);

                    getChargingStageAndDeadline(new getChargingStageAndDeadlineCallback() {
                        @Override
                        public void onValues(byte result, String stage, int seconds) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt8(result);
                            _hidl_reply.writeString(stage);
                            _hidl_reply.writeInt32(seconds);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 3 /* setProperty */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_1.IGoogleBattery.kInterfaceName);

                    int feature = _hidl_request.readInt32();
                    int property = _hidl_request.readInt32();
                    int value = _hidl_request.readInt32();
                    byte _hidl_out_result = setProperty(feature, property, value);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt8(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 4 /* getProperty */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_1.IGoogleBattery.kInterfaceName);

                    int feature = _hidl_request.readInt32();
                    int property = _hidl_request.readInt32();
                    getProperty(feature, property, new getPropertyCallback() {
                        @Override
                        public void onValues(byte result, int value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt8(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 5 /* clearBatteryDefender */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

                    byte _hidl_out_result = clearBatteryDefender();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt8(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 6 /* getAdapterType */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

                    getAdapterType(new getAdapterTypeCallback() {
                        @Override
                        public void onValues(byte result, int value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt8(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 7 /* getAdapterId */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

                    getAdapterId(new getAdapterIdCallback() {
                        @Override
                        public void onValues(byte result, int value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt8(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 8 /* setHealthAlwaysOn */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

                    int soc = _hidl_request.readInt32();
                    byte _hidl_out_result = setHealthAlwaysOn(soc);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeInt8(_hidl_out_result);
                    _hidl_reply.send();
                    break;
                }

                case 9 /* getHealthIndex */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

                    getHealthIndex(new getHealthIndexCallback() {
                        @Override
                        public void onValues(byte result, int value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt8(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 10 /* getHealthPerfIndex */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

                    getHealthPerfIndex(new getHealthPerfIndexCallback() {
                        @Override
                        public void onValues(byte result, int value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt8(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 11 /* getHealthStatus */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

                    getHealthStatus(new getHealthStatusCallback() {
                        @Override
                        public void onValues(byte result, byte value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt8(result);
                            _hidl_reply.writeInt8(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 12 /* getChargingSpeed */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

                    getChargingSpeed(new getChargingSpeedCallback() {
                        @Override
                        public void onValues(byte result, int value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt8(result);
                            _hidl_reply.writeInt32(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 13 /* getChargingType */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

                    getChargingType(new getChargingTypeCallback() {
                        @Override
                        public void onValues(byte result, byte value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt8(result);
                            _hidl_reply.writeInt8(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 14 /* getChargingStatus */:
                {
                    _hidl_request.enforceInterface(vendor.google.google_battery.V1_2.IGoogleBattery.kInterfaceName);

                    getChargingStatus(new getChargingStatusCallback() {
                        @Override
                        public void onValues(byte result, byte value) {
                            _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                            _hidl_reply.writeInt8(result);
                            _hidl_reply.writeInt8(value);
                            _hidl_reply.send();
                            }});
                    break;
                }

                case 256067662 /* interfaceChain */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    java.util.ArrayList<String> _hidl_out_descriptors = interfaceChain();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeStringVector(_hidl_out_descriptors);
                    _hidl_reply.send();
                    break;
                }

                case 256131655 /* debug */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    android.os.NativeHandle fd = _hidl_request.readNativeHandle();
                    java.util.ArrayList<String> options = _hidl_request.readStringVector();
                    debug(fd, options);
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 256136003 /* interfaceDescriptor */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    String _hidl_out_descriptor = interfaceDescriptor();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.writeString(_hidl_out_descriptor);
                    _hidl_reply.send();
                    break;
                }

                case 256398152 /* getHashChain */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    java.util.ArrayList<byte[/* 32 */]> _hidl_out_hashchain = getHashChain();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    {
                        android.os.HwBlob _hidl_blob = new android.os.HwBlob(16 /* size */);
                        {
                            int _hidl_vec_size = _hidl_out_hashchain.size();
                            _hidl_blob.putInt32(0 /* offset */ + 8 /* offsetof(hidl_vec<T>, mSize) */, _hidl_vec_size);
                            _hidl_blob.putBool(0 /* offset */ + 12 /* offsetof(hidl_vec<T>, mOwnsBuffer) */, false);
                            android.os.HwBlob childBlob = new android.os.HwBlob((int)(_hidl_vec_size * 32));
                            for (int _hidl_index_0 = 0; _hidl_index_0 < _hidl_vec_size; ++_hidl_index_0) {
                                {
                                    long _hidl_array_offset_1 = _hidl_index_0 * 32;
                                    byte[] _hidl_array_item_1 = (byte[/* 32 */]) _hidl_out_hashchain.get(_hidl_index_0);

                                    if (_hidl_array_item_1 == null || _hidl_array_item_1.length != 32) {
                                        throw new IllegalArgumentException("Array element is not of the expected length");
                                    }

                                    childBlob.putInt8Array(_hidl_array_offset_1, _hidl_array_item_1);
                                    _hidl_array_offset_1 += 32 * 1;
                                }
                            }
                            _hidl_blob.putBlob(0 /* offset */ + 0 /* offsetof(hidl_vec<T>, mBuffer) */, childBlob);
                        }
                        _hidl_reply.writeBuffer(_hidl_blob);
                    }
                    _hidl_reply.send();
                    break;
                }

                case 256462420 /* setHALInstrumentation */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    setHALInstrumentation();
                    break;
                }

                case 256660548 /* linkToDeath */:
                {
                break;
                }

                case 256921159 /* ping */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    ping();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    _hidl_reply.send();
                    break;
                }

                case 257049926 /* getDebugInfo */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    android.hidl.base.V1_0.DebugInfo _hidl_out_info = getDebugInfo();
                    _hidl_reply.writeStatus(android.os.HwParcel.STATUS_SUCCESS);
                    ((android.hidl.base.V1_0.DebugInfo) _hidl_out_info).writeToParcel(_hidl_reply);
                    _hidl_reply.send();
                    break;
                }

                case 257120595 /* notifySyspropsChanged */:
                {
                    _hidl_request.enforceInterface(android.hidl.base.V1_0.IBase.kInterfaceName);

                    notifySyspropsChanged();
                    break;
                }

                case 257250372 /* unlinkToDeath */:
                {
                break;
                }

            }
        }
    }
}
