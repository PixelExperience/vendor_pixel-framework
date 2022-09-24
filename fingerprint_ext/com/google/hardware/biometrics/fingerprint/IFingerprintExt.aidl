package com.google.hardware.biometrics.fingerprint;

@VintfStability
oneway interface IFingerprintExt {
    void onPointerDown(in long pointerId, in int x, in int y, in float minor, in float major);
    void onPointerUp(in long pointerId);
    void onUiReady();
}
