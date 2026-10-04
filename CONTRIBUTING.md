# Contributing

CycleSync spans health-related software, Bluetooth communication, embedded
firmware, and electrical hardware. Changes should remain small, reviewable, and
explicit about their safety impact.

## Development workflow

1. Create a branch from `main`.
2. Update or add tests for behavior changes.
3. Run protocol, mobile, and firmware checks relevant to the change.
4. Document any BLE contract or safety-policy change.
5. Open a pull request describing risk, validation evidence, and rollback steps.

## Pull-request checklist

- [ ] No secrets, private health data, or model binaries are committed.
- [ ] Shared messages still validate against `protocol/cyclesync.schema.json`.
- [ ] Adaptive output remains recommendation-only and user-confirmed.
- [ ] Firmware limits and emergency-stop behavior are unchanged or tested.
- [ ] Documentation reflects architecture or hardware changes.
- [ ] Tests pass locally.

## Hardware changes

Do not connect prototype stimulation circuitry to a person. Hardware-output
changes require independent review, bench isolation, current limiting, timeout
verification, and documented test evidence. This repository intentionally does
not define clinical-use parameters.
