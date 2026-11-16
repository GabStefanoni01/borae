import { centsToInput, formatCents, formatDelta, parseBrlToCents } from './money';

describe('money — conversão BRL ↔ centavos', () => {
  it('converte textos válidos para centavos inteiros', () => {
    expect(parseBrlToCents('25,90')).toBe(2590);
    expect(parseBrlToCents('25,9')).toBe(2590);
    expect(parseBrlToCents('25')).toBe(2500);
    expect(parseBrlToCents('25.90')).toBe(2590);
    expect(parseBrlToCents('R$ 3,00')).toBe(300);
    expect(parseBrlToCents('1.234,56')).toBe(123456);
    expect(parseBrlToCents('1.234')).toBe(123400);
    expect(parseBrlToCents('0,01')).toBe(1);
    expect(parseBrlToCents(',5')).toBe(50);
    expect(parseBrlToCents('0')).toBe(0);
  });

  it('não sofre erro de ponto flutuante (0,29 e 19,99)', () => {
    expect(parseBrlToCents('0,29')).toBe(29);
    expect(parseBrlToCents('19,99')).toBe(1999);
    expect(parseBrlToCents('1.005,07')).toBe(100507);
  });

  it('recusa valores inválidos ou negativos', () => {
    for (const value of ['', '   ', 'abc', '-5', '12,5x', '1,234', '1,2,3', '12.345.6', 'R$', null, undefined, '99999999999']) {
      expect(parseBrlToCents(value as string)).toBeNull();
    }
  });

  it('formata centavos para exibição em BRL', () => {
    expect(formatCents(2590)).toBe('R$ 25,90');
    expect(formatCents(0)).toBe('R$ 0,00');
    expect(formatCents(5)).toBe('R$ 0,05');
    expect(formatCents(123456789)).toBe('R$ 1.234.567,89');
    expect(formatDelta(300)).toBe('+ R$ 3,00');
  });

  it('ida e volta preserva o valor', () => {
    for (const cents of [0, 1, 99, 100, 2590, 123456, 999999999]) {
      expect(parseBrlToCents(centsToInput(cents))).toBe(cents);
    }
  });
});
