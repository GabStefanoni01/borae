import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext } from '../restaurant-context.service';
import { StateComponent } from '../ui/ui.components';
import { BusinessDay, DayOfWeek, RestaurantProfileService, profileErrorMessage } from './restaurant-profile.service';

interface EditableInterval { opensAt: string; closesAt: string; }
interface EditableDay { day: DayOfWeek; label: string; open: boolean; intervals: EditableInterval[]; }

const WEEK: { day: DayOfWeek; label: string }[] = [
  { day: 'MONDAY', label: 'Segunda-feira' },
  { day: 'TUESDAY', label: 'Terça-feira' },
  { day: 'WEDNESDAY', label: 'Quarta-feira' },
  { day: 'THURSDAY', label: 'Quinta-feira' },
  { day: 'FRIDAY', label: 'Sexta-feira' },
  { day: 'SATURDAY', label: 'Sábado' },
  { day: 'SUNDAY', label: 'Domingo' }
];

/**
 * Aba Horários — GET/PUT /restaurants/{id}/business-hours.
 * O PUT substitui a grade inteira; dias fechados são omitidos (a API os considera fechados).
 * As verificações abaixo só antecipam erros comuns; a validação oficial é do servidor.
 */
@Component({
  selector: 'bo-business-hours-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconComponent, StateComponent],
  template: `
    @if (loading()) {
      <bo-state kind="loading" [compact]="true" title="Carregando horários" />
    } @else if (loadError()) {
      <bo-state kind="error" [compact]="true" title="Não foi possível carregar os horários" [text]="loadError()">
        <button type="button" class="d-btn d-btn-secondary" (click)="load()">Tentar novamente</button>
      </bo-state>
    } @else {
      <section class="d-card">
        <div class="d-card-head">
          <span class="d-card-icon"><bo-icon name="clock" [size]="18" /></span>
          <h2>Horário de funcionamento</h2>
        </div>
        <p class="d-card-text bh-lead">Marque os dias em que o restaurante abre e informe um ou mais intervalos, como almoço e jantar. Dias fechados não precisam de horário.</p>

        <ul class="bh-days">
          @for (d of days; track d.day; let i = $index) {
            <li class="bh-day" [class.closed]="!d.open">
              <div class="bh-day-head">
                <label class="bh-switch">
                  <input type="checkbox" [(ngModel)]="d.open" (ngModelChange)="toggle(d)" [name]="'open-' + d.day">
                  <span class="bh-track" aria-hidden="true"></span>
                  <span class="bh-day-name">{{ d.label }}</span>
                </label>
                <span class="bh-state">{{ d.open ? summary(d) : 'Fechado' }}</span>
              </div>

              @if (d.open) {
                <div class="bh-intervals">
                  @for (iv of d.intervals; track $index; let j = $index) {
                    <div class="bh-interval">
                      <label class="sr-only" [for]="'o-' + d.day + j">Abre ({{ d.label }}, intervalo {{ j + 1 }})</label>
                      <input type="time" class="d-input bh-time" [id]="'o-' + d.day + j" [name]="'o-' + d.day + j" [(ngModel)]="iv.opensAt" (ngModelChange)="touch()" required>
                      <span class="bh-sep" aria-hidden="true">às</span>
                      <label class="sr-only" [for]="'c-' + d.day + j">Fecha ({{ d.label }}, intervalo {{ j + 1 }})</label>
                      <input type="time" class="d-input bh-time" [id]="'c-' + d.day + j" [name]="'c-' + d.day + j" [(ngModel)]="iv.closesAt" (ngModelChange)="touch()" required>
                      <button type="button" class="d-btn d-btn-ghost d-btn-sm bh-remove" (click)="removeInterval(d, j)" [disabled]="d.intervals.length === 1" [attr.aria-label]="'Remover intervalo ' + (j + 1) + ' de ' + d.label">
                        <bo-icon name="trash" [size]="14" />
                      </button>
                    </div>
                  }
                  <div class="bh-day-actions">
                    <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="addInterval(d)"><bo-icon name="plus" [size]="14" /> Adicionar intervalo</button>
                    <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="copyToOpenDays(d)"><bo-icon name="copy" [size]="14" /> Copiar para os dias abertos</button>
                  </div>
                  @if (problems()[i]; as p) { <p class="bh-problem" role="alert"><bo-icon name="alert" [size]="14" /> {{ p }}</p> }
                </div>
              }
            </li>
          }
        </ul>
        <p class="d-note"><bo-icon name="info" [size]="14" /> O fechamento precisa ser depois da abertura no mesmo dia. Para funcionar depois da meia-noite, registre o restante do turno no dia seguinte (ex.: 00:00 às 02:00).</p>
      </section>

      <div class="bh-bar">
        @if (feedback(); as fb) {
          <p class="bh-feedback" [class.error]="fb.type === 'error'" [attr.role]="fb.type === 'error' ? 'alert' : 'status'"><bo-icon [name]="fb.type === 'error' ? 'alert' : 'check-circle'" [size]="16" /> {{ fb.text }}</p>
        } @else {
          <p class="bh-feedback muted">{{ dirty() ? 'Você tem alterações não salvas.' : 'Grade salva.' }}</p>
        }
        <div class="bh-actions">
          <button type="button" class="d-btn d-btn-ghost" (click)="discard()" [disabled]="!dirty() || saving()">Descartar</button>
          <button type="button" class="d-btn d-btn-primary" (click)="save()" [disabled]="!dirty() || saving() || hasProblems()">
            <bo-icon [name]="saving() ? 'loader' : 'check'" [size]="16" [spin]="saving()" /> {{ saving() ? 'Salvando...' : 'Salvar horários' }}
          </button>
        </div>
      </div>
    }
  `,
  styles: [`
    .bh-lead{margin:-4px 0 16px}
    .bh-days{list-style:none;margin:0;padding:0;display:grid;gap:8px}
    .bh-day{padding:12px 14px;border:1px solid var(--d-border);border-radius:12px;background:var(--d-surface);transition:background-color .15s}
    .bh-day.closed{background:var(--d-head)}
    .bh-day-head{display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:8px}
    .bh-switch{display:inline-flex;align-items:center;gap:10px;cursor:pointer;font-weight:600}
    .bh-switch input{position:absolute;opacity:0;width:1px;height:1px}
    .bh-track{position:relative;width:36px;height:20px;border-radius:999px;background:var(--d-border-strong);transition:background-color .15s;flex-shrink:0}
    .bh-track::after{content:'';position:absolute;top:2px;left:2px;width:16px;height:16px;border-radius:50%;background:#fff;box-shadow:0 1px 2px rgba(0,0,0,.2);transition:transform .2s}
    .bh-switch input:checked+.bh-track{background:var(--d-primary)}
    .bh-switch input:checked+.bh-track::after{transform:translateX(16px)}
    .bh-switch input:focus-visible+.bh-track{outline:2px solid var(--d-primary);outline-offset:2px}
    .bh-state{font-size:13px;color:var(--d-muted)}
    .bh-intervals{display:grid;gap:8px;margin-top:12px;padding-top:12px;border-top:1px dashed var(--d-border)}
    .bh-interval{display:grid;grid-template-columns:minmax(0,130px) auto minmax(0,130px) auto;align-items:center;justify-content:start;gap:8px}
    .bh-time{width:100%;min-width:0}
    @media (max-width:480px){.bh-interval{grid-template-columns:minmax(0,1fr) auto minmax(0,1fr) auto}}
    .bh-sep{color:var(--d-muted);font-size:13px}
    .bh-remove{color:var(--d-error)}
    .bh-day-actions{display:flex;flex-wrap:wrap;gap:4px}
    .bh-problem{display:flex;align-items:center;gap:6px;margin:0;color:var(--d-error);font-size:12.5px;font-weight:500}
    .bh-bar{position:sticky;bottom:12px;z-index:5;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:12px;margin-top:16px;padding:12px 16px;border:1px solid var(--d-border);border-radius:14px;background:var(--d-surface);box-shadow:var(--d-shadow-pop)}
    .bh-feedback{display:flex;align-items:center;gap:8px;margin:0;font-size:13.5px;color:var(--d-success);font-weight:500}
    .bh-feedback.error{color:var(--d-error)}
    .bh-feedback.muted{color:var(--d-muted);font-weight:400}
    .bh-actions{display:flex;gap:10px;margin-left:auto}
  `]
})
export class BusinessHoursPage {
  private readonly ctx = inject(RestaurantContext);
  private readonly api = inject(RestaurantProfileService);

