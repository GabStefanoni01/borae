/**
 * Ajustes do ambiente de testes (Vitest + jsdom).
 * O jsdom não implementa showModal/close de <dialog>; os diálogos do painel usam esses métodos.
 */
if (typeof HTMLDialogElement !== 'undefined') {
  HTMLDialogElement.prototype.showModal ??= function (this: HTMLDialogElement) { this.setAttribute('open', ''); };
  HTMLDialogElement.prototype.close ??= function (this: HTMLDialogElement) { this.removeAttribute('open'); };
}
if (typeof URL.createObjectURL !== 'function') {
  URL.createObjectURL = () => 'blob:teste';
  URL.revokeObjectURL = () => undefined;
}
