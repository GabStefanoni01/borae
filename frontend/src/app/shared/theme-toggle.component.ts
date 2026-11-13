import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { BoraeIconName, IconComponent } from './icon.component';
import { ThemeMode, ThemeService } from './theme.service';

/** Alternador de tema: Claro / Escuro / Sistema (grupo de botões de rádio acessível). */
@Component({
  selector: 'bo-theme-toggle',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  template: `
    <div class="tt" role="radiogroup" aria-label="Tema da interface">
      @for (option of options; track option.mode) {
        <button type="button" role="radio" [attr.aria-checked]="theme.mode() === option.mode"
          [class.active]="theme.mode() === option.mode" (click)="theme.setMode(option.mode)"
          [attr.aria-label]="option.label" [title]="option.label">
          <bo-icon [name]="option.icon" [size]="15" />
        </button>
      }
    </div>
  `,
  styles: [`
    .tt{display:inline-flex;gap:2px;padding:3px;border-radius:999px;background:var(--tt-bg,rgba(127,127,127,.12));border:1px solid var(--tt-border,rgba(127,127,127,.18))}
    button{display:grid;place-items:center;width:30px;height:28px;border:0;border-radius:999px;background:transparent;color:var(--tt-color,#707070);cursor:pointer;transition:background-color .15s,color .15s}
    button:hover{color:var(--tt-active-color,#191919)}
    button.active{background:var(--tt-active-bg,#fff);color:var(--tt-active-color,#191919);box-shadow:0 1px 3px rgba(0,0,0,.12)}
  `]
})
export class ThemeToggleComponent {
  protected readonly theme = inject(ThemeService);
  protected readonly options: { mode: ThemeMode; label: string; icon: BoraeIconName }[] = [
    { mode: 'light', label: 'Tema claro', icon: 'sun' },
    { mode: 'dark', label: 'Tema escuro', icon: 'moon' },
    { mode: 'system', label: 'Seguir o sistema', icon: 'monitor' }
  ];
}