  protected days: EditableDay[] = this.fromApi([]);
  private original = '';
  protected readonly version = signal(0);
  protected readonly loading = signal(false);
  protected readonly loadError = signal('');
  protected readonly saving = signal(false);
  protected readonly feedback = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  protected readonly dirty = computed(() => { this.version(); return JSON.stringify(this.days) !== this.original; });

  /** Avisos por dia (antecipam os erros 400 mais comuns da API). */
  protected readonly problems = computed(() => {
    this.version();
    return this.days.map(d => {
      if (!d.open) return '';
      if (d.intervals.some(iv => !iv.opensAt || !iv.closesAt)) return 'Preencha a abertura e o fechamento.';
      if (d.intervals.some(iv => iv.closesAt <= iv.opensAt)) return 'O fechamento deve ser depois da abertura.';
      const sorted = [...d.intervals].sort((a, b) => a.opensAt.localeCompare(b.opensAt));
      for (let i = 1; i < sorted.length; i++) if (sorted[i].opensAt < sorted[i - 1].closesAt) return 'Os intervalos deste dia se sobrepõem.';
      return '';
    });
  });
  protected readonly hasProblems = computed(() => this.problems().some(Boolean));

  constructor() {
    effect(() => {
      const restaurant = this.ctx.selected();
      if (restaurant && this.ctx.canEditProfile()) this.load();
    });
  }

