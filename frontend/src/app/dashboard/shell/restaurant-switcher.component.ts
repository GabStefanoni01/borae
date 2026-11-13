import { ChangeDetectionStrategy, Component, ElementRef, HostListener, inject, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext, ROLE_LABEL } from '../restaurant-context.service';

/** Seletor do restaurante administrado. Dados exclusivamente de GET /users/me/restaurants. */
@Component({
  selector: 'bo-restaurant-switcher',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent],
  template: `
    @if (ctx.selected(); as current) {
      <button type="button" class="d-switch-trigger" (click)="open.set(!open())"
        aria-haspopup="listbox" [attr.aria-expanded]="open()" aria-controls="d-switch-list">
        <span class="d-avatar d-avatar-restaurant" aria-hidden="true">{{ initials(current.restaurantName) }}</span>
        <span class="d-switch-text">
          <strong>{{ current.restaurantName }}</strong>
          <small>{{ roleLabel[current.role] }}</small>
        </span>
        <bo-icon name="chevrons-up-down" [size]="16" />
      </button>
    } @else {
      <a routerLink="/app/novo-restaurante" class="d-switch-trigger d-switch-empty" (click)="navigated.emit()">
        <span class="d-avatar d-avatar-ghost" aria-hidden="true"><bo-icon name="plus" [size]="16" /></span>
        <span class="d-switch-text"><strong>Cadastrar restaurante</strong><small>Nenhum vínculo ainda</small></span>
      </a>
    }

    @if (open()) {
      <div class="d-switch-menu">
        <p class="d-switch-label" id="d-switch-label">Seus restaurantes</p>
        <ul id="d-switch-list" role="listbox" aria-labelledby="d-switch-label">
          @for (item of ctx.restaurants(); track item.restaurantId) {
            <li role="option" [attr.aria-selected]="item.restaurantId === ctx.selectedId()">
              <button type="button" (click)="choose(item.restaurantId)">
                <span class="d-avatar d-avatar-restaurant d-avatar-sm" aria-hidden="true">{{ initials(item.restaurantName) }}</span>
                <span class="d-switch-text"><strong>{{ item.restaurantName }}</strong><small>{{ roleLabel[item.role] }}</small></span>
                @if (item.restaurantId === ctx.selectedId()) { <bo-icon name="check" [size]="16" class="d-switch-check" /> }
              </button>
            </li>
          }
        </ul>
        <div class="d-switch-footer">
          <a routerLink="/app/novo-restaurante" (click)="close()"><bo-icon name="plus" [size]="16" /> Cadastrar restaurante</a>
          <a routerLink="/app/convite" (click)="close()"><bo-icon name="ticket" [size]="16" /> Aceitar convite</a>
        </div>
      </div>
    }
  `,
  host: { class: 'd-switch' }
})
export class RestaurantSwitcherComponent {
  protected readonly ctx = inject(RestaurantContext);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  protected readonly open = signal(false);
  protected readonly roleLabel = ROLE_LABEL;
  readonly navigated = output<void>();

  protected choose(restaurantId: string): void {
    this.ctx.select(restaurantId);
    this.close();
  }

  protected close(): void {
    this.open.set(false);
    this.navigated.emit();
  }

  protected initials(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) this.open.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.open.set(false);
  }
}
