import {
  ChangeDetectionStrategy, Component, ElementRef, effect, input, output, viewChild
} from '@angular/core';
import { IconComponent, BoraeIconName } from '../../shared/icon.component';
import { MODULE_STATUS_LABEL, ModuleStatus } from '../modules';
import { ROLE_LABEL, RestaurantRole } from '../restaurant-context.service';

/*
 * Componentes visuais compartilhados do painel.
 * Estilos em dashboard/styles/ui.css (classes .d-*), carregados pelo shell.
 */

/** Selo de disponibilidade de um módulo (não confundir com status do restaurante). */
@Component({
  selector: 'bo-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': '"d-badge d-badge-" + status()' },
  template: `<span class="d-badge-dot" aria-hidden="true"></span>{{ label() }}`
})
export class StatusBadgeComponent {
  readonly status = input.required<ModuleStatus>();
  protected label = () => MODULE_STATUS_LABEL[this.status()];
}

/** Papel do usuário em um restaurante. */
@Component({
  selector: 'bo-role-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': '"d-role d-role-" + role().toLowerCase()' },
  template: `{{ label() }}`
})
export class RoleBadgeComponent {
  readonly role = input.required<RestaurantRole>();
  protected label = () => ROLE_LABEL[this.role()];
}

/** Cabeçalho padrão de página: título, subtítulo, selo opcional e área de ações (ng-content). */
@Component({
  selector: 'bo-page-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [StatusBadgeComponent],
  template: `
    <header class="d-page-header">
      <div class="d-page-heading">
        <div class="d-page-title-row">
          <h1>{{ title() }}</h1>
          @if (status(); as s) { <bo-status-badge [status]="s" /> }
        </div>
        @if (subtitle()) { <p class="d-page-subtitle">{{ subtitle() }}</p> }
      </div>
      <div class="d-page-actions"><ng-content /></div>
    </header>
  `
})
export class PageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input('');
  readonly status = input<ModuleStatus | null>(null);
}

export type StateKind = 'empty' | 'loading' | 'error' | 'forbidden' | 'planned' | 'not-found';

const STATE_ICON: Record<StateKind, BoraeIconName> = {
  empty: 'compass', loading: 'loader', error: 'alert', forbidden: 'lock', planned: 'clock', 'not-found': 'search'
};

/** Estado padronizado: vazio, carregando, erro, sem permissão, planejado, não encontrado. */
@Component({
  selector: 'bo-state',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  host: {
    '[class]': '"d-state d-state-" + kind() + (compact() ? " d-state-compact" : "")',
    '[attr.role]': 'kind() === "error" ? "alert" : "status"',
    '[attr.aria-busy]': 'kind() === "loading" ? "true" : null'
  },
  template: `
    <span class="d-state-icon"><bo-icon [name]="icon() ?? defaultIcon()" [size]="compact() ? 20 : 26" [spin]="kind() === 'loading'" /></span>
    @if (title()) { <h2 class="d-state-title">{{ title() }}</h2> }
    @if (text()) { <p class="d-state-text">{{ text() }}</p> }
    <div class="d-state-actions"><ng-content /></div>
  `
})
export class StateComponent {
  readonly kind = input<StateKind>('empty');
  readonly title = input('');
  readonly text = input('');
  readonly icon = input<BoraeIconName | null>(null);
  readonly compact = input(false);
  protected defaultIcon = () => STATE_ICON[this.kind()];
}

/** Diálogo de confirmação acessível baseado em <dialog> nativo (foco preso, Esc fecha). */
@Component({
  selector: 'bo-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <dialog #dialog class="d-dialog" (cancel)="onCancel($event)" (click)="onBackdrop($event)" aria-labelledby="d-dialog-title">
      <div class="d-dialog-body">
        <span class="d-dialog-icon" [class.danger]="danger()"><bo-icon [name]="danger() ? 'alert' : 'info'" [size]="22" /></span>
        <h2 id="d-dialog-title">{{ title() }}</h2>
        <p>{{ message() }}</p>
      </div>
      <div class="d-dialog-actions">
        <button type="button" class="d-btn d-btn-secondary" (click)="cancelled.emit()" [disabled]="busy()">Cancelar</button>
        <button type="button" class="d-btn" [class.d-btn-danger]="danger()" [class.d-btn-primary]="!danger()" (click)="confirmed.emit()" [disabled]="busy()">
          @if (busy()) { <bo-icon name="loader" [size]="16" [spin]="true" /> }
          {{ confirmLabel() }}
        </button>
      </div>
    </dialog>
  `
})
export class ConfirmDialogComponent {
  readonly open = input(false);
  readonly title = input.required<string>();
  readonly message = input('');
  readonly confirmLabel = input('Confirmar');
  readonly danger = input(false);
  readonly busy = input(false);
  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const el = this.dialog().nativeElement;
      if (this.open() && !el.open) el.showModal();
      if (!this.open() && el.open) el.close();
    });
  }

  protected onCancel(event: Event): void {
    event.preventDefault();
    if (!this.busy()) this.cancelled.emit();
  }

  protected onBackdrop(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement && !this.busy()) this.cancelled.emit();
  }
}
