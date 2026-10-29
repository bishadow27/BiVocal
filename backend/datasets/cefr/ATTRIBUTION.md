# Words CEFR Dataset attribution

- Source: [Maximax67/Words-CEFR-Dataset](https://github.com/Maximax67/Words-CEFR-Dataset)
- Imported file: `datasets/word_list_cefr.csv`
- Source author: Belikov Maxim
- Copyright: Copyright (c) 2024 Belikov Maxim
- License: MIT; the full notice is preserved in `LICENSE` beside this file.

The upstream project says its levels are computed from CEFR-J data, word/lemma/stem
frequency and part-of-speech signals. Therefore BiVoca treats these labels as
source-derived estimates. They are not represented as official CEFR certification.

The three final CSV columns (`CoreInventory 1`, `CoreInventory 2`, `Threshold`) are
source vocabulary/category signals. They are retained as audit metadata and are not
used to replace BiVoca's ten application topics.
