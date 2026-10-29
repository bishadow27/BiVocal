import importlib.util
import sys
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"
sys.path.insert(0, str(SCRIPTS))
PATH = SCRIPTS / "m15_5_vocabulary_pipeline.py"
SPEC = importlib.util.spec_from_file_location("m15_5_vocabulary_pipeline", PATH)
assert SPEC and SPEC.loader
pipeline = importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(pipeline)


class M155PipelineTests(unittest.TestCase):
    def test_unknown_cefr_is_not_a_rejection(self):
        row = {"headword": "zephyr", "normalizedEnglish": "zephyr", "vietnamese": "gió nhẹ",
               "partOfSpeech": "noun", "pronunciation": "/ˈzefə/", "example": "A zephyr crossed the field.",
               "primaryTopicCode": "GENERAL_VOCABULARY", "cefrStatus": "undetermined"}
        self.assertEqual(pipeline.structural_reasons(row), [])

    def test_unverified_optional_fields_can_be_omitted(self):
        row = {"headword": "apple", "normalizedEnglish": "apple", "vietnamese": "quả táo",
               "partOfSpeech": "noun", "pronunciation": None, "example": None,
               "primaryTopicCode": "FOOD", "cefrStatus": "exact"}
        self.assertEqual(pipeline.structural_reasons(row), [])

    def test_selection_caps_senses_and_prefers_headword_diversity(self):
        rows = [{"candidateId": f"a{i}", "normalizedEnglish": "a", "partOfSpeech": "noun",
                 "vietnamese": f"nghĩa {i}", "qualityScore": 100-i, "cefrStatus": "exact",
                 "cefr": "A1", "source": {"wordDefinitionId": i + 1}} for i in range(5)]
        rows += [{"candidateId": "b", "normalizedEnglish": "b", "partOfSpeech": "noun",
                  "vietnamese": "nghĩa b", "qualityScore": 1, "cefrStatus": "exact",
                  "cefr": "A1", "source": {"wordDefinitionId": 20}}]
        chosen = pipeline.choose(rows, 4)
        self.assertIn("b", {x["candidateId"] for x in chosen})
        self.assertLessEqual(sum(x["normalizedEnglish"] == "a" for x in chosen), 3)

    def test_reference_only_meanings_are_not_auto_approved(self):
        base = {"normalizedEnglish": "gouges", "partOfSpeech": "verb", "pronunciation": "/gaʊdʒɪz/",
                "example": "The tool gouges a deep line.", "primaryTopicCode": "GENERAL_VOCABULARY",
                "cefrStatus": "undetermined"}
        for meaning in ("Xem gouge", "Số nhiều của gouge", "Động từ chia ở ngôi thứ ba số ít của gouge"):
            row = {**base, "headword": "gouges", "vietnamese": meaning}
            self.assertIn("REFERENCE_ONLY_MEANING", pipeline.structural_reasons(row))

    def test_topic_requires_source_category_evidence(self):
        self.assertEqual(
            pipeline.classify_topic("sedimentary", "có cặn; thuốc cặn", []),
            (["GENERAL_VOCABULARY"], "GENERAL_DICTIONARY_TOPIC"))
        self.assertEqual(
            pipeline.classify_topic("drama", "nghệ thuật kịch", ["LEISURE"]),
            (["LEISURE"], "SOURCE_CEFR_CATEGORY"))

    def test_pos_conflict_and_transliterated_meaning_are_rejected(self):
        adjective = {"headword": "centennial", "normalizedEnglish": "centennial",
                     "vietnamese": "Lễ kỷ niệm một trăm năm", "partOfSpeech": "adjective",
                     "primaryTopicCode": "GENERAL_VOCABULARY", "cefrStatus": "exact"}
        self.assertIn("PART_OF_SPEECH_MEANING_CONFLICT", pipeline.structural_reasons(adjective))
        loanword = {"headword": "ligroin", "normalizedEnglish": "ligroin",
                    "vietnamese": "Ligroin.", "partOfSpeech": "noun",
                    "primaryTopicCode": "GENERAL_VOCABULARY", "cefrStatus": "exact"}
        self.assertIn("TRANSLATION_EQUALS_HEADWORD", pipeline.structural_reasons(loanword))

    def test_datafix_matches_english_pos_and_original_meaning_not_id(self):
        bad = {"headword": "wrist", "normalizedEnglish": "wrist",
               "vietnamese": "Cổ tay áo.", "partOfSpeech": "noun",
               "primaryTopicCode": "GENERAL_VOCABULARY", "cefrStatus": "exact"}
        self.assertIn("DATAFIX_KNOWN_SEMANTIC_ISSUE", pipeline.structural_reasons(bad))
        changed_meaning = {**bad, "vietnamese": "Cổ tay."}
        self.assertNotIn("DATAFIX_KNOWN_SEMANTIC_ISSUE", pipeline.structural_reasons(changed_meaning))
        changed_pos = {**bad, "partOfSpeech": "verb"}
        self.assertNotIn("DATAFIX_KNOWN_SEMANTIC_ISSUE", pipeline.structural_reasons(changed_pos))

    def test_datafix_suggestions_are_block_evidence_not_human_approval(self):
        self.assertGreater(len(pipeline.DATAFIX_OVERRIDES), 0)
        for row in pipeline.DATAFIX_OVERRIDES.values():
            self.assertIn(row["action"], {"BLOCK_REVIEW", "SUGGEST_REPLACE"})
            self.assertNotEqual(row.get("review_status"), "HUMAN_APPROVED")

    def test_repair_v2_hard_blocks_use_exact_content_triple(self):
        blocked = {"headword": "deceive", "normalizedEnglish": "deceive",
                   "vietnamese": "Làm thất vọng.", "partOfSpeech": "verb",
                   "primaryTopicCode": "GENERAL_VOCABULARY", "cefrStatus": "exact"}
        self.assertIn("REPAIR_V2_HARD_BLOCK", pipeline.structural_reasons(blocked))
        corrected = {**blocked, "vietnamese": "Lừa dối."}
        self.assertNotIn("REPAIR_V2_HARD_BLOCK", pipeline.structural_reasons(corrected))
        other_pos = {**blocked, "partOfSpeech": "noun"}
        self.assertNotIn("REPAIR_V2_HARD_BLOCK", pipeline.structural_reasons(other_pos))

    def test_lexical_overlap_is_not_semantic_verification(self):
        rows = [{"candidateId": "lexical", "headword": "wrist", "normalizedEnglish": "wrist", "partOfSpeech": "noun",
                 "vietnamese": "cổ tay áo", "qualityScore": 100, "cefrStatus": "undetermined",
                 "cefr": None, "source": {"wordDefinitionId": 1, "dictionaryPos": "N", "independentLexicalOverlap": True,
                                            "semanticVerified": False}},
                {"candidateId": "semantic", "headword": "apple", "normalizedEnglish": "apple", "partOfSpeech": "noun",
                 "vietnamese": "quả táo", "qualityScore": 1, "cefrStatus": "undetermined",
                 "cefr": None, "source": {"wordDefinitionId": 2, "dictionaryPos": "N", "semanticVerified": True}}]
        self.assertEqual(pipeline.choose(rows, 1)[0]["candidateId"], "semantic")


if __name__ == "__main__": unittest.main()
