import type { CardInput, TopicInput } from '../types/api';
export type FormErrors = Record<string, string>;
export const nullable = (value: string) => value.trim() || null;
export const parts = [
  'noun',
  'verb',
  'adjective',
  'adverb',
  'preposition',
  'conjunction',
  'pronoun',
  'phrase',
] as const;
export const levels = ['A1', 'A2', 'B1', 'B2'] as const;
export function validateTopic(input: TopicInput): FormErrors {
  const errors: FormErrors = {};
  if (!/^[A-Z][A-Z0-9_]{0,49}$/.test(input.code))
    errors.code = 'Bắt đầu bằng A–Z; chỉ chữ hoa, số và _, tối đa 50 ký tự.';
  if (!input.name || input.name.length > 100) errors.name = 'Tên chủ đề từ 1 đến 100 ký tự.';
  if (input.description && input.description.length > 5000)
    errors.description = 'Mô tả tối đa 5.000 ký tự.';
  if (input.imageUrl) {
    try {
      const url = new URL(input.imageUrl);
      if (!['http:', 'https:'].includes(url.protocol) || input.imageUrl.length > 2048)
        throw new Error();
    } catch {
      errors.imageUrl = 'Nhập URL HTTP hoặc HTTPS hợp lệ, tối đa 2.048 ký tự.';
    }
  }
  return errors;
}
export function validateCard(input: CardInput): FormErrors {
  const errors: FormErrors = {};
  if (!input.topicCode) errors.topicCode = 'Chọn chủ đề.';
  if (!input.english || input.english.length > 150)
    errors.english = 'Từ tiếng Anh từ 1 đến 150 ký tự.';
  if (!input.vietnamese || input.vietnamese.length > 255)
    errors.vietnamese = 'Nghĩa tiếng Việt từ 1 đến 255 ký tự.';
  if (input.pronunciation && input.pronunciation.length > 150)
    errors.pronunciation = 'IPA tối đa 150 ký tự.';
  if (!levels.includes(input.level)) errors.level = 'Chọn A1, A2, B1 hoặc B2.';
  if (input.partOfSpeech && !parts.includes(input.partOfSpeech))
    errors.partOfSpeech = 'Từ loại không hợp lệ.';
  for (const key of ['example', 'exampleVi'] as const)
    if (input[key] && input[key].length > 5000) errors[key] = 'Ví dụ tối đa 5.000 ký tự.';
  return errors;
}
