import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Logotipo OFICIAL do Boraê (arquivos em public/assets/images/brand/, extraídos da folha
 * oficial de variações — sem redesenho; para fundos claros só a cor do texto vira grafite).
 *
 * variant: 'horizontal' (cloche | Boraê) · 'stacked' (com "Gastronomia ao seu alcance") · 'mark' (ícone do app)
 * tone:    'light' = fundo claro · 'dark' = fundo escuro · 'auto' = acompanha o tema (html[data-theme])
 * size:    altura em px
 */
const ASPECT = { horizontal: 569 / 116, stacked: 358 / 253, mark: 1 };

@Component({
  selector: 'bo-brand-logo',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': '"logo tone-" + tone()',
    '[style.height.px]': 'size()',
    '[style.width.px]': 'width()'
  },
  template: `
    @if (variant() === 'mark') {
      <img src="assets/images/brand/borae-app-icon.png" alt="Boraê" [width]="size()" [height]="size()" decoding="async">
    } @else {
      @if (tone() !== 'dark') {
        <img class="on-light" [class.auto]="tone() === 'auto'" [src]="'assets/images/brand/borae-' + file() + '-on-light.png'" alt="Boraê" [height]="size()" decoding="async">
      }
      @if (tone() !== 'light') {
        <img class="on-dark" [class.auto]="tone() === 'auto'" [src]="'assets/images/brand/borae-' + file() + '-on-dark.png'" alt="Boraê" [height]="size()" decoding="async">
      }
    }
  `,
  styles: [`
    :host { display: inline-flex; align-items: center; flex-shrink: 0; line-height: 0; }
    img { height: 100%; width: auto; display: block; transition: transform .4s cubic-bezier(.16,1,.3,1); }
    :host(:hover) img { transform: translateY(-1px); }
    .auto.on-dark { display: none; }
    :host-context([data-theme=dark]) .auto.on-light { display: none; }
    :host-context([data-theme=dark]) .auto.on-dark { display: block; }
    @media (prefers-reduced-motion: reduce) { :host(:hover) img { transform: none; } }
  `]
})
export class BrandLogoComponent {
  readonly variant = input<'horizontal' | 'stacked' | 'mark'>('horizontal');
  readonly tone = input<'light' | 'dark' | 'auto'>('light');
  /** Altura em px. */
  readonly size = input<number>(36);
  protected readonly width = computed(() => Math.round(this.size() * ASPECT[this.variant()]));
  protected readonly file = computed(() => this.variant() === 'stacked' ? 'stacked' : 'lockup');
}
