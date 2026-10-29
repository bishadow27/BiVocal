import importlib.util
import sys
import unittest
from pathlib import Path

MODULE_PATH = Path(__file__).resolve().parents[1] / "scripts" / "large_vocabulary_pipeline.py"
SPEC = importlib.util.spec_from_file_location("large_vocabulary_pipeline", MODULE_PATH)
assert SPEC and SPEC.loader
pipeline = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = pipeline
SPEC.loader.exec_module(pipeline)


class LargeVocabularyPipelineTests(unittest.TestCase):
    def test_normalize_nfkc_case_and_spaces(self):
        self.assertEqual(pipeline.normalize("  ＢＯＯＫ   Store "), "book store")

    def test_source_topic_mapping_never_uses_unknown_labels(self):
        labels, topics = pipeline.source_topics([{
            "CoreInventory 1": "Food and drink", "CoreInventory 2": "Unknown",
            "Threshold": "Travel",
        }])
        self.assertEqual(labels, ["Food and drink", "Unknown", "Travel"])
        self.assertEqual(topics, ["FOOD", "TRAVEL"])

    def test_quality_gate_rejects_missing_ipa_and_generic_example(self):
        candidate = {"normalizedEnglish": "apple", "vietnamese": "quả táo",
                     "example": "The word apple is used in English.", "pronunciation": None,
                     "topicCodes": ["FOOD"], "cefrStatus": "exact",
                     "cefrStatusReason": "EXACT_HEADWORD_POS"}
        reasons = pipeline.quality_reasons(candidate)
        self.assertIn("MISSING_VALID_IPA", reasons)
        self.assertIn("GENERIC_OR_PLACEHOLDER_EXAMPLE", reasons)

    def test_cefr_conflict_is_never_auto_eligible(self):
        candidate = {"normalizedEnglish": "march", "vietnamese": "cuộc diễu hành",
                     "example": "The march crossed the city center.", "pronunciation": "/mɑːtʃ/",
                     "topicCodes": ["DAILY_LIFE"], "cefrStatus": "uncertain",
                     "cefrStatusReason": "SOURCE_CEFR_CONFLICT"}
        self.assertIn("SOURCE_CEFR_CONFLICT", pipeline.quality_reasons(candidate))

    def test_meaning_rule_conflict_does_not_override_source_without_evidence(self):
        topics, method = pipeline.classify_topic("remedy", "Thuốc và cách điều trị", ["TRAVEL"])
        self.assertEqual(topics, [])
        self.assertEqual(method, "MEANING_SOURCE_TOPIC_CONFLICT")

    def test_meaning_rule_and_source_can_confirm_topic(self):
        topics, method = pipeline.classify_topic("medicine", "Thuốc điều trị bệnh", ["HEALTH"])
        self.assertEqual(topics, ["HEALTH"])
        self.assertEqual(method, "MEANING_AND_SOURCE_CONFIRMED")

    def test_source_topics_without_semantic_evidence_remain_grounded_in_source_order(self):
        topics, method = pipeline.classify_topic("grave", "Nơi chôn vùi", ["SHOPPING", "HOME"])
        self.assertEqual(topics, ["SHOPPING", "HOME"])
        self.assertEqual(method, "SOURCE_CEFR_CATEGORY")

    def test_quality_gate_requires_semantic_topic_confirmation(self):
        candidate = {"normalizedEnglish": "statue", "vietnamese": "Tượng.",
                     "example": "A large statue stands in the city park.",
                     "pronunciation": "/ˈstætʃuː/", "topicCodes": ["SHOPPING"],
                     "topicClassificationMethod": "SINGLE_SOURCE_TOPIC",
                     "cefrStatus": "exact", "cefrStatusReason": "EXACT_HEADWORD_POS"}
        self.assertIn("TOPIC_NOT_SEMANTICALLY_CONFIRMED", pipeline.quality_reasons(candidate))

    def test_near_duplicate_meanings_are_conservatively_queued(self):
        candidates = [
            {"candidateId": "keep", "normalizedEnglish": "singular", "partOfSpeech": "noun",
             "vietnamese": "Số ít.", "qualityScore": 15, "validationReasons": []},
            {"candidateId": "review", "normalizedEnglish": "singular", "partOfSpeech": "noun",
             "vietnamese": "Từ ở số ít.", "qualityScore": 15, "validationReasons": []},
            {"candidateId": "distinct", "normalizedEnglish": "health", "partOfSpeech": "noun",
             "vietnamese": "Cốc rượu chúc sức khoẻ.", "qualityScore": 15, "validationReasons": []},
            {"candidateId": "health", "normalizedEnglish": "health", "partOfSpeech": "noun",
             "vietnamese": "Sức khoẻ.", "qualityScore": 15, "validationReasons": []},
        ]
        self.assertEqual(pipeline.near_duplicate_losers(candidates), {"review"})


if __name__ == "__main__":
    unittest.main()
