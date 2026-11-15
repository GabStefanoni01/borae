import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../shared/icon.component';
import { CATALOG_LIMITS, Menu } from './catalog.models';

export interface MenuFormValue { name: string; description: string; }

/**
 * Formulário de cardápio (criar e editar). Só coleta os dados; a gravação é do componente pai,
 * que repassa o erro do servidor por `error`.
 */
@Component({
  selector: 'bo-menu-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconComponent],
  template: `
    <form class="d-form" (ngSubmit)="submit()" novalidate>
      <div class="d-field">
        <label for="mf-name">Nome do cardápio</label>
        <input id="mf-name" name="name" class="d-input" [ngModel]="name()" (ngModelChange)="name.set($event)" required
               [maxlength]="limits.menuName" placeholder="Ex.: Almoço, Jantar, Bebidas" autocomplete="off"
               [attr.aria-invalid]="showNameError() || null" aria-describedby="mf-name-hint">
        @if (showNameError()) { <span class="c-field-error" role="alert"><bo-icon name="alert" [size]="14" /> Informe o nome do cardápio.</span> }
        <span class="d-hint" id="mf-name-hint"><span>Como o cardápio aparece para o cliente.</span><span>{{ name().length }}/{{ limits.menuName }}</span></span>
      </div>
      <div class="d-field">
        <label for="mf-desc">Descrição <span>(opcional)</span></label>
        <textarea id="mf-desc" name="description" class="d-input" rows="3" [ngModel]="description()" (ngModelChange)="description.set($event)"
                  [maxlength]="limits.menuDescription" placeholder="Ex.: Servido de segunda a sexta, das 11h às 15h."></textarea>
        <span class="d-hint"><span></span><span>{{ description().length }}/{{ limits.menuDescription }}</span></span>
      </div>
      @if (error()) { <div class="d-alert d-alert-error" role="alert"><bo-icon name="alert" [size]="16" /><p>{{ error() }}</p></div> }
      <div class="d-form-actions c-form-end">
        <button type="button" class="d-btn d-btn-secondary" (click)="cancelled.emit(dirty())" [disabled]="busy()">Cancelar</button>
        <button type="submit" class="d-btn d-btn-primary" [disabled]="busy()">
          @if (busy()) { <bo-icon name="loader" [size]="16" [spin]="true" /> }
          Salvar cardápio
        </button>
      </div>
    </form>
  `
})
export class MenuFormComponent {
  /** Cardápio em edição (null = novo). */
  readonly menu = input<Menu | null>(null);
  readonly busy = input(false);
  readonly error = input('');
  readonly saved = output<MenuFormValue>();
  /** Emite se havia alterações não salvas, para o pai decidir se confirma o descarte. */
  readonly cancelled = output<boolean>();

  protected get limits() { return CATALOG_LIMITS; }
  protected readonly name = signal('');
  protected readonly description = signal('');
  private readonly submitted = signal(false);

  protected readonly showNameError = computed(() => this.submitted() && !this.name().trim());
  readonly dirty = computed(() => {
    const menu = this.menu();
    return this.name().trim() !== (menu?.name ?? '') || this.description().trim() !== (menu?.description ?? '');
  });

  constructor() {
    effect(() => {
      const menu = this.menu();
      this.name.set(menu?.name ?? '');
      this.description.set(menu?.description ?? '');
      this.submitted.set(false);
    });
  }

  protected submit(): void {
    this.submitted.set(true);
    if (!this.name().trim() || this.busy()) return;
    this.saved.emit({ name: this.name().trim(), description: this.description().trim() });
  }
}
