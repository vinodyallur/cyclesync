# Privacy model

CycleSync follows a local-first design because menstrual, pain, and physiological
data are highly sensitive.

## Defaults

- Recommendation inference runs on the phone.
- Session records remain in app-private storage.
- Raw sensor samples are not uploaded automatically.
- Analytics are off unless a future build presents explicit consent.
- Data can be deleted from the app without contacting a server.
- Demo and test fixtures use synthetic data only.

## Future sharing

Any future clinician or research export should be explicit, selective, revocable,
encrypted, and separated from product analytics. The export UI must show exactly
which sessions and fields will leave the device.

## Data minimization

The recommendation interface prefers derived features over retaining raw streams.
Fields not required for the current session should not be collected. Logs must not
contain names, account identifiers, or free-text health notes.
