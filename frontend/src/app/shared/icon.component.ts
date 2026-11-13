import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Biblioteca de ícones do Boraê.
 * Ícones vetoriais de traço (24x24, stroke 2) desenhados para a marca, servidos
 * por um componente Angular standalone — sem emojis e sem dependências externas.
 * Uso: <bo-icon name="arrow-right" [size]="18" />
 */

const circle = (cx: number, cy: number, r: number) =>
  `M${cx - r} ${cy}a${r} ${r} 0 1 0 ${r * 2} 0a${r} ${r} 0 1 0 ${-r * 2} 0`;

const rect = (x: number, y: number, w: number, h: number, r = 0) =>
  r === 0
    ? `M${x} ${y}h${w}v${h}h${-w}z`
    : `M${x + r} ${y}h${w - 2 * r}a${r} ${r} 0 0 1 ${r} ${r}v${h - 2 * r}a${r} ${r} 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a${r} ${r} 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a${r} ${r} 0 0 1 ${r} ${-r}z`;

export const BORAE_ICONS = {
  'arrow-right': ['M5 12h14', 'm12 5 7 7-7 7'],
  'arrow-left': ['M19 12H5', 'm12 19-7-7 7-7'],
  'arrow-up-right': ['M7 17 17 7', 'M7 7h10v10'],
  check: ['M20 6 9 17l-5-5'],
  'check-circle': [circle(12, 12, 10), 'm8 12 3 3 5-6'],
  x: ['M18 6 6 18', 'm6 6 12 12'],
  plus: ['M12 5v14', 'M5 12h14'],
  menu: ['M4 7h16', 'M4 12h16', 'M4 17h16'],
  info: [circle(12, 12, 10), 'M12 16v-4', 'M12 8h.01'],
  alert: [circle(12, 12, 10), 'M12 8v4', 'M12 16h.01'],
  sparkles: ['M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z', 'M19 3v4', 'M17 5h4', 'M5 17v4', 'M3 19h4'],
  store: ['M4 10v10h16V10', 'M3 10 5 4h14l2 6', 'M3 10a3 3 0 0 0 6 0 3 3 0 0 0 6 0 3 3 0 0 0 6 0', 'M10 20v-5h4v5'],
  utensils: ['M7 3v18', 'M4 3v5a3 3 0 0 0 6 0V3', 'M17 21V3c-2 1.5-3 4-3 7v4h3'],
  cloche: ['M3 18h18', 'M5 18a7 7 0 0 1 14 0', 'M12 11V9', circle(12, 7.5, 1.5)],
  'book-open': ['M2 4h6a4 4 0 0 1 4 4v13a3 3 0 0 0-3-3H2z', 'M22 4h-6a4 4 0 0 0-4 4v13a3 3 0 0 1 3-3h7z'],
  cube: ['M12 2 3 7v10l9 5 9-5V7z', 'm3 7 9 5 9-5', 'M12 12v10'],
  lightbulb: ['M9 18h6', 'M10 22h4', 'M12 2a7 7 0 0 0-4 12.7V17h8v-2.3A7 7 0 0 0 12 2z'],
  star: ['M12 2l3.1 6.3 6.9 1-5 4.9 1.2 6.8L12 17.8 5.8 21l1.2-6.8-5-4.9 6.9-1z'],
  camera: ['M3 8a2 2 0 0 1 2-2h2.5L9 4h6l1.5 2H19a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z', circle(12, 13, 3.5)],
  smartphone: [rect(5, 2, 14, 20, 2), 'M12 18h.01'],
  link: ['M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7', 'M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7'],
  gift: [rect(3, 8, 18, 4, 1), 'M12 8v13', 'M19 12v9H5v-9', 'M7.5 8a2.5 2.5 0 0 1 0-5C10 3 12 8 12 8s2-5 4.5-5a2.5 2.5 0 0 1 0 5'],
  search: [circle(11, 11, 7), 'm20 20-3.5-3.5'],
  building: [rect(4, 2, 16, 20, 2), 'M9 22v-4h6v4', 'M8 6h.01', 'M12 6h.01', 'M16 6h.01', 'M8 10h.01', 'M12 10h.01', 'M16 10h.01', 'M8 14h.01', 'M12 14h.01', 'M16 14h.01'],
  users: ['M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2', circle(9, 7, 4), 'M22 21v-2a4 4 0 0 0-3-3.9', 'M16 3.1a4 4 0 0 1 0 7.8'],
  target: [circle(12, 12, 10), circle(12, 12, 6), circle(12, 12, 2)],
  heart: ['M19 14c1.5-1.5 3-3.2 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.8 0-3 .5-4.5 2-1.5-1.5-2.7-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4 3 5.5l7 7z'],
  'chef-hat': ['M6 13.9A4 4 0 0 1 7.2 6a5 5 0 0 1 9.6 0A4 4 0 0 1 18 13.9V21H6z', 'M6 17h12'],
  'qr-code': [rect(3, 3, 7, 7, 1), rect(14, 3, 7, 7, 1), rect(3, 14, 7, 7, 1), 'M14 14h3v3h-3z', 'M21 14v.01', 'M14 21h.01', 'M17 21h4v-4'],
  leaf: ['M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.5 19 2c1 2 2 4.2 2 8 0 5.5-4.8 10-10 10z', 'M2 21c0-3 1.9-5.4 5.1-6'],
  layers: ['m12 2 10 5-10 5L2 7z', 'm2 17 10 5 10-5', 'm2 12 10 5 10-5'],
  mail: [rect(2, 4, 20, 16, 2), 'm22 7-10 6L2 7'],
  lock: [rect(4, 11, 16, 10, 2), 'M8 11V7a4 4 0 0 1 8 0v4'],
  user: [circle(12, 8, 4), 'M4 21a8 8 0 0 1 16 0'],
  eye: ['M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z', circle(12, 12, 3)],
  'eye-off': ['m3 3 18 18', 'M10.6 5.1A10 10 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-2.9 3.9', 'M6.6 6.6C3.8 8.4 2 12 2 12s3.5 7 10 7a9.7 9.7 0 0 0 5.4-1.6', 'M9.9 9.9a3 3 0 0 0 4.2 4.2'],
  'log-out': ['M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4', 'm16 17 5-5-5-5', 'M21 12H9'],
  'shield-check': ['M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z', 'm9 12 2 2 4-4'],
  loader: ['M21 12a9 9 0 1 1-6.2-8.6'],
  'map-pin': ['M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 0 1 16 0z', circle(12, 10, 3)]
} as const satisfies Record<string, readonly string[]>;

