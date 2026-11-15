import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { IconComponent } from './icon.component';
import { formatCents, formatDelta } from './money';

/** Dados exibidos ao consumidor (subconjunto de PublicProduct, sem campos administrativos). */
export interface MenuProductView {
  name: string;
  description: string | null;
  priceInCents: number;
  availability: 'AVAILABLE' | 'UNAVAILABLE';
  ingredients: string[];
  /** Somente adicionais ativos (a API pública já filtra; a prévia do painel filtra antes de passar). */
  addons: { name: string; priceDeltaInCents: number }[];
  imageUrl: string | null;
}

/**
 * Cartão de produto como o consumidor vê no cardápio digital.
 * Usado pela página pública (/r/:slug) e pela prévia no painel. Estilos com prefixo .mp-
 * em styles.css (globais), pois aparece fora e dentro do painel.
 * Não há ações de pedido: adicionais são informativos e o total não é calculado.
 */
@Component({
  selector: 'bo-menu-product-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  host: { class: 'mp-card', '[class.mp-unavailable]': 'unavailable()' },
  template: `
    <div class="mp-media">
      @if (product().imageUrl && !imageFailed()) {
        <img [src]="product().imageUrl" [alt]="product().name" loading="lazy" decoding="async" width="640" height="480" (error)="imageFailed.set(true)">
      } @else {
        <span class="mp-placeholder" aria-hidden="true"><bo-icon name="cloche" [size]="30" /></span>
      }
      @if (unavailable()) { <span class="mp-flag">Indisponível no momento</span> }
    </div>
    <div class="mp-body">
      <div class="mp-head">
        <h3 class="mp-name">{{ product().name }}</h3>
        <span class="mp-price">{{ price() }}</span>
      </div>
      @if (product().description) { <p class="mp-desc">{{ product().description }}</p> }
      @if (product().ingredients.length) {
        <div class="mp-block">
          <h4>Ingredientes</h4>
          <p class="mp-ingredients">{{ product().ingredients.join(', ') }}</p>
          <p class="mp-note">Ingredientes informados pelo restaurante.</p>
        </div>
      }
      @if (product().addons.length) {
        <div class="mp-block">
          <h4>Adicionais</h4>
          <ul class="mp-addons">
            @for (addon of product().addons; track addon.name) {
              <li><span>{{ addon.name }}</span><span class="mp-delta">{{ delta(addon.priceDeltaInCents) }}</span></li>
            }
          </ul>
        </div>
      }
    </div>
  `
})
export class MenuProductCardComponent {
  readonly product = input.required<MenuProductView>();
  protected readonly imageFailed = signal(false);
  protected readonly unavailable = computed(() => this.product().availability === 'UNAVAILABLE');
  protected readonly price = computed(() => formatCents(this.product().priceInCents));
  protected delta(cents: number): string { return formatDelta(cents); }
}
