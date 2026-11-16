import {
  ChangeDetectionStrategy, Component, ElementRef, Injectable, effect, inject, input, output, signal, viewChild
} from '@angular/core';
import { IconComponent } from '../../shared/icon.component';

export interface Toast { id: number; kind: 'success' | 'error'; text: string; }

/**
 * Avisos rápidos do painel (sucesso/erro após uma operação confirmada pelo servidor).
 * Fornecido no shell do painel; anunciado por leitores de tela via aria-live.
 */
@Injectable()
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private seq = 0;

  success(text: string): void { this.push('success', text); }
  error(text: string): void { this.push('error', text); }

  dismiss(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  private push(kind: Toast['kind'], text: string): void {
    const id = ++this.seq;
    this.toasts.update(list => [...list.slice(-2), { id, kind, text }]);
    setTimeout(() => this.dismiss(id), kind === 'error' ? 7000 : 4000);
  }
}

@Component({
  selector: 'bo-toasts',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <div class="d-toasts" aria-live="polite" aria-atomic="false">
      @for (t of toast.toasts(); track t.id) {
        <div class="d-toast" [class.d-toast-error]="t.kind === 'error'" [attr.role]="t.kind === 'error' ? 'alert' : 'status'">
          <bo-icon [name]="t.kind === 'error' ? 'alert' : 'check-circle'" [size]="18" />
          <p>{{ t.text }}</p>
          <button type="button" class="d-toast-close" (click)="toast.dismiss(t.id)" aria-label="Fechar aviso"><bo-icon name="x" [size]="14" /></button>
        </div>
      }
    </div>
  `
})
export class ToastsComponent {
  protected readonly toast = inject(ToastService);
}

/**
 * Painel modal (formulários curtos) baseado em <dialog> nativo: foco preso e Esc fecha.
 * O conteúdo do formulário entra por ng-content; o fechamento é decidido pelo componente pai.
 */
@Component({
  selector: 'bo-panel-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <dialog #dialog class="d-dialog d-panel-dialog" (cancel)="onCancel($event)" [attr.aria-labelledby]="titleId">
      <header class="d-panel-head">
        <div>
          <h2 [id]="titleId">{{ title() }}</h2>
          @if (subtitle()) { <p>{{ subtitle() }}</p> }
        </div>
        <button type="button" class="d-icon-btn" (click)="closed.emit()" aria-label="Fechar"><bo-icon name="x" [size]="18" /></button>
      </header>
      <div class="d-panel-body"><ng-content /></div>
    </dialog>
  `
})
export class PanelDialogComponent {
  private static seq = 0;
  readonly open = input(false);
  readonly title = input.required<string>();
  readonly subtitle = input('');
  readonly closed = output<void>();
  protected readonly titleId = `d-panel-title-${++PanelDialogComponent.seq}`;
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const el = this.dialog().nativeElement;
      if (this.open() && !el.open) el.showModal?.();
      if (!this.open() && el.open) el.close();
    });
  }

  protected onCancel(event: Event): void {
    event.preventDefault();
    this.closed.emit();
  }
}
