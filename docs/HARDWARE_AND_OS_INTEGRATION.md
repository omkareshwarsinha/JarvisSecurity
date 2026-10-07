# MobiArmour Platform — Hardware, Sensor & Operating System Integration Guide

**Document Version:** 3.0.0-HardwareSpec  
**Classification:** Low-Level Android System Architecture  
**Target Environments:** Android AOSP, Samsung Knox, Google Pixel, Android Enterprise  

---

## 1. Hardware & System Services Mapping

| Subsystem | Android System Service | Java/Kotlin API Class | MobiArmour Usage & Interaction |
|---|---|---|---|
| **Virtual Network Device** | `VpnService` | `android.net.VpnService` | Creates virtual TUN network adapter (`tun0`) for per-app packet drop sinks and device air-gap isolation. |
| **Enterprise Policy Engine** | `DevicePolicyManager` | `android.app.admin.DevicePolicyManager` | Interrogates storage encryption status, enforces instant screen lock via `lockNow()`, and activates Device Admin profile. |
| **Keyguard & Biometrics** | `KeyguardManager` | `android.app.KeyguardManager` | Evaluates `isDeviceSecure()` to determine if hardware PIN, pattern, password, or biometric enrollment is active. |
| **Cellular Radio & SMS** | `TelephonyManager` / `SmsManager` | `android.telephony.SmsManager` | Intercepts inbound emergency SMS PDUs and transmits out-of-band coordinate and status telemetry. |
| **Audio Hardware** | `AudioManager` | `android.media.AudioManager` | Overrides silent and vibrate audio profiles via `STREAM_ALARM` to trigger high-decibel acoustic beacon. |
| **Location Hardware** | `LocationManager` | `android.location.LocationManager` | Interrogates GPS and Network provider cached fixes to locate lost or stolen endpoints. |
| **Power & Battery** | `BatteryManager` / `PowerManager` | `android.os.BatteryManager` | Evaluates battery level, charging state, and thermal state to modulate continuous security monitor frequency. |
| **Package Subsystem** | `PackageManager` | `android.content.pm.PackageManager` | Enumerates packages, queries APK manifest permissions, retrieves X.509 signatures, and verifies target SDK levels. |

---

## 2. Low-Level Linux & Kernel Subsystems Inspection

### 2.1 File System & Mount Checks
To detect root compromises without relying solely on standard Android API calls that may be hooked by Magisk or Zygisk, MobiArmour verifies filesystem anomalies:
- Probing mount points for writable `/system` or `/vendor` partitions (`mount -o remount,rw /system`).
- Testing for the existence and executability of standard su binaries across `/system/bin/su`, `/system/xbin/su`, `/sbin/su`, and `/system/sd/xbin/su`.
- Scanning for dangerous root management packages (`com.topjohnwu.magisk`, `eu.chainfire.supersu`, `me.weishu.kernelsu`).

### 2.2 SELinux Enforcement
- The system interrogates the SELinux operational mode. Devices operating in `Permissive` or `Disabled` mode instead of `Enforcing` present severe vulnerabilities where process isolation between application sandboxes is broken.

### 2.3 Kernel Build Tags
- `Build.TAGS` is verified against the `release-keys` standard. Custom or insecure vendor builds stamped with `test-keys` are flagged as critical posture warnings.

---

## 3. Battery & Thermal Optimization Strategies

Continuous background monitoring can rapidly degrade battery life if not designed with extreme discipline:
- **No Background Polling Loops:** Background checks utilize event-driven broadcasts (`ACTION_PACKAGE_ADDED`, `ACTION_PACKAGE_REMOVED`, `ACTION_BATTERY_CHANGED`) rather than infinite background worker sleep loops.
- **Batched Database Writes:** Network traffic drop records are batched and inserted in memory before writing to SQLite, keeping disk I/O low.
- **Sliding Window Pruning:** The network traffic table executes a retention limit of 500 rows during batch write operations, avoiding table fragmentation and unbounded database file growth.
