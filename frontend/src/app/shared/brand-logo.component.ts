import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Logotipo Boraê em vetor.
 * Reproduz o símbolo oficial (cúpula/cloche laranja) com a palavra "Boraê" em
 * Plus Jakarta Sans, sem o fundo creme do PNG — encaixa em qualquer superfície,
 * escala sem perder nitidez e se adapta a fundos claros e escuros.
 *
 * variant="horizontal" (padrão) — símbolo à esquerda, ideal para header/rodapé.
 * variant="stacked" — símbolo acima da palavra, como o logo oficial.
 * variant="mark" — apenas o símbolo.
 * tone="dark" — palavra em branco para fundos escuros.
 */
@Component({
  selector: 'bo-brand-logo',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': '"logo logo-" + variant() + " tone-" + tone()',
    '[style.--logo-size.px]': 'size()',
    role: 'img',
    'aria-label': 'Boraê'
  },
  template: `
    <svg class="mark" viewBox="0 0 48 40" aria-hidden="true" focusable="false">
      <circle cx="24" cy="7.5" r="3.4" class="fill-primary" />
      <path d="M22.4 10.4h3.2v2.6h-3.2z" class="fill-primary" />
      <path d="M5.5 30.5C5.5 20 13.8 12.2 24 12.2S42.5 20 42.5 30.5z" class="fill-primary" />
      <path d="M12.6 25.2c1.5-4.7 5.4-8.1 10.2-8.9" class="shine" />
      <rect x="2" y="32" width="44" height="4.6" rx="2.3" class="fill-primary" />
    </svg>
    @if (variant() !== 'mark') {
      <span class="word" aria-hidden="true">Boraê</span>
    }
  `,
  styles: [`
    :host {
      --logo-size: 40px;
      display: inline-flex;
      align-items: center;
      gap: calc(var(--logo-size) * 0.22);
      line-height: 1;
      user-select: none;
    }
    :host(.logo-stacked) { flex-direction: column; gap: calc(var(--logo-size) * 0.12); }
    .mark {
      width: calc(var(--logo-size) * 1.2);
      height: var(--logo-size);
      overflow: visible;
      transition: transform .5s cubic-bezier(.16, 1, .3, 1);
      transform-origin: 50% 90%;
    }
    :host(:hover) .mark { transform: rotate(-6deg) translateY(-1px); }
    .fill-primary { fill: var(--color-primary, #FF7200); }
    .shine { fill: none; stroke: #fff; stroke-width: 2.6; stroke-linecap: round; opacity: .55; }
    .word {
      font-family: var(--font-family-headings, 'Plus Jakarta Sans', sans-serif);
      font-weight: 800;
      font-size: calc(var(--logo-size) * 0.78);
      letter-spacing: -0.045em;
      color: var(--color-graphite, #191919);
      padding-top: calc(var(--logo-size) * 0.1);
    }
    :host(.tone-dark) .word { color: #fff; }
    @media (prefers-reduced-motion: reduce) { :host(:hover) .mark { transform: none; } }
  `]
})
export class BrandLogoComponent {
  readonly variant = input<'horizontal' | 'stacked' | 'mark'>('horizontal');
  readonly tone = input<'light' | 'dark'>('light');
  /** Altura do símbolo em px. */
  readonly size = input<number>(40);
}