  load(): void {
    const id = this.ctx.selectedId();
    if (!id) return;
    this.loading.set(true);
    this.loadError.set('');
    this.feedback.set(null);
    this.api.getBusinessHours(id).subscribe({
      next: days => { this.setDays(days); this.loading.set(false); },
      error: error => { this.loading.set(false); this.loadError.set(profileErrorMessage(error, 'load')); }
    });
  }

  protected summary(d: EditableDay): string {
    return d.intervals.filter(iv => iv.opensAt && iv.closesAt).map(iv => `${iv.opensAt}–${iv.closesAt}`).join(' · ') || 'Aberto';
  }

  protected toggle(d: EditableDay): void {
    if (d.open && d.intervals.length === 0) d.intervals = [{ opensAt: '11:00', closesAt: '15:00' }];
    this.touch();
  }

  protected addInterval(d: EditableDay): void {
    const last = d.intervals[d.intervals.length - 1];
    d.intervals = [...d.intervals, last?.closesAt && last.closesAt < '18:00' ? { opensAt: '18:00', closesAt: '23:00' } : { opensAt: '', closesAt: '' }];
    this.touch();
  }

  protected removeInterval(d: EditableDay, index: number): void {
    d.intervals = d.intervals.filter((_, i) => i !== index);
    this.touch();
  }

  protected copyToOpenDays(source: EditableDay): void {
    for (const d of this.days) {
      if (d !== source && d.open) d.intervals = source.intervals.map(iv => ({ ...iv }));
    }
    this.touch();
    this.feedback.set({ type: 'success', text: `Horários de ${source.label.toLowerCase()} copiados para os outros dias abertos. Salve para confirmar.` });
  }

  protected touch(): void {
    this.feedback.set(null);
    this.version.update(v => v + 1);
  }

  protected discard(): void {
    this.days = JSON.parse(this.original);
    this.touch();
  }

  protected save(): void {
    const id = this.ctx.selectedId();
    if (!id || this.saving() || this.hasProblems()) return;
    const payload: BusinessDay[] = this.days
      .filter(d => d.open && d.intervals.length > 0)
      .map(d => ({ dayOfWeek: d.day, intervals: d.intervals.map(iv => ({ opensAt: iv.opensAt, closesAt: iv.closesAt })) }));
    this.saving.set(true);
    this.feedback.set(null);
    this.api.replaceBusinessHours(id, payload).subscribe({
      next: days => {
        this.saving.set(false);
        this.setDays(days);
        this.feedback.set({ type: 'success', text: 'Horários salvos com sucesso.' });
      },
      error: error => {
        this.saving.set(false);
        this.feedback.set({ type: 'error', text: profileErrorMessage(error, 'hours') });
      }
    });
  }

  private setDays(days: BusinessDay[]): void {
    this.days = this.fromApi(days);
    this.original = JSON.stringify(this.days);
    this.version.update(v => v + 1);
  }

  /** A API pode devolver horários como "HH:mm" ou "HH:mm:ss"; o campo <input type="time"> usa "HH:mm". */
  private fromApi(days: BusinessDay[]): EditableDay[] {
    return WEEK.map(({ day, label }) => {
      const found = days.find(d => d.dayOfWeek === day);
      const intervals = (found?.intervals ?? [])
        .map(iv => ({ opensAt: iv.opensAt.slice(0, 5), closesAt: iv.closesAt.slice(0, 5) }))
        .sort((a, b) => a.opensAt.localeCompare(b.opensAt));
      return { day, label, open: intervals.length > 0, intervals };
    });
  }
}
