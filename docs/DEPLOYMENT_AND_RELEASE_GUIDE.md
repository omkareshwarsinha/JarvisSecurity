# MobiArmour Platform — Production Deployment, Release & Hardening Checklist

**Document Version:** 3.0.0-DeploymentChecklist  
**Classification:** DevOps & Security Operations Reference  
**Target Artifact:** Android Release APK / Android App Bundle (AAB)  

---

## 1. Pre-Release Security & Compilation Verification

| Step | Verification Procedure | Expected Outcome | Verification Tool / Command |
|---|---|---|---|
| **1.1 Unit & Integration Tests** | Execute comprehensive JVM and Robolectric test suites. | All unit tests pass with zero failures. | `gradle :app:testDebugUnitTest` |
| **1.2 Clean Code Compilation** | Perform end-to-end Kotlin compilation with all KSP processors. | Zero compilation errors or unresolved symbols. | `gradle assembleDebug` |
| **1.3 Zero Hardcoded Secrets** | Scan repository for API keys, bearer tokens, or hardcoded credentials. | Zero private credentials found; all configuration resides in `.env.example` / BuildConfig. | `grep -rI "AIza" .` |
| **1.4 Version Catalog Integrity** | Validate dependencies in `gradle/libs.versions.toml`. | All dependency versions locked and aligned. | Inspect `libs.versions.toml` |
| **1.5 Room Database Migration Test** | Verify migration paths (`MIGRATION_1_2`, `MIGRATION_2_3`). | In-memory and upgraded databases operate cleanly without table drop exceptions. | `RoomDatabaseTest.kt` |

---

## 2. Android Manifest & Permissions Hardening

- [x] **Zero Broad Storage Permissions:** Zero requests for `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`.
- [x] **Explicit Broadcast Receiver Export Rules:**
  - `RemoteLockdownReceiver`: Exported with `android:permission="android.permission.BROADCAST_SMS"` to ensure only OS telephony daemon can dispatch SMS intents.
  - `MobiArmourAdminReceiver`: Exported with `android:permission="android.permission.BIND_DEVICE_ADMIN"` to enforce Device Admin binding only by system.
- [x] **VPN Service Binding:**
  - `JarvisFirewallService`: Protected with `android:permission="android.permission.BIND_VPN_SERVICE"`.
- [x] **Explicit Activity Intent Filters:**
  - `PhishingGateActivity`: Handles web intent schemes (`http`, `https`) safely with parameter validation.
- [x] **Target SDK Alignment:**
  - `targetSdk = 35` (Android 15), `minSdk = 26` (Android 8.0 Oreo).

---

## 3. Production Release Build Procedure

### Step 1: Clean Build Environment
```bash
gradle clean
```

### Step 2: Compile Release Bundle & APK
```bash
gradle assembleRelease
# Or for developer verification / evaluation builds:
gradle assembleDebug
```

### Step 3: Verify APK Artifact Alignment
Confirm artifact existence in the standard deployment directory:
```bash
cp app/build/outputs/apk/debug/app-debug.apk output/mobiarmour-debug.apk
ls -lh output/
```

### Step 4: Validate APK Signature & Alignment
Ensure the APK is signed and zipalign validated:
```bash
zipalign -c -v 4 output/mobiarmour-debug.apk
```

---

## 4. Post-Deployment Operational Verification Runbook

1. **Initial Launch Test:** Launch MobiArmour; verify immediate display of baseline posture dashboard with zero crashes.
2. **Device Posture Audit:** Trigger "Run Security Audit"; verify that screen lock, encryption status, and developer settings are accurately detected within 3 seconds.
3. **Firewall Initialization:** Start Firewall Service; confirm Android VPN key icon appears in system status bar.
4. **Drop Sink Verification:** Toggle an app rule to "Blocked"; verify outgoing network attempts are discarded and logged into the Firewall Traffic tab.
5. **Database Purge Test:** Open Settings, select "Purge Database", verify all tables clear cleanly and re-initialize gracefully without throwing SQLite errors.