export type BoraeIconName = keyof typeof BORAE_ICONS;

@Component({
  selector: 'bo-icon',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'bo-icon',
    '[class.bo-icon-spin]': 'spin()',
    '[attr.aria-hidden]': 'label() ? null : "true"',
    '[attr.role]': 'label() ? "img" : null',
    '[attr.aria-label]': 'label() || null'
  },
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      [attr.width]="size()"
      [attr.height]="size()"
      [attr.stroke-width]="strokeWidth()"
      stroke-linecap="round"
      stroke-linejoin="round"
      focusable="false"
    >
      @for (d of paths(); track $index) {
        <path [attr.d]="d" />
      }
    </svg>
  `,
  styles: [`
    :host { display: inline-flex; flex-shrink: 0; line-height: 0; }
    svg { display: block; }
    :host(.bo-icon-spin) svg { animation: bo-spin .9s linear infinite; }
    @keyframes bo-spin { to { transform: rotate(360deg); } }
  `]
})
export class IconComponent {
  readonly name = input.required<BoraeIconName>();
  readonly size = input<number>(20);
  readonly strokeWidth = input<number>(2);
  readonly spin = input<boolean>(false);
  /** Texto acessível. Sem label, o ícone é decorativo (aria-hidden). */
  readonly label = input<string>('');

  protected readonly paths = computed<readonly string[]>(() => BORAE_ICONS[this.name()] ?? []);
}
