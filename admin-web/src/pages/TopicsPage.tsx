import { useState, type FormEvent } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Pencil, Search } from 'lucide-react';
import { adminApi } from '../api/admin.api';
import type { AdminTopic, TopicInput } from '../types/api';
import { ApiError, errorMessage } from '../api/errors';
import {
  Badge,
  Button,
  DataTable,
  Dialog,
  PageHeader,
  Pagination,
  Panel,
  QueryView,
  TextField,
  Truncate,
  useToast,
} from '../components/ui';
import { useListState } from '../hooks/useListState';
import { nullable, validateTopic, type FormErrors } from '../lib/forms';
import { contentKeys, invalidate } from '../lib/invalidate';
export function TopicEditor({ topic, onClose }: { topic: AdminTopic | null; onClose: () => void }) {
  const client = useQueryClient(),
    toast = useToast();
  const [code, setCode] = useState(topic?.code ?? ''),
    [name, setName] = useState(topic?.name ?? '');
  const [description, setDescription] = useState(topic?.description ?? ''),
    [imageUrl, setImageUrl] = useState(topic?.imageUrl ?? '');
  const [errors, setErrors] = useState<FormErrors>({});
  const mutation = useMutation({
    mutationFn: (body: TopicInput) =>
      topic ? adminApi.updateTopic(topic.id, body) : adminApi.createTopic(body),
    onSuccess: () => {
      void invalidate(client, contentKeys);
      toast(topic ? 'Đã cập nhật chủ đề.' : 'Đã thêm chủ đề.');
      onClose();
    },
    onError: (error) => {
      if (
        error instanceof ApiError &&
        ['TOPIC_CODE_EXISTS', 'TOPIC_NAME_EXISTS'].includes(error.code)
      )
        setErrors({ [error.code === 'TOPIC_CODE_EXISTS' ? 'code' : 'name']: errorMessage(error) });
    },
  });
  function submit(event: FormEvent) {
    event.preventDefault();
    const body = {
      code: code.trim(),
      name: name.trim(),
      description: nullable(description),
      imageUrl: nullable(imageUrl),
    };
    const validation = validateTopic(body);
    setErrors(validation);
    if (!Object.keys(validation).length) mutation.mutate(body);
  }
  return (
    <Dialog
      title={topic ? 'Chỉnh sửa chủ đề' : 'Thêm chủ đề'}
      onClose={onClose}
      busy={mutation.isPending}
      drawer
    >
      <form onSubmit={submit} noValidate>
        <div className="dialog-body">
          <p className="form-intro">Thông tin chủ đề dùng để tổ chức nội dung Flashcards.</p>
          <TextField
            label="Mã chủ đề"
            value={code}
            onChange={setCode}
            required
            maxLength={50}
            error={errors.code}
            help="Chữ hoa A–Z, số và _. Bắt đầu bằng chữ cái."
          />
          <TextField
            label="Tên chủ đề"
            value={name}
            onChange={setName}
            required
            maxLength={100}
            error={errors.name}
          />
          <TextField
            label="Mô tả"
            value={description}
            onChange={setDescription}
            multiline
            maxLength={5000}
            error={errors.description}
          />
          <TextField
            label="URL hình ảnh"
            value={imageUrl}
            onChange={setImageUrl}
            type="url"
            maxLength={2048}
            error={errors.imageUrl}
            help="Tùy chọn. Chỉ HTTP hoặc HTTPS."
          />
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
          <Button type="submit" variant="primary" busy={mutation.isPending}>
            Lưu chủ đề
          </Button>
        </footer>
      </form>
    </Dialog>
  );
}
export function TopicsPage() {
  const list = useListState();
  const [editor, setEditor] = useState<{ topic: AdminTopic | null } | null>(null);
  const query = useQuery({
    queryKey: ['admin-topics', list.filters],
    queryFn: ({ signal }) => adminApi.topics(list.filters, signal),
    placeholderData: keepPreviousData,
  });
  return (
    <>
      <PageHeader
        title="Chủ đề"
        description="Quản lý các chủ đề và nhóm từ vựng"
        actions={
          <Button variant="primary" onClick={() => setEditor({ topic: null })}>
            <Plus size={17} />
            Thêm chủ đề
          </Button>
        }
      />
      <Panel>
        <div className="toolbar">
          <div className="search-field">
            <Search size={17} aria-hidden="true" />
            <input
              aria-label="Tìm chủ đề"
              placeholder="Tìm theo mã hoặc tên chủ đề…"
              maxLength={200}
              value={list.search}
              onChange={(e) => list.setSearch(e.target.value)}
            />
          </div>
        </div>
        <QueryView query={query}>
          {(data) => (
            <>
              <DataTable
                rows={data.items}
                rowKey={(t) => t.id}
                columns={[
                  { label: 'Mã', render: (t) => <Badge>{t.code}</Badge> },
                  { label: 'Tên chủ đề', render: (t) => <strong>{t.name}</strong> },
                  { label: 'Mô tả', render: (t) => <Truncate value={t.description} /> },
                  { label: 'Số Flashcard', render: (t) => t.flashcardCount, className: 'numeric' },
                  {
                    label: 'Thao tác',
                    render: (t) => (
                      <Button
                        variant="quiet"
                        aria-label={`Sửa chủ đề ${t.code}`}
                        disabled={query.isPlaceholderData}
                        onClick={() => setEditor({ topic: t })}
                      >
                        <Pencil size={15} />
                        Sửa
                      </Button>
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
      {editor && <TopicEditor topic={editor.topic} onClose={() => setEditor(null)} />}
    </>
  );
}
