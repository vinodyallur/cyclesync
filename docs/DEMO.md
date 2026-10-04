# Hackathon demo script

## Primary flow

1. Open the Android app and connect to the CycleSync ESP32 over BLE.
2. Show live signal quality and synthetic or bench sensor features.
3. Enter a pain score and comfort score.
4. Generate an on-device recommendation.
5. Open the explanation panel to show inputs and safety checks.
6. Confirm the recommendation.
7. Show the ESP32 acknowledgement and session countdown.
8. Trigger emergency stop and show both app and firmware return to idle.
9. Disable connectivity and repeat recommendation generation offline.

## Fallbacks

- If physical sensors are noisy, switch to the checked synthetic telemetry fixture.
- If BLE is unavailable, use the app's simulated transport with identical messages.
- If model integration is incomplete, use the deterministic baseline engine and
  explain the stable interface reserved for the on-device model.
- Keep stimulation output disconnected; use an LED or dashboard indicator as the
  controlled-output demonstration.

## Claims discipline

Describe the system as a personalization and safety workflow prototype. Do not
claim pain reduction, diagnosis, treatment, or clinical effectiveness.
