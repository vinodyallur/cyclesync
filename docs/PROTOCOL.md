# BLE protocol

CycleSync uses versioned JSON messages during the hackathon prototype. JSON is
easy to inspect during integration; a later binary encoding can preserve the same
logical contract.

## Envelope

Every message contains:

- `protocolVersion`: currently `1`;
- `kind`: `telemetry`, `recommendation`, `control_command`, or `ack`;
- `deviceId` and `sessionId`;
- monotonic `sequence` for replay and ordering checks;
- `timestampMs` in Unix milliseconds;
- a kind-specific `payload`.

## Command lifecycle

1. Firmware sends telemetry.
2. Android validates schema, order, freshness, and signal quality.
3. The local engine creates an internal recommendation.
4. The Android safety policy validates and explains the result.
5. The user confirms; Android creates an `apply_setting` command.
6. Firmware revalidates bounds, applies or rejects the command, and sends `ack`.
7. An emergency `stop` command bypasses recommendation queues.

Examples in `protocol/examples/` are checked in CI against
`protocol/cyclesync.schema.json`.
