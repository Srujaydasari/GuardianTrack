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
