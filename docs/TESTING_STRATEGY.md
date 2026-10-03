# MobiArmour Platform — Verification & Testing Strategy

**Document Version:** 3.0.0-TestingStrategy  
**Classification:** Quality Assurance & Security Validation Blueprint  
**Audience:** Test Engineers, Core Contributors, Security Auditors  

---

## 1. Testing Philosophy & Verification Tiers

To validate an ambitious mobile security platform without relying on simulated or mock infrastructure, MobiArmour enforces a multi-tier testing methodology spanning fast local JVM tests, in-memory component simulations, and physical OEM device validation.

```
+------------------------------------------------------------------------------------+
| TIER 3: PHYSICAL OEM TEST BENCH VALIDATION (Real Hardware, Battery, Carrier SMS)    |
| - Samsung (One UI), Google (Pixel/AOSP), Xiaomi (MIUI/HyperOS), OnePlus (OxygenOS)  |
| - Android 9 (API 28) through Android 15 (API 35) matrix coverage                   |
| - Physical VPN throughput, Doze mode lifecycle resilience, hardware GPS fix        |
+-----------------------------------------+------------------------------------------+
                                          |
                                          v
+------------------------------------------------------------------------------------+
| TIER 2: LOCAL JVM ROBOLECTRIC INTEGRATION SUITE                                    |
| - Full Android Framework simulation on host JVM (PackageManager, VpnService)       |
| - Room SQLite In-Memory Database transaction validation                            |
| - Critical User Journey (CUJ) verification and state transitions                   |
+-----------------------------------------+------------------------------------------+
                                          |
                                          v
+------------------------------------------------------------------------------------+
| TIER 1: DETERMINISTIC UNIT TESTS & STATIC CODE ANALYSIS                            |
| - Pure Kotlin unit tests for ScoringEngine and heuristic algorithms                |
| - Android Gradle Plugin Lint & AST security scanning                               |
| - ZipFile archive byte parser boundary tests (corrupted ZIP, zero-length DEX)       |
+------------------------------------------------------------------------------------+
```

---

## 2. Test Execution & Automation Suite

### 2.1 Automated Unit & Robolectric Suite
- **Test Runner:** Gradle execution via `gradle :app:testDebugUnitTest`.
- **Target Fixtures:**
  - `ExampleRobolectricTest.kt`: Simulates application initialization, verifies `JarvisDatabase` creation, and confirms `JarvisMainViewModel` default state integrity.
  - `ScoringEngineTest`: Verifies deterministic score calculations across edge cases (e.g., zero installed apps, all apps requesting dangerous permissions, extreme outdated patch dates).
  - `SmsCommandParserTest`: Ingests synthetic SMS PDUs, tests unauthorized sender rejection, verifies case-insensitive command matching (`/lockdown`, `#LOCKDOWN`), and ensures malformed payloads are safely dropped.
  - `FirewallRoutingRuleTest`: Validates that `FirewallRuleEntity` additions correctly trigger `VpnService.Builder.addAllowedApplication()` calls exclusively for blocked packages.

### 2.2 In-Memory Room Database Validation
All DAO interfaces (`ScanResultDao`, `SecurityEventDao`, `FirewallRuleDao`, `TrustedDeviceDao`) are validated against transient in-memory SQLite instances (`Room.inMemoryDatabaseBuilder()`):
- Verifies foreign key constraints, index lookups, and transactional batch inserts.
- Asserts that timeline events are strictly ordered by descending timestamp.
- Confirms that updating a firewall rule modifies the existing entry rather than duplicating rows.

---

## 3. Physical Hardware & OEM Matrix Validation

Because Android exhibits behavioral variance across different manufacturers' proprietary power managers and framework modifications, physical testing must target representative devices:

| OEM / System Family | Target Test Device | Target Android API | Key Validation Focus |
|---|---|---|---|
| **Google Pixel (AOSP / Stock)** | Pixel 7 / Pixel 8 | Android 14 / 15 | Native `VpnService` performance, modern `QUERY_ALL_PACKAGES` compliance, Photo Picker zero-permission contract. |
| **Samsung (One UI)** | Galaxy S21 / S23 | Android 13 / 14 | Interaction with Samsung Knox, background service retention, Keyguard locking via Device Administrator. |
| **Xiaomi (MIUI / HyperOS)** | Redmi Note 11 / 12 | Android 11 / 12 | Aggressive background battery management, autostart permission restrictions, SMS broadcast delivery. |
| **Legacy Android Baseline** | Android Emulator / Test Device | Android 9.0 (API 28) | Backward compatibility of Java 8 ZipFile streaming, legacy notification channels, deprecated Keyguard APIs. |

---

## 4. Critical User Journey (CUJ) Test Scenarios

### CUJ-01: Stealth App Quarantine & Uninstallation
1. **Initial Condition:** Device contains a simulated test package lacking `Intent.CATEGORY_LAUNCHER` with background receiver flags.
2. **Action:** User executes "Scan Now" in Application Auditor.
3. **Assertion:** Package is identified as `isMalwareThreat = true` and `isHiddenApp = true`. Threat Action Center is displayed.
4. **Action:** User taps "UNINSTALL".
5. **Assertion:** `Intent.ACTION_DELETE` is fired with `package:<pkg>`. Upon broadcast `ACTION_PACKAGE_REMOVED`, the package is purged from UI state and score is recalculated.

### CUJ-02: Firewall Isolation & Selective Routing
1. **Initial Condition:** Firewall is disabled; web browsing operates normally.
2. **Action:** User toggles "Block" on target application and activates Firewall.
3. **Assertion:** `JarvisFirewallService` starts; target package is added to allowed applications in the VPN builder; target package traffic is discarded. Non-blocked browser continues to resolve domains with unhindered throughput.
4. **Action:** User engages "Software Air-Gap Mode".
5. **Assertion:** Universal routes `0.0.0.0/0` and `::/0` capture all traffic; all egress is terminated.

### CUJ-03: Out-of-Band Remote Lockdown Execution
1. **Initial Condition:** Device Administrator is granted; trusted contact `+15551234567` is registered.
2. **Action:** Synthetic SMS containing `#lockdown` is delivered from `+15551234567`.
3. **Assertion:** `RemoteLockdownReceiver` authenticates sender; `DevicePolicyManager.lockNow()` is executed; acoustic beacon activates; SMS response is queued with GPS coordinates.
4. **Negative Test:** Synthetic SMS containing `#lockdown` from unauthorized number `+15559999999` is rejected and logged as a security alert.
