# Raksha System Architecture (Slide Version)

> If your Mermaid tool expects raw diagram text (for example `mmdc`), use
> `SYSTEM_ARCHITECTURE_FLOWCHART.mmd` instead of this markdown file.

## Purpose
- Presentation-friendly architecture view with high-level flows only.
- No endpoint or API-level details.

## System Flowchart (Mermaid)
```mermaid
flowchart LR
    APP[Raksha Mobile App]
    DASH[Raksha Police Dashboard]
    CORE[Raksha Backend Platform]
    DB[MongoDB]
    EVID[Evidence Storage]
    OTP[OTP Services]
    MAPS[Google Maps]
    NCRB[NCRB Risk Dataset]
    EMG[Emergency Network 112]

    APP -->|Auth and profile sync| CORE
    CORE -->|Email and SMS verification| OTP
    CORE <-->|Core app and incident data| DB

    APP -->|SOS alerts and live location| CORE
    CORE -->|Realtime incident feed| DASH
    DASH -->|Acknowledge and resolve incidents| CORE

    APP -->|Evidence stream| CORE
    CORE -->|Store and serve evidence| EVID
    DASH -->|Evidence review requests| CORE

    APP -->|Navigation requests| MAPS
    APP -->|Risk context input| NCRB
    APP -->|Emergency call| EMG
```

## Notes
- Backend remains the integration hub between mobile app and police dashboard.
- OTP Services represent both email OTP and SMS OTP providers.
