# 💧 WaterReminderApp

Ein minimaler Wasser-Tracker für Android (API 26+), gebaut mit **Kotlin**, **Jetpack Compose** und **WorkManager**.

## Features

- Tägliches Trinkziel mit Fortschrittsanzeige
- Konfigurierbare Erinnerungen (Intervall in Minuten, Zeitfenster für Wochentag & Wochenende)
- Snooze (1× pro Reminder) – Follow-up ohne erneuten Snooze-Button
- Glückwunsch-Notification bei Zielerreichung (genau 1× pro Tag)
- Automatischer Tages-Reset um Mitternacht
- Debug-Informationen nur im Debug-Build sichtbar

## Smoke Test (5 min)

Schnell-Checkliste zur manuellen Validierung vor einem Release:

- [ ] **Notification Permission (Android 13+):** Einstellungen öffnen → Erinnerungen-Toggle aktivieren → Systemprompt erscheint → „Erlauben" gewährt Berechtigung, Snackbar zeigt `Erinnerungen gestartet ✅`; bei „Ablehnen" erscheint `Benachrichtigungen nicht erlaubt ❌`
- [ ] **Reminder im Zeitfenster:** Intervall auf 1 Min setzen, aktuelle Uhrzeit liegt zwischen Mo–Fr 08–23 Uhr bzw. Sa/So 09–23 Uhr → Notification erscheint innerhalb des konfigurierten Intervalls
- [ ] **Snooze-Verhalten:** Reminder-Notification öffnen → „Snooze"-Action antippen → Follow-up-Notification erscheint nach Snooze-Dauer **ohne** erneuten Snooze-Button
- [ ] **+250 ml trinken:** Hauptbutton antippen → Anzeige erhöht sich um 250 ml, Fortschrittsbalken aktualisiert sich
- [ ] **Goal erreicht:** Wasser so oft eintragen, bis Tagesziel überschritten wird → genau **eine** Glückwunsch-Notification → danach keine weiteren Reminder am selben Tag
- [ ] **Daily Reset:** Gerätedatum auf nächsten Tag stellen (oder bis Mitternacht warten) → ml-Zähler springt auf 0, Reminder sind wieder aktiv
- [ ] **Intervall < 15 Min:** Einstellung auf z. B. 5 Min setzen → App-Hinweis `⚡ Unter 15 Min. läuft der Reminder im OneTime-Modus …` ist sichtbar, Reminder funktioniert trotzdem (erhöhter Akkuverbrauch beachten)

## Build & Test

```bash
# Debug-APK bauen + Unit-Tests + Lint
./gradlew assembleDebug testDebugUnitTest lintDebug

# Nur Unit-Tests
./gradlew testDebugUnitTest

# Nur Lint
./gradlew lintDebug
```

## Technologien

| Schicht | Technologie |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Hintergrund | WorkManager (OneTimeWork-Kette) |
| Persistenz | DataStore Preferences |
| Sprache | Kotlin 2.x |
| Min SDK | 26 (Android 8.0) |
