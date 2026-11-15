/**
 * Valores monetários em BRL trafegam como centavos inteiros (priceInCents, priceDeltaInCents).
 * Estas funções convertem texto ↔ centavos com aritmética de strings e inteiros,
 * sem usar ponto flutuante como fonte de verdade.
 */

/** Maior valor aceito pelo campo (R$ 9.999.999,99) — evita números além da precisão segura. */
export const MAX_CENTS = 999_999_999;

/**
 * Converte o texto digitado em centavos.
 * Aceita "25,90", "25.90", "25", "1.234,56", "R$ 3,5". Devolve null se o texto não for um valor válido
 * (vazio, negativo, letras, mais de duas casas decimais ou acima de MAX_CENTS).
 */
export function parseBrlToCents(input: string | null | undefined): number | null {
  let text = (input ?? '').replace(/R\$/gi, '').replace(/\s+/g, '');
  if (!text) return null;
  if (!/^[0-9.,]+$/.test(text)) return null;

  let integerPart: string;
  let fractionPart = '';
  if (text.includes(',')) {
    // Vírgula é o separador decimal; pontos são separadores de milhar.
    const parts = text.split(',');
    if (parts.length > 2) return null;
    integerPart = parts[0].replace(/\./g, '');
    fractionPart = parts[1];
  } else {
    const dots = text.split('.');
    const last = dots[dots.length - 1];
    if (dots.length === 2 && last.length <= 2) {
      // "25.9" ou "25.90": ponto como separador decimal.
      integerPart = dots[0];
      fractionPart = last;
    } else {
      // "1.234" ou "1.234.567": pontos de milhar.
      if (dots.slice(1).some(group => group.length !== 3)) return null;
      integerPart = dots.join('');
    }
  }

  if (!/^\d*$/.test(integerPart) || !/^\d{0,2}$/.test(fractionPart)) return null;
  if (!integerPart && !fractionPart) return null;
  const reais = integerPart ? Number.parseInt(integerPart, 10) : 0;
  const cents = Number.parseInt((fractionPart + '00').slice(0, 2), 10);
  const total = reais * 100 + cents;
  if (!Number.isSafeInteger(total) || total > MAX_CENTS) return null;
  return total;
}

/** Formata centavos como "R$ 25,90" (com separador de milhar: "R$ 1.234,56"). */
export function formatCents(cents: number | null | undefined): string {
  return `R$ ${centsToInput(cents ?? 0)}`;
}

/** Formata um acréscimo: "+ R$ 3,00". */
export function formatDelta(cents: number): string {
  return `+ ${formatCents(cents)}`;
}

/** Centavos → texto editável sem símbolo: 2590 → "25,90"; 123456 → "1.234,56". */
export function centsToInput(cents: number): string {
  const value = Math.trunc(Math.abs(cents));
  const sign = cents < 0 ? '-' : '';
  const reais = Math.floor(value / 100).toString();
  const fraction = (value % 100).toString().padStart(2, '0');
  const grouped = reais.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${sign}${grouped},${fraction}`;
}
