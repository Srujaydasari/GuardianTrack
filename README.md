# GuardianTrack – Android Background Location Tracker

GuardianTrack is an Android app that continuously tracks GPS location every 1 minute in the background and uploads the data (timestamp, latitude, longitude) to a Google Sheet via Google Apps Script. The service persists even when the screen is off or the app is removed from recent tasks.

---

## ✅ Features by Day

### 📅 Day 1: Project Setup + Foreground Service + Location
- Set up a new Android project with required dependencies
- Implemented `FusedLocationProviderClient` to fetch GPS coordinates
- Created a `ForegroundService` to keep location tracking alive in the background
- Logged latitude, longitude, and timestamp to **Logcat**
- Added necessary location and foreground service permissions

### 📅 Day 2: Google Sheet Integration + Periodic Updates
- Integrated Retrofit with a Google Apps Script Web App endpoint
- Sent location data (lat/lng + timestamp) to Google Sheets every 1 minute
- Used Foreground Service to persist updates in the background
- Confirmed background operation works with screen off or app killed
- Validated Google Sheet receives data correctly

### 📅 Day 3: Auto-Start on Boot    
- Created a `BroadcastReceiver` to listen for `BOOT_COMPLETED` event  
- Added appropriate `intent-filter` in `AndroidManifest.xml`  
- Tested app behavior after reboot to ensure service auto-starts  

### 📅 Day 4: Call & SMS Log Capture  

- Used `ContentResolver` to query:  
  - `CallLog.Calls.CONTENT_URI`  
  - `Telephony.Sms.CONTENT_URI`  
- Extracted caller number, timestamp, and type (incoming/outgoing)  
- Stored logs locally, optionally filtering duplicates by timestamp  


### 📅 Day 5: Upload to Google Sheets  

- Set up Google Apps Script endpoint or Firebase function for upload  
- Used `HttpUrlConnection`, Volley, or Retrofit to POST:  
  - Location updates  
  - Call/SMS logs  
- Formatted and pushed data to Google Sheets  


### 📅 Day 6: Watchdog & UI Layer  

- Watch for:  
  - GPS turned off → show persistent notification or Toast  
  - Revoked permissions → prompt user to re-enable  
- Created basic UI to:  
  - Show last 10 GPS entries  
  - Manually re-trigger permission checks  
 

### 📅 Day 7: Polish & Submit  

- Tested thoroughly on:  
  - Reboot behavior  
  - Background performance  
  - Battery saver modes  
  - Permission toggling  
- Recorded demo video (screen recording + narration)  
- Finalized GitHub repo with:  
  - Clean and commented code  
  - Comprehensive README (permissions, setup, testing guide)  


## 📋 Permissions Required
- `ACCESS_FINE_LOCATION` (for GPS location)
- `FOREGROUND_SERVICE` (to run background location tracking)
- `RECEIVE_BOOT_COMPLETED` (to auto-start service on reboot)
- `READ_CALL_LOG` (to read call logs)
- `READ_SMS` (to read SMS logs)
- `INTERNET` (to upload data to Google Sheets)


