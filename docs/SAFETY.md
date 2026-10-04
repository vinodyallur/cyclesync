# Prototype safety policy

CycleSync is an engineering prototype, not a medical device. This document
defines software and workflow guardrails for demonstrations; it does not replace
electrical-safety review, risk management, or clinical evaluation.

## Non-negotiable controls

1. **Output disabled by default.** Firmware boots in bench mode and does not
   energize an attached stimulation stage without an explicit build-time flag.
2. **User confirmation.** Adaptive recommendations cannot become apply commands
   until the user confirms them in the current session.
3. **Independent bounds.** The Android policy and firmware each enforce their
   own setting and duration bounds.
4. **Immediate stop.** Emergency stop is always available and has priority over
   queued recommendations or model processing.
5. **Automatic timeout.** A session ends when its approved duration expires or
   communication becomes stale.
6. **Fail closed.** Invalid, missing, replayed, or low-quality inputs prevent an
   adaptive command.
7. **No autonomous escalation.** The system never increases a setting without a
   new recommendation and confirmation.

## Demonstration modes

- **Simulation mode:** generated telemetry and an on-screen controller. Preferred
  for development and presentations.
- **Bench mode:** physical sensors and ESP32 with stimulation output electrically
  disconnected or replaced by a visible dummy load.
- **Human-use mode:** intentionally unsupported by this prototype repository.

## Required future work

- formal hazard analysis and risk controls;
- electrical isolation, biocompatibility, and EMC testing;
- contraindication and usability review with qualified clinicians;
- model validation across representative participants and conditions;
- regulatory classification and quality-management planning;
- supervised clinical evaluation before effectiveness claims.
