# Permission Handling Guide

## Overview

This guide documents the permission handling implementation for the Snapcast Android app, including runtime permissions for notifications and automatic permissions for mDNS discovery.

## Permissions Used

### Runtime Permissions (Requested at First Launch)

#### 1. `POST_NOTIFICATIONS` (Android 13+)
- **Purpose:** Required to display foreground service notifications for media playback
- **When Requested:** During app onboarding (first launch)
- **User Impact:** Without this permission, users won't see playback controls in the notification shade
- **Graceful Degradation:** App continues to work, but notification controls won't be visible

### Install-Time Permissions (No User Prompt Required)

#### 2. `CHANGE_WIFI_MULTICAST_STATE`
- **Purpose:** Required for mDNS (Multicast DNS) server discovery
- **Details:** Automatically granted at install time (normal protection level)
- **Usage:** App acquires/releases a multicast lock when discovering servers via mDNS
- **Implementation:** Handled in `MdnsRepositoryImpl`

#### 3. `INTERNET`
- **Purpose:** Network communication with Snapcast server
- **Details:** Automatically granted (normal protection level)

#### 4. `WAKE_LOCK`
- **Purpose:** Keep device awake during audio playback
- **Details:** Automatically granted (normal protection level)

#### 5. `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- **Purpose:** Run audio playback service in foreground
- **Details:** Automatically granted (normal protection level)

## Implementation Architecture

### Onboarding Flow

```
App Launch
    ↓
Check onboarding status
    ↓
First Launch? → Show PermissionsScreen → Request POST_NOTIFICATIONS → Mark onboarding complete
    ↓
Not First Launch? → Show MainScreen directly
```

### Key Components

#### 1. `PermissionsScreen.kt`
- **Location:** `/app/src/main/java/de/badaix/snapcast/ui/PermissionsScreen.kt`
- **Purpose:** Handles runtime permission requests with user-friendly UI
- **Features:**
  - Beautiful Material 3 design
  - Explains why each permission is needed
  - Graceful handling of permission denial
  - Auto-continues when permissions granted

#### 2. `MainActivity.kt` (Updated)
- **Changes:** Added onboarding flow management
- **Flow:**
  1. Check if user has completed onboarding
  2. If not, show `PermissionsHandler`
  3. After permissions granted, mark onboarding complete
  4. Show main app

#### 3. `SharedPreferencesDataSource.kt` (Updated)
- **New Methods:**
  - `hasCompletedOnboarding(): Boolean`
  - `setOnboardingCompleted(completed: Boolean)`
- **Purpose:** Track whether user has gone through the onboarding/permissions flow

#### 4. `MdnsRepositoryImpl.kt`
- **Multicast Lock Handling:**
  ```kotlin
  // Acquire multicast lock for mDNS
  val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
  multicastLock = wifiManager?.createMulticastLock("snapcast_mdns")?.apply {
      setReferenceCounted(true)
      acquire()
  }
  ```
- **Cleanup:** Properly releases multicast lock when discovery stops

### Dependencies Added

#### Accompanist Permissions Library
- **Version:** 0.36.0
- **Purpose:** Simplifies runtime permission handling in Compose
- **Usage:** Provides `rememberPermissionState` and `MultiplePermissionsState` APIs

```kotlin
// In gradle/libs.versions.toml
accompanist = "0.36.0"
accompanist-permissions = { 
    group = "com.google.accompanist", 
    name = "accompanist-permissions", 
    version.ref = "accompanist" 
}

// In app/build.gradle.kts
implementation(libs.accompanist.permissions)
```

## User Experience

### First Launch
1. App opens to a clean permissions screen
2. User sees clear explanation:
   - **Notifications** - "Required to show playback controls and status updates while playing audio in the background"
3. User taps "Continue"
4. System permission dialog appears (Android 13+)
5. After grant/deny, user proceeds to main app
6. mDNS discovery starts automatically (no permission prompt needed)

### Subsequent Launches
- App opens directly to main screen
- No permission prompts shown (already completed onboarding)

