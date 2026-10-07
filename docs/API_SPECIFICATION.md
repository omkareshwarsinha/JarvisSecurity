# MobiArmour Platform — Mobile API, Domain Model & Interface Specifications

**Document Version:** 3.0.0-APISpec  
**Classification:** Internal Developer & Architecture Reference  
**Namespace:** `com.example.jarvis`  

---

## 1. Core Domain Interfaces & Engine Contracts

### 1.1 `DeviceAuditor`
Interrogates device-level security configurations and hardware posture.

```kotlin
interface DeviceAuditorContract {
    /**
     * Conducts a comprehensive hardware and OS security posture audit.
     * Evaluates lock screen, encryption status, root indicators, ADB debugging,
     * developer options, unknown sources, mock location, and security patch hygiene.
     */
    suspend fun auditDevice(): List<DeviceCheckItem>
}
```

### 1.2 `PackageAuditor` & `DeepCodeAuditor`
Performs static analysis and binary inspections of installed APK packages.

```kotlin
interface PackageAuditorContract {
    /**
     * Enumerates and audits all installed applications on the device.
     * Categorizes system vs. third-party apps, evaluates granted dangerous permissions,
     * inspects target SDK versions, and assigns preliminary risk indicators.
     */
    suspend fun auditAllPackages(): List<AppSecurityInfo>
}

interface DeepCodeAuditorContract {
    /**
     * Streams APK ZIP archive to inspect DEX binaries, native architectures,
     * X.509 certificates, and telemetry endpoints.
     */
    suspend fun inspectApk(sourceDir: String): DeepAuditReport
}
```

### 1.3 `ScoringEngine`
Calculates explainable, deterministic security scores based on audit findings.

```kotlin
object ScoringEngine {
    /**
     * Produces aggregated overall score (0-100), sub-scores, and itemized deductions.
     */
    fun evaluate(
        deviceChecks: List<DeviceCheckItem>,
        appInfos: List<AppSecurityInfo>
    ): ScoringResult

    fun toScanSummary(result: ScoringResult): ScanSummary
}
```

### 1.4 `SmsCommandParser`
Tokenizes and validates incoming SMS emergency broadcasts.

```kotlin
object SmsCommandParser {
    /**
     * Parses incoming SMS message body.
     * Supports prefixes ('#', '/', '!') and optional PIN/nonce arguments.
     * Rejects conversational sentences.
     */
    fun parse(messageBody: String): ParsedSmsCommand
}

sealed class ParsedSmsCommand {
    data class Lockdown(val pinOrNonce: String? = null) : ParsedSmsCommand()
    data class Siren(val pinOrNonce: String? = null) : ParsedSmsCommand()
    data class Location(val pinOrNonce: String? = null) : ParsedSmsCommand()
    object Silence : ParsedSmsCommand()
    object Unknown : ParsedSmsCommand()
}
```

---

## 2. Persistence Layer Data Access Objects (DAOs)

### 2.1 `FirewallRuleDao`
```kotlin
@Dao
interface FirewallRuleDao {
    @Query("SELECT * FROM firewall_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<FirewallRuleEntity>>

    @Query("SELECT * FROM firewall_rules WHERE isBlocked = 1")
    fun getBlockedRules(): Flow<List<FirewallRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRule(rule: FirewallRuleEntity)

    @Query("DELETE FROM firewall_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)
}
```

### 2.2 `NetworkTrafficDao`
```kotlin
@Dao
interface NetworkTrafficDao {
    @Query("SELECT * FROM network_traffic ORDER BY timestamp DESC LIMIT 500")
    fun getRecentTraffic(): Flow<List<NetworkTrafficEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(traffic: List<NetworkTrafficEntity>)

    @Query("DELETE FROM network_traffic WHERE id NOT IN (SELECT id FROM network_traffic ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun pruneOldTraffic(keepCount: Int = 500)

    @Query("DELETE FROM network_traffic")
    suspend fun clearAll()
}
```

### 2.3 `SecurityEventDao`
```kotlin
@Dao
interface SecurityEventDao {
    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 100): Flow<List<SecurityEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SecurityEventEntity)

    @Query("DELETE FROM security_events")
    suspend fun clearAllEvents()
}
```

### 2.4 `TrustedDeviceDao`
```kotlin
@Dao
interface TrustedDeviceDao {
    @Query("SELECT * FROM trusted_devices WHERE isEnabled = 1")
    suspend fun getEnabledDevices(): List<TrustedDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: TrustedDeviceEntity): Long

    @Delete
    suspend fun delete(device: TrustedDeviceEntity)
}
```

---

## 3. Data Transfer & Presentation Models

### 3.1 `DeviceCheckItem`
```kotlin
data class DeviceCheckItem(
    val id: String,
    val title: String,
    val category: CheckCategory,
    val severity: FindingSeverity,
    val detectedValue: String,
    val description: String,
    val technicalExplanation: String,
    val recommendation: String,
    val isRemediable: Boolean = true,
    val remediationAction: RemediationAction? = null
)
```

### 3.2 `AppSecurityInfo`
```kotlin
data class AppSecurityInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val targetSdk: Int,
    val minSdk: Int,
    val isSystemApp: Boolean,
    val isDebuggable: Boolean,
    val requestedPermissions: List<String>,
    val grantedDangerousPermissions: List<String>,
    val specialPermissions: List<String>,
    val riskScore: Int,
    val riskLevel: FindingSeverity,
    val riskFactors: List<String>
)
```
