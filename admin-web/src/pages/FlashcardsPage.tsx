import { useState, type FormEvent } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Pencil, Power, Search } from 'lucide-react';
import { adminApi } from '../api/admin.api';
import type {
  AdminFlashcard,
  CardInput,
  CreateCardInput,
  FlashcardFilters,
  Level,
  PartOfSpeech,
} from '../types/api';
import { ApiError, errorMessage } from '../api/errors';
import {
  Badge,
  Button,
  ConfirmDialog,
  DataTable,
  Dialog,
  ErrorPanel,
  PageHeader,
  Pagination,
  Panel,
  QueryView,
  SelectField,
  TextField,
  Truncate,
  useToast,
} from '../components/ui';
import { useListState } from '../hooks/useListState';
import { useTopicOptions } from '../hooks/useTopicOptions';
import { nullable, validateCard, levels, parts, type FormErrors } from '../lib/forms';
import { contentKeys, invalidate } from '../lib/invalidate';
import { dateTime } from '../lib/format';

export function FlashcardEditor({
  card,
  onClose,
}: {
  card: AdminFlashcard | null;
  onClose: () => void;
}) {
  const topics = useTopicOptions(),
    client = useQueryClient(),
    toast = useToast();
  const [form, setForm] = useState({
    topicCode: card?.topicCode ?? '',
    english: card?.english ?? '',
    vietnamese: card?.vietnamese ?? '',
    pronunciation: card?.pronunciation ?? '',
    partOfSpeech: card?.partOfSpeech ?? '',
    example: card?.example ?? '',
    exampleVi: card?.exampleVi ?? '',
    level: card?.level ?? '',
    isActive: true,
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const set = (key: keyof typeof form, value: string | boolean) =>
    setForm((old) => ({ ...old, [key]: value }));
  const mutation = useMutation({
    mutationFn: (body: CardInput | CreateCardInput) =>
      card ? adminApi.updateCard(card.id, body) : adminApi.createCard(body as CreateCardInput),
    onSuccess: () => {
      void invalidate(client, contentKeys);
      toast(card ? 'Đã cập nhật Flashcard.' : 'Đã thêm từ vựng.');
      onClose();
    },
    onError: (error) => {
      if (error instanceof ApiError && error.code === 'FLASHCARD_EXISTS')
        setErrors({
          english: errorMessage(error),
          vietnamese: 'Kiểm tra cặp từ và nghĩa, kể cả Flashcard Inactive.',
        });
    },
  });
  function submit(event: FormEvent) {
    event.preventDefault();
    const body: CardInput = {
      topicCode: form.topicCode,
      english: form.english.trim(),
      vietnamese: form.vietnamese.trim(),
      pronunciation: nullable(form.pronunciation),
      partOfSpeech: nullable(form.partOfSpeech) as PartOfSpeech | null,
      example: nullable(form.example),
      exampleVi: nullable(form.exampleVi),
      level: form.level as Level,
    };
    const validation = validateCard(body);
    setErrors(validation);
    if (Object.keys(validation).length) return;
    mutation.mutate(card ? body : { ...body, isActive: form.isActive });
  }
  return (
    <Dialog
      title={card ? 'Chỉnh sửa Flashcard' : 'Thêm từ vựng'}
      onClose={onClose}
      busy={mutation.isPending}
      drawer
    >
      <form onSubmit={submit} noValidate>
        <div className="dialog-body">
          <p className="form-intro">
            {card
              ? 'Chỉnh sửa nội dung. Trạng thái Active/Inactive được quản lý riêng.'
              : 'Tạo từ vựng mới trong một chủ đề có sẵn.'}
          </p>
          {topics.isError && (
            <ErrorPanel
              error={topics.error}
              retry={() => {
                void topics.refetch();
              }}
            />
          )}
          <SelectField
            label="Chủ đề"
            value={form.topicCode}
            onChange={(v) => set('topicCode', v)}
            required
            disabled={topics.isPending || topics.isError}
            error={errors.topicCode}
            options={[
              { value: '', label: topics.isPending ? 'Đang tải chủ đề…' : 'Chọn chủ đề' },
              ...(topics.data ?? []).map((t) => ({
                value: t.code,
                label: `${t.name} (${t.code})`,
              })),
            ]}
          />
          <TextField
            label="English"
            value={form.english}
            onChange={(v) => set('english', v)}
            required
            maxLength={150}
            error={errors.english}
          />
          <TextField
            label="Vietnamese"
            value={form.vietnamese}
            onChange={(v) => set('vietnamese', v)}
            required
            maxLength={255}
            error={errors.vietnamese}
          />
          <TextField
            label="IPA / phát âm"
            value={form.pronunciation}
            onChange={(v) => set('pronunciation', v)}
            maxLength={150}
            error={errors.pronunciation}
          />
          <div className="form-grid">
            <SelectField
              label="Từ loại"
              value={form.partOfSpeech}
              onChange={(v) => set('partOfSpeech', v)}
              error={errors.partOfSpeech}
              options={[
                { value: '', label: 'Không chọn' },
                ...parts.map((v) => ({ value: v, label: v })),
              ]}
            />
            <SelectField
              label="Level"
              value={form.level}
              onChange={(v) => set('level', v)}
              required
              error={errors.level}
              options={[
                { value: '', label: 'Chọn Level' },
                ...levels.map((v) => ({ value: v, label: v })),
              ]}
            />
          </div>
          <TextField
            label="Ví dụ tiếng Anh"
            value={form.example}
            onChange={(v) => set('example', v)}
            multiline
            maxLength={5000}
            error={errors.example}
          />
          <TextField
            label="Ví dụ tiếng Việt"
            value={form.exampleVi}
            onChange={(v) => set('exampleVi', v)}
            multiline
            maxLength={5000}
            error={errors.exampleVi}
          />
          {!card && (
            <label className="checkbox-field">
              <input
                type="checkbox"
                checked={form.isActive}
                onChange={(e) => set('isActive', e.target.checked)}
              />
              Active ngay khi tạo
            </label>
          )}
          {mutation.isError && (
            <p className="form-error" role="alert">
              {errorMessage(mutation.error)}
            </p>
          )}
        </div>
        <footer className="dialog-footer">
          <Button disabled={mutation.isPending} onClick={onClose}>
            Hủy
          </Button>
          <Button
            type="submit"
            variant="primary"
            busy={mutation.isPending}
            disabled={topics.isPending || topics.isError}
          >
            Lưu Flashcard
          </Button>
        </footer>
      </form>
    </Dialog>
  );
}
export function FlashcardsPage() {
  const list = useListState(),
    topics = useTopicOptions(),
    client = useQueryClient(),
    toast = useToast();
  const [topic, setTopic] = useState(''),
    [status, setStatus] = useState<FlashcardFilters['status']>('ALL'),
    [level, setLevel] = useState<FlashcardFilters['level']>('');
  const [editor, setEditor] = useState<{ card: AdminFlashcard | null } | null>(null),
    [target, setTarget] = useState<AdminFlashcard | null>(null);
  const filters = { ...list.filters, topicCode: topic, status, level };
  const query = useQuery({
    queryKey: ['admin-flashcards', filters],
    queryFn: ({ signal }) => adminApi.flashcards(filters, signal),
    placeholderData: keepPreviousData,
  });
  const mutation = useMutation({
    mutationFn: (card: AdminFlashcard) => adminApi.cardStatus(card.id, !card.isActive),
    onSuccess: () => {
      void invalidate(client, contentKeys);
      toast('Đã cập nhật trạng thái Flashcard.');
      setTarget(null);
    },
  });
  return (
    <>
      <PageHeader
        title="Flashcards"
        description="Quản lý từ vựng, nội dung và trạng thái học tập"
        actions={
          <Button variant="primary" onClick={() => setEditor({ card: null })}>
            <Plus size={17} />
            Thêm từ vựng
          </Button>
        }
      />
      {topics.isError && (
        <ErrorPanel
          error={topics.error}
          retry={() => {
            void topics.refetch();
          }}
        />
      )}
      <Panel>
        <div className="toolbar filters-toolbar">
          <div className="search-field">
            <Search size={17} aria-hidden="true" />
            <input
              aria-label="Tìm Flashcard"
              placeholder="Tìm English hoặc Vietnamese…"
              maxLength={200}
              value={list.search}
              onChange={(e) => list.setSearch(e.target.value)}
            />
          </div>
          <SelectField
            label="Chủ đề"
            value={topic}
            onChange={(v) => {
              setTopic(v);
              list.setPage(1);
            }}
            options={[
              { value: '', label: 'Tất cả chủ đề' },
              ...(topics.data ?? []).map((t) => ({ value: t.code, label: t.name })),
            ]}
            disabled={topics.isPending}
          />
          <SelectField
            label="Trạng thái"
            value={status}
            onChange={(v) => {
              setStatus(v as FlashcardFilters['status']);
              list.setPage(1);
            }}
            options={[
              { value: 'ALL', label: 'Tất cả' },
              { value: 'ACTIVE', label: 'Active' },
              { value: 'INACTIVE', label: 'Inactive' },
            ]}
          />
          <SelectField
            label="Level"
            value={level}
            onChange={(v) => {
              setLevel(v as FlashcardFilters['level']);
              list.setPage(1);
            }}
            options={[
              { value: '', label: 'Tất cả' },
              ...levels.map((v) => ({ value: v, label: v })),
            ]}
          />
        </div>
        <QueryView query={query}>
          {(data) => (
            <>
              <DataTable
                rows={data.items}
                rowKey={(c) => c.id}
                columns={[
                  {
                    label: 'English',
                    render: (c) => (
                      <strong>
                        <Truncate value={c.english} />
                      </strong>
                    ),
                  },
                  { label: 'Vietnamese', render: (c) => <Truncate value={c.vietnamese} /> },
                  { label: 'IPA', render: (c) => <Truncate value={c.pronunciation} /> },
                  { label: 'Chủ đề', render: (c) => <Badge>{c.topicCode}</Badge> },
                  { label: 'Level', render: (c) => <Badge>{c.level ?? '—'}</Badge> },
                  {
                    label: 'Trạng thái',
                    render: (c) => (
                      <Badge tone={c.isActive ? 'green' : 'neutral'}>
                        {c.isActive ? 'Active' : 'Inactive'}
                      </Badge>
                    ),
                  },
                  {
                    label: 'Cập nhật',
                    render: (c) => <span className="nowrap muted">{dateTime(c.updatedAt)}</span>,
                  },
                  {
                    label: 'Thao tác',
                    render: (c) => (
                      <div className="row-actions">
                        <Button
                          variant="quiet"
                          aria-label={`Sửa ${c.english}`}
                          disabled={query.isPlaceholderData}
                          onClick={() => setEditor({ card: c })}
                        >
                          <Pencil size={15} />
                          Sửa
                        </Button>
                        <Button
                          variant="quiet"
                          aria-label={`${c.isActive ? 'Tắt' : 'Bật'} ${c.english}`}
                          disabled={query.isPlaceholderData}
                          onClick={() => {
                            mutation.reset();
                            setTarget(c);
                          }}
                        >
                          <Power size={15} />
                        </Button>
                      </div>
                    ),
                  },
                ]}
              />
              <Pagination
                {...data}
                busy={query.isFetching}
                onPage={list.setPage}
                onLimit={list.setLimit}
              />
            </>
          )}
        </QueryView>
      </Panel>
      {editor && <FlashcardEditor card={editor.card} onClose={() => setEditor(null)} />}
      {target && (
        <ConfirmDialog
          title={target.isActive ? 'Chuyển sang Inactive?' : 'Kích hoạt Flashcard?'}
          description={
            target.isActive
              ? `“${target.english}” sẽ ngừng xuất hiện trong học tập và Quiz. Progress, lịch sử và XP vẫn được giữ.`
              : `“${target.english}” sẽ xuất hiện trở lại trong nội dung học tập.`
          }
          busy={mutation.isPending}
          error={mutation.error}
          onClose={() => setTarget(null)}
          onConfirm={() => mutation.mutate(target)}
        />
      )}
    </>
  );
}