### Permission Denial Handling
- If user denies notification permission:
  - App continues to work normally
  - Audio playback works
  - mDNS discovery works
  - Only limitation: notification controls won't be visible
- User can re-enable notification permission in system settings

## Best Practices Followed

### 1. ✅ Request Permissions at Appropriate Time
- **Runtime permissions** requested during onboarding, not immediately on launch
- **Context provided** explaining why each permission is needed

### 2. ✅ Graceful Degradation
- App continues to work if permissions are denied
- Core functionality (audio playback) not blocked by permission denial

### 3. ✅ Proper Resource Management
- Multicast locks acquired only when needed
- Locks properly released in `onCleared()` lifecycle method

### 4. ✅ Android Version Awareness
- POST_NOTIFICATIONS only requested on Android 13+ (API 33+)
- Uses `Build.VERSION.SDK_INT` checks

### 5. ✅ User Privacy
- Only requests essential permissions
- Clear explanations for each permission
- No background location or other sensitive permissions

## Testing Considerations

### Test Cases

1. **First Launch on Android 13+**
   - Verify permissions screen appears
   - Verify notification permission is requested
   - Verify app proceeds after grant/deny

2. **First Launch on Android 12 and Below**
   - Verify app proceeds directly to main screen (no runtime permissions needed)

3. **mDNS Discovery**
   - Verify servers are discovered automatically
   - Verify multicast lock is acquired
   - Verify lock is released when stopping discovery

4. **Permission Denial**
   - Deny notification permission
   - Verify app still works
   - Verify audio playback works
   - Verify mDNS discovery works

5. **Subsequent Launches**
   - Verify no permission prompts
   - Verify app goes directly to main screen

### Manual Testing Commands

```bash
# Reset app data to test first launch again
adb shell pm clear de.badaix.snapcast

# Check current permissions
adb shell dumpsys package de.badaix.snapcast | grep permission

# Revoke notification permission
adb shell pm revoke de.badaix.snapcast android.permission.POST_NOTIFICATIONS

# Grant notification permission
adb shell pm grant de.badaix.snapcast android.permission.POST_NOTIFICATIONS
```

## Troubleshooting

### Issue: Permissions screen doesn't appear
**Solution:** Clear app data and relaunch to trigger first-launch flow

### Issue: mDNS discovery not working
**Check:**
1. `CHANGE_WIFI_MULTICAST_STATE` permission is in manifest
2. Multicast lock is being acquired (check logs)
3. Device is on same network as Snapcast server
4. Router/firewall allows multicast traffic

### Issue: Notification controls not showing
**Check:**
1. Notification permission granted (Android 13+)
2. Foreground service is running
3. `POST_NOTIFICATIONS` permission is in manifest

## Future Enhancements

### Potential Additions
1. **Settings Screen Link:** Add deep link to app settings for re-enabling permissions
2. **In-App Rationale:** Show explanation dialog if user repeatedly denies permission
3. **Permission Status Indicator:** Show notification permission status in settings
4. **Network Permission:** Consider requesting Wi-Fi state permissions for better mDNS reliability

## Code References

### Main Files
- `/app/src/main/java/de/badaix/snapcast/ui/PermissionsScreen.kt`
- `/app/src/main/java/de/badaix/snapcast/MainActivity.kt`
- `/app/src/main/java/de/badaix/snapcast/data/datasource/SharedPreferencesDataSource.kt`
- `/app/src/main/java/de/badaix/snapcast/data/repository/MdnsRepositoryImpl.kt`
- `/app/src/main/AndroidManifest.xml`

### Key APIs Used
- `com.google.accompanist.permissions.rememberMultiplePermissionsState`
- `android.net.wifi.WifiManager.createMulticastLock`
- `android.net.nsd.NsdManager.discoverServices`

## Compliance

### Privacy Policy Considerations
Update your privacy policy to mention:
- Notification permission usage
- Network access for audio streaming
- mDNS/local network discovery

### Play Store Considerations
- Permission declarations are compliant with Play Store policies
- Permissions requested only when necessary
- Clear user-facing explanations provided

---

**Last Updated:** November 2025  
**Android API Support:** API 21 (Android 5.0) to API 36+

