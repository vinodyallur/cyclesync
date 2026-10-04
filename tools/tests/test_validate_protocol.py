from __future__ import annotations

import copy
import json
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))

from validate_protocol import build_validator  # noqa: E402


class ProtocolSchemaTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.validator = build_validator()
        cls.examples = ROOT / "protocol" / "examples"

    def load_example(self, name: str) -> dict:
        with (self.examples / name).open("r", encoding="utf-8") as handle:
            return json.load(handle)

    def assert_valid(self, document: dict) -> None:
        errors = list(self.validator.iter_errors(document))
        self.assertEqual([], errors, [error.message for error in errors])

    def test_checked_examples_are_valid(self) -> None:
        for path in sorted(self.examples.glob("*.json")):
            with self.subTest(path=path.name):
                self.assert_valid(self.load_example(path.name))

    def test_apply_command_requires_confirmation(self) -> None:
        command = self.load_example("control-command.json")
        command["payload"]["userConfirmed"] = False
        self.assertTrue(list(self.validator.iter_errors(command)))

    def test_unknown_envelope_fields_are_rejected(self) -> None:
        telemetry = self.load_example("telemetry.json")
        invalid = copy.deepcopy(telemetry)
        invalid["cloudUserId"] = "should-not-exist"
        self.assertTrue(list(self.validator.iter_errors(invalid)))

    def test_out_of_range_recommendation_is_rejected(self) -> None:
        recommendation = self.load_example("recommendation.json")
        recommendation["payload"]["suggestedLevel"] = 99
        self.assertTrue(list(self.validator.iter_errors(recommendation)))


if __name__ == "__main__":
    unittest.main()