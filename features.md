# Raksha — Feature Specification

## Feature Overview

| # | Feature | Priority | Type |
|---|---|---|---|
| 1 | Audio Threat Detection | Critical | AI/ML |
| 2 | SOS Alert Flow | Critical | Core |
| 3 | Safe Route Engine | High | AI/Data |
| 4 | Predictive Risk Heatmap | High | AI/Data |
| 5 | Trusted Contacts Management | Critical | Core |
| 6 | Shield Activation Control | Critical | Core |
| 7 | SOS Event History | Medium | Core |
| 8 | Onboarding | Critical | Core |

---

## Feature 1: Audio Threat Detection

### What it does
Listens for distress sounds in the background when Shield is active, and automatically triggers an SOS alert without the user needing to act.

### How it works
- A Foreground Service starts when the user activates Shield
- Android AudioRecord API captures continuous 16kHz mono audio
- Audio frames are fed into a TFLite model derived from YAMNet, fine-tuned for distress sounds (screaming, crying, shouting for help)
- If the model's distress confidence score exceeds 0.75, the SOS flow is triggered automatically
- Audio buffer is discarded immediately after inference — never stored, never transmitted
- Only the confidence score and timestamp are retained locally in Room DB

### Privacy guarantee
- 100% on-device processing
- No audio ever leaves the phone
- No raw audio stored anywhere
- Model outputs a binary decision only

### Trigger threshold
- Default: 0.75 confidence score
- Below threshold: no action, no logging
- Above threshold: SOS flow begins immediately

### Acceptance criteria
- [ ] Foreground Service runs persistently when Shield is active
- [ ] AudioRecord captures audio without gaps
- [ ] TFLite inference completes within 200ms per frame
- [ ] Threshold trigger fires SOS flow reliably in testing
- [ ] Audio buffer confirmed cleared post-inference
- [ ] Persistent notification shown while Shield is active

---

## Feature 2: SOS Alert Flow

### What it does
On threat detection or manual trigger, immediately alerts trusted contacts and emergency services with the user's live location.

### Trigger types
- Automatic: TFLite confidence >= 0.75
- Manual: User long-presses SOS button for 1.5 seconds (prevents accidental activation)

### Alert sequence (fires in order)
1. Fetch current GPS coordinates via FusedLocationProvider
2. Construct SOS SMS message:
   ```
   EMERGENCY: [Name] needs help.
   Location: https://maps.google.com/?q=[lat],[lng]
   Time: [timestamp]
   Detected threat confidence: [X]%
   ```
3. Send SMS to all preset trusted contacts via SmsManager
4. Auto-dial 112 via Intent (silent background dial)
5. Log event to Room DB (sos_events table)
6. Navigate user to Active SOS screen
7. Begin location update loop — fetch GPS every 30 seconds
8. Send updated location SMS to trusted contacts every 2 minutes
9. Loop continues until user manually cancels

### Cancellation
- User must tap "Cancel Alert" button on Active SOS screen
- Cancellation logs resolved timestamp to sos_events
- Stops location update loop
- Sends final SMS: "UPDATE: [Name] has cancelled the alert. Last known location: [link]"

### Offline / no signal fallback
- If SMS fails to send, retry every 60 seconds
- Events logged locally regardless of send status
- Unsent alerts retried automatically when signal is restored

### Acceptance criteria
- [ ] SOS fires within 3 seconds of trigger
- [ ] SMS received by trusted contacts with correct location link
- [ ] 112 dialled automatically
- [ ] Location updates fire every 30 seconds
- [ ] Cancellation stops all loops and sends final update
- [ ] Failed SMS retried on signal restoration

---

## Feature 3: Safe Route Engine

### What it does
Scores available routes to a destination by safety, not just speed, using historical crime data and time-of-day weighting.

### How it works
- User enters destination on the Route screen
- Google Maps Directions API returns available route options
- Each route is scored using the safety scoring algorithm (see specsheet.md)
- Routes ranked by safety score — lowest score = safest
- Top 2 routes displayed as cards with safety score badge and color-coded overlay on map
- User selects route and navigates normally via Google Maps

### Scoring factors
- Incident density: NCRB crime data (district-level, bundled as local JSON asset) for areas along the route corridor
- Time of day: Night hours weighted significantly higher than daytime
- Route length: Minor factor, shorter preferred at equal safety scores

### Data source
- Bundled NCRB JSON dataset (offline, no network required for scoring)
- Dataset covers district-level crime statistics, incident types, and frequency

### Acceptance criteria
- [ ] Route options fetched and scored within 3 seconds
- [ ] Safety scores differ meaningfully between routes
- [ ] Color-coded map overlay renders correctly
- [ ] Works offline for scoring (only Maps API call requires network)
- [ ] Time of day scoring updates automatically based on device clock

