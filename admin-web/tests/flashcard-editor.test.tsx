import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastProvider } from '../src/components/ui';
import { FlashcardEditor } from '../src/pages/FlashcardsPage';
import type { AdminFlashcard } from '../src/types/api';

const card: AdminFlashcard = {
  id: 43,
  topicId: 9,
  topicCode: 'FOOD',
  topicName: 'Fixture topic',
  english: 'fixture word',
  vietnamese: 'nghĩa kiểm thử',
  pronunciation: null,
  partOfSpeech: null,
  example: null,
  exampleVi: null,
  level: 'A2',
  isActive: false,
  createdAt: null,
  updatedAt: null,
};
let writes: { method: string; path: string; body: Record<string, unknown> }[];
beforeEach(() => {
  writes = [];
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = new URL(String(input), 'http://test.local');
      if (init?.body) {
        writes.push({
          method: init.method!,
          path: url.pathname,
          body: JSON.parse(String(init.body)) as Record<string, unknown>,
        });
        return new Response(JSON.stringify(card), { status: 200 });
      }
      return new Response(
        JSON.stringify({
          items: [
            {
              id: 9,
              code: 'FOOD',
              name: 'Fixture topic',
              description: null,
              imageUrl: null,
              flashcardCount: 1,
            },
          ],
          page: 1,
          limit: 100,
          total: 1,
          totalPages: 1,
        }),
      );
    }),
  );
});
function mount(value: AdminFlashcard | null) {
  const close = vi.fn();
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <FlashcardEditor card={value} onClose={close} />
      </ToastProvider>
    </QueryClientProvider>,
  );
  return close;
}
describe('Flashcard mutation contracts', () => {
  it('editing an inactive card sends PUT content without isActive and preserves nulls', async () => {
    const close = mount(card);
    await screen.findByRole('option', { name: 'Fixture topic (FOOD)' });
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Lưu Flashcard' }));
    await waitFor(() => expect(close).toHaveBeenCalledOnce());
    expect(writes).toEqual([
      {
        method: 'PUT',
        path: '/api/v1/admin/flashcards/43',
        body: {
          topicCode: 'FOOD',
          english: card.english,
          vietnamese: card.vietnamese,
          pronunciation: null,
          partOfSpeech: null,
          example: null,
          exampleVi: null,
          level: 'A2',
        },
      },
    ]);
  });
  it('creating a card sends the chosen isActive only in POST', async () => {
    mount(null);
    const dialog = screen.getByRole('dialog'),
      actor = userEvent.setup();
    await screen.findByRole('option', { name: 'Fixture topic (FOOD)' });
    await actor.selectOptions(within(dialog).getByLabelText(/Chủ đề/), 'FOOD');
    await actor.type(screen.getByLabelText(/English/), 'new fixture');
    await actor.type(screen.getByLabelText(/Vietnamese/), 'nghĩa mới');
    await actor.selectOptions(screen.getByLabelText(/Level/), 'B1');
    await actor.click(screen.getByRole('checkbox'));
    await actor.click(screen.getByRole('button', { name: 'Lưu Flashcard' }));
    await waitFor(() => expect(writes).toHaveLength(1));
    expect(writes[0]).toMatchObject({
      method: 'POST',
      path: '/api/v1/admin/flashcards',
      body: { level: 'B1', isActive: false },
    });
  });
  it('legacy missing Level requires an explicit choice instead of silently assigning A1', async () => {
    mount({ ...card, level: null });
    await screen.findByRole('option', { name: 'Fixture topic (FOOD)' });
    await userEvent.click(screen.getByRole('button', { name: 'Lưu Flashcard' }));
    expect(screen.getByLabelText(/Level/)).toHaveAttribute('aria-invalid', 'true');
    expect(writes).toHaveLength(0);
    expect(screen.getByRole('dialog')).toBeVisible();
  });
});
