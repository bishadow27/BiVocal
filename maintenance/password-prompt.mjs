import { emitKeypressEvents } from 'node:readline';

export class OperatorInputError extends Error {}

// Only prints '*', never the entered characters. Used in the operator's terminal.
export function readHiddenPassword(
  prompt,
  { input = process.stdin, output = process.stdout } = {},
) {
  if (typeof input.setRawMode !== 'function') {
    return Promise.reject(
      new OperatorInputError('Hay chay lenh truc tiep trong PowerShell, khong qua pipe.'),
    );
  }
  return new Promise((resolve, reject) => {
    const wasRaw = input.isRaw === true;
    const characters = [];
    emitKeypressEvents(input);
    const finish = (cancelled = false) => {
      input.removeListener('keypress', handleKey);
      input.setRawMode(wasRaw);
      input.pause();
      output.write('\n');
      const value = characters.join('');
      characters.length = 0;
      if (cancelled) reject(new OperatorInputError('Da huy. Chua xoa tai khoan.'));
      else resolve(value);
    };
    const handleKey = (text, key = {}) => {
      if (key.ctrl && key.name === 'c') return finish(true);
      if (key.name === 'return' || key.name === 'enter') return finish();
      if (key.name === 'backspace') {
        if (characters.length) {
          characters.pop();
          output.write('\b \b');
        }
        return;
      }
      if (
        key.ctrl ||
        key.meta ||
        !text ||
        /[\x00-\x1f\x7f]/.test(text) ||
        [
          'up',
          'down',
          'left',
          'right',
          'home',
          'end',
          'delete',
          'insert',
          'pageup',
          'pagedown',
          'tab',
        ].includes(key.name)
      )
        return;
      for (const character of Array.from(text)) {
        if (characters.length < 256) {
          characters.push(character);
          output.write('*');
        }
      }
    };
    output.write(prompt);
    input.setRawMode(true);
    input.on('keypress', handleKey);
    input.resume();
  });
}

export async function askNewPassword(read = readHiddenPassword) {
  const password = await read('Mat khau ADMIN moi (it nhat 8 ky tu): ');
  const repeated = await read('Nhap lai dung mat khau vua nhap: ');
  if (password !== repeated) {
    throw new OperatorInputError(
      'Hai mat khau khong giong nhau. Chua xoa tai khoan; hay chay lai lenh.',
    );
  }
  return password;
}