---

## Feature 4: Predictive Risk Heatmap

### What it does
Overlays a real-time risk heatmap on the home screen map, showing users which areas around them carry higher historical risk — before they even start navigating.

### How it works
- On home screen, Google Maps renders a heatmap overlay using HeatmapTileProvider
- Data points sourced from bundled NCRB JSON asset (district/area level)
- Each point weighted by: incident frequency, incident type severity, and current time of day
- Heatmap refreshes weighting automatically every hour or when time crosses a threshold (e.g. 8pm, 11pm)

### Heatmap color coding
- Green (low risk): few incidents, daytime
- Amber (moderate): moderate incidents or evening hours
- Red (high risk): high incident density or late night hours

### Limitations (to be acknowledged in demo)
- Data is district-level, not street-level (NCRB granularity)
- Historical data — does not reflect real-time incidents
- Street-level precision is a future enhancement with crowdsourced incident reporting

### Acceptance criteria
- [ ] Heatmap renders on home screen map on load
- [ ] Color gradient correctly reflects risk levels
- [ ] Weighting updates based on current time
- [ ] No performance degradation on map with heatmap enabled
- [ ] Works fully offline (bundled data)

---

## Feature 5: Trusted Contacts Management

### What it does
Lets users pre-configure up to 5 trusted contacts who will receive SOS alerts.

### Rules
- Minimum 1 contact required before Shield can be activated
- Maximum 5 contacts
- Contacts stored in Room DB (trusted_contacts table)
- Each contact: name + phone number

### Screens
- Onboarding: Add contacts during initial setup
- Settings: Add, edit, or remove contacts at any time

### Acceptance criteria
- [ ] Contacts saved and persisted across app restarts
- [ ] Minimum 1 contact enforced before Shield activation
- [ ] Maximum 5 contacts enforced in UI
- [ ] Contact deletion via swipe in settings
- [ ] All contacts receive SOS SMS simultaneously

---

## Feature 6: Shield Activation Control

### What it does
Gives the user full control over when audio monitoring is active, addressing privacy concerns around always-on listening.

### Behaviour
- Default state: Shield OFF (audio monitoring not running)
- User explicitly activates Shield via toggle on home screen
- When active: Foreground Service starts, persistent notification shown
- When deactivated: Foreground Service stops, audio capture ends immediately
- Shield state persists across app minimization (Foreground Service)
- Shield does NOT auto-activate on app open or device boot

### Persistent notification (while Shield is active)
```
Title: Raksha is protecting you
Body:  Tap to open | Tap to deactivate
```

### Acceptance criteria
- [ ] Shield toggle correctly starts and stops Foreground Service
- [ ] Persistent notification appears when active, disappears when deactivated
- [ ] Audio capture confirmed stopped on deactivation
- [ ] Shield state survives app minimization
- [ ] Shield does not auto-activate without user action

---

## Feature 7: SOS Event History

### What it does
Logs all past SOS events locally for the user's reference, including auto-detected and manually triggered alerts.

### Displayed per event
- Date and time
- Trigger type (auto / manual)
- Confidence score (if auto)
- Status (active / resolved)
- Number of contacts alerted

### Storage
- Room DB, sos_events + location_updates tables
- Data never leaves device
- User can clear history from settings

### Acceptance criteria
- [ ] All SOS events logged regardless of SMS send status
- [ ] History list renders in reverse chronological order
- [ ] Event detail view shows all fields
- [ ] Clear history action requires confirmation

---

## Feature 8: Onboarding

### What it does
Collects minimum required setup information before the app can be used.

### Steps
1. Welcome screen — app name, tagline, single CTA
2. Name + phone number input
3. Add trusted contacts (minimum 1 required to proceed)
4. Permissions request screen — RECORD_AUDIO, LOCATION, SEND_SMS, CALL_PHONE
5. Done screen — brief explanation of Shield toggle

### Rules
- Onboarding shown only on first launch
- Cannot skip contacts step
- Cannot skip permissions step (explain why each is needed)
- If permissions denied, show clear explanation and re-prompt

### Acceptance criteria
- [ ] Onboarding shown only once
- [ ] Cannot proceed without minimum 1 trusted contact
- [ ] Cannot proceed without required permissions granted
- [ ] Data persisted to Room DB on completion
- [ ] Clean transition to home screen after onboarding

---

## Out of Scope (Hackathon)

The following are noted as future features and will not be built for demo:

- Crowdsourced real-time incident reporting
- Street-level crime data (NCRB is district-level only)
- iOS version
- Anonymous community reporting platform
- In-app therapist connection
- Multi-language support
- Police dashboard integration (separate deliverable)
