import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark' | 'system';
export type ResolvedTheme = 'light' | 'dark';

const STORAGE_KEY = 'borae.theme';

/**
 * Tema claro/escuro da plataforma.
 * - A preferência ('light' | 'dark' | 'system') fica no navegador (conveniência por usuário).
 * - O tema resolvido é aplicado em <html data-theme="light|dark">; o painel e o login
 *   usam esse atributo nos seus tokens de cor. A landing page permanece clara.
 * - src/index.html aplica o mesmo atributo antes do Angular iniciar para evitar "piscar".
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly media = this.document.defaultView?.matchMedia?.('(prefers-color-scheme: dark)');

  readonly mode = signal<ThemeMode>(this.readStored());
  private readonly systemDark = signal(this.media?.matches ?? false);
  readonly resolved = computed<ResolvedTheme>(() => {
    const mode = this.mode();
    return mode === 'system' ? (this.systemDark() ? 'dark' : 'light') : mode;
  });

  constructor() {
    this.media?.addEventListener?.('change', event => {
      this.systemDark.set(event.matches);
      this.apply();
    });
    this.apply();
  }

  setMode(mode: ThemeMode): void {
    this.mode.set(mode);
    try { localStorage.setItem(STORAGE_KEY, mode); } catch { /* armazenamento indisponível */ }
    this.apply();
  }

  private apply(): void {
    const root = this.document.documentElement;
    root.dataset['theme'] = this.resolved();
    root.style.colorScheme = this.resolved();
  }

  private readStored(): ThemeMode {
    try {
      const value = localStorage.getItem(STORAGE_KEY);
      return value === 'light' || value === 'dark' || value === 'system' ? value : 'system';
    } catch {
      return 'system';
    }
  }
}
