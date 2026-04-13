# Raksha — Technical Specification Sheet

## Project Overview

| Field | Detail |
|---|---|
| App Name | Raksha |
| Platform | Android |
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Architecture | MVVM + StateFlow |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 34 (Android 14) |
| Category | Women's Safety |

---

## Architecture

### Pattern
MVVM (Model-View-ViewModel) with unidirectional data flow via StateFlow and Kotlin Coroutines.

### Layer Structure
```
app/
├── ui/                  # Jetpack Compose screens and components
├── viewmodel/           # ViewModels per screen
├── repository/          # Data layer abstraction
├── service/             # Foreground service for audio monitoring
├── ml/                  # TFLite model wrapper and inference engine
├── data/
│   ├── local/           # Room database, DAOs, entities
│   └── assets/          # Bundled NCRB JSON crime dataset
└── utils/               # GPS, SMS, permissions, maps helpers
```

---

## Tech Stack

### Core
| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| State Management | ViewModel + StateFlow |
| Async | Kotlin Coroutines + Flow |
| DI | Hilt |
| Navigation | Compose Navigation |

### Data
| Component | Technology |
|---|---|
| Local DB | Room (SQLite) |
| Crime Dataset | Bundled NCRB JSON asset |
| Preferences | DataStore |

### ML / Audio
| Component | Technology |
|---|---|
| ML Runtime | TensorFlow Lite |
| Base Model | YAMNet (fine-tuned for distress) |
| Audio Capture | Android AudioRecord API |
| Inference Thread | Background Coroutine on IO dispatcher |

### Location & Maps
| Component | Technology |
|---|---|
| GPS | FusedLocationProviderClient |
| Maps | Google Maps SDK for Android |
| Heatmap | Google Maps HeatmapTileProvider |
| Routing | Google Maps Directions API |

### Communication
| Component | Technology |
|---|---|
| SMS | Android SmsManager |
| Emergency Call | Android Intent (ACTION_CALL) |

### Background Processing
| Component | Technology |
|---|---|
| Audio Monitoring | Foreground Service |
| Persistent Tasks | WorkManager |

---

## Permissions

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

---

## Room Database Schema

### Table: users
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | Auto-generated |
| name | TEXT | User's full name |
| phone | TEXT | User's phone number |

### Table: trusted_contacts
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | Auto-generated |
| name | TEXT | Contact name |
| phone | TEXT | Contact phone number |

### Table: sos_events
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | Auto-generated |
| timestamp | TEXT | ISO 8601 format |
| lat | REAL | Latitude at trigger |
| lng | REAL | Longitude at trigger |
| confidence_score | REAL | TFLite output score (0.0–1.0) |
| trigger_type | TEXT | "auto" or "manual" |
| status | TEXT | "active" or "resolved" |

### Table: location_updates
| Column | Type | Notes |
|---|---|---|
| id | INTEGER PK | Auto-generated |
| sos_event_id | INTEGER FK | References sos_events.id |
| timestamp | TEXT | ISO 8601 format |
| lat | REAL | Updated latitude |
| lng | REAL | Updated longitude |

---

## ML Model Spec

| Field | Detail |
|---|---|
| Base Model | YAMNet (TensorFlow Hub) |
| Fine-tuned For | Distress sounds: screaming, crying, calling for help |
| Input | 16kHz mono audio, 0.975s frames |
| Output | Classification score per class (0.0–1.0) |
| Trigger Threshold | 0.75 confidence |
| Privacy | Audio buffer discarded post-inference, never stored or transmitted |
| Runtime | On-device only, no network calls |

---

## SOS Alert Spec

### Trigger Conditions
- Automatic: TFLite confidence score >= 0.75
- Manual: User presses SOS button on home screen

### On Trigger
1. Fetch GPS coordinates via FusedLocationProvider
2. Construct SOS SMS: `"EMERGENCY: [Name] needs help. Location: https://maps.google.com/?q=[lat],[lng] | Time: [timestamp] | Auto-detected threat: [score]%"`
3. Send SMS to all trusted contacts via SmsManager
4. Auto-dial 112 via Intent
5. Log event to Room DB (sos_events)
6. Begin location update loop every 30 seconds (location_updates table)
7. Send updated location SMS to trusted contacts every 2 minutes
8. Continue until user manually cancels from Active SOS screen

---

## Route Safety Scoring Algorithm

```
safety_score = (incident_density_weight * 0.5)
             + (time_of_day_weight * 0.3)
             + (route_length_weight * 0.2)

incident_density_weight = normalized incident count from NCRB JSON for route corridor
time_of_day_weight      = 0.2 (6am–8pm) | 0.7 (8pm–11pm) | 1.0 (11pm–6am)
route_length_weight     = normalized inverse of route distance (shorter = safer, minor factor)

Final route ranking: lowest safety_score = safest
```

---

## Screens

| Screen | Purpose |
|---|---|
| Onboarding | Name, phone, add trusted contacts |
| Home | Map with heatmap, Shield toggle, Navigate Safely button |
| Route | Destination input, scored route options |
| Active SOS | Alert sent confirmation, cancel, live location status |
| Settings | Manage contacts, view past SOS events log |

---

## API Keys Required

- Google Maps SDK (Android)
- Google Maps Directions API
- Google Maps JavaScript API (police dashboard — separate)

---

## Build & Run Requirements

- Android Studio Hedgehog or later
- JDK 17
- Google Services JSON file (Firebase / Maps)
- Physical Android device recommended for audio testing (emulator mic support is limited)
