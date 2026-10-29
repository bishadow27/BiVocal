import test from 'node:test';
import assert from 'node:assert/strict';
import { PassThrough } from 'node:stream';
import { readHiddenPassword, askNewPassword } from './password-prompt.mjs';

function terminal() {
  const input = new PassThrough();
  input.isRaw = false;
  input.setRawMode = (raw) => {
    input.isRaw = raw;
  };
  let printed = '';
  const output = {
    write: (text) => {
      printed += text;
    },
  };
  return {
    input,
    output,
    get printed() {
      return printed;
    },
  };
}

test('hidden input masks characters and supports Unicode backspace without leaking password', async () => {
  const io = terminal();
  const result = readHiddenPassword('Password: ', io);
  for (const value of ['s', 'e', 'c', 'r', 'e', 't', '界']) io.input.emit('keypress', value, {});
  io.input.emit('keypress', '', { name: 'backspace' });
  io.input.emit('keypress', '1', {});
  io.input.emit('keypress', '\r', { name: 'return' });
  assert.equal(await result, 'secret1');
  assert.equal(io.printed.includes('secret'), false);
  assert.equal(io.printed.includes('界'), false);
  assert.equal(io.input.isRaw, false);
  assert.equal(io.input.listenerCount('keypress'), 0);
  io.input.destroy();
});

test('Ctrl+C cancels and restores terminal mode', async () => {
  const io = terminal();
  const result = readHiddenPassword('Password: ', io);
  io.input.emit('keypress', '\u0003', { name: 'c', ctrl: true });
  await assert.rejects(result, /Da huy/);
  assert.equal(io.input.isRaw, false);
  assert.equal(io.input.listenerCount('keypress'), 0);
  io.input.destroy();
});

test('different repeated password aborts before the database phase', async () => {
  const values = ['first-fixture', 'different-fixture'];
  await assert.rejects(
    askNewPassword(async () => values.shift()),
    /Hai mat khau/,
  );
});

test('matching repeated password is returned to validation', async () => {
  assert.equal(await askNewPassword(async () => 'same-fixture'), 'same-fixture');
});
