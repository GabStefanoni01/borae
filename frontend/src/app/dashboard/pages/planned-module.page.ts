import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../../shared/icon.component';
import { SUBSCRIPTION_PRICES, findModule } from '../modules';
import { PageHeaderComponent, StateComponent } from '../ui/ui.components';

/**
 * Página explicativa de módulo planejado (dados em dashboard/modules.ts).
 * Não contém formulários nem ações: apenas descreve o recurso e seu status.
 * O id do módulo chega pela rota (data.moduleId → input, via withComponentInputBinding).
 */
@Component({
  selector: 'bo-planned-module-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent, PageHeaderComponent, StateComponent],
  template: `
    @if (module(); as m) {
      <bo-page-header [title]="m.title" [subtitle]="m.subtitle" [status]="m.status" />

      <bo-state kind="planned" [icon]="m.icon" title="Estamos preparando esta experiência."
        text="Este recurso faz parte da evolução do Boraê e está sendo planejado para ampliar as possibilidades do seu restaurante. Ele ainda não está disponível.">
        <a routerLink="/app" class="d-btn d-btn-secondary"><bo-icon name="arrow-left" [size]="16" /> Voltar à visão geral</a>
      </bo-state>

      <div class="d-grid d-grid-2 pm-grid">
        <section class="d-card">
          <div class="d-card-head"><span class="d-card-icon"><bo-icon name="compass" [size]="18" /></span><h2>Sobre o recurso</h2></div>
          @if (m.summary) { <p class="d-card-text">{{ m.summary }}</p> }
          @if (m.benefit) {
            <p class="pm-benefit"><bo-icon name="sparkles" [size]="16" /><span><strong>Benefício esperado:</strong> {{ m.benefit }}</span></p>
          }
        </section>

        @if (m.features?.length) {
          <section class="d-card">
            <div class="d-card-head"><span class="d-card-icon"><bo-icon name="layers" [size]="18" /></span><h2>O que está previsto</h2></div>
            <ul class="pm-list">@for (f of m.features; track f) { <li><bo-icon name="check" [size]="14" />{{ f }}</li> }</ul>
          </section>
        }
      </div>

      @if (m.id === 'assinatura') {
        <section class="d-section">
          <h2 class="d-section-title">Valores aprovados</h2>
          <div class="d-table-wrap">
            <table class="d-table">
              <caption class="sr-only">Valores aprovados do plano único — cobrança ainda não integrada</caption>
              <thead><tr><th scope="col">Periodicidade</th><th scope="col">Preço</th><th scope="col">Desconto</th></tr></thead>
              <tbody>
                @for (p of prices; track p.period) {
                  <tr><td data-label="Periodicidade"><strong>{{ p.period }}</strong></td><td data-label="Preço">{{ p.price }}</td><td data-label="Desconto">{{ p.discount }}</td></tr>
                }
              </tbody>
            </table>
          </div>
          <p class="d-note"><bo-icon name="info" [size]="14" /> Plano único, sem comissão por pedido ou transação. A contratação ainda não está disponível pelo painel.</p>
        </section>
      }

      @if (m.decisions; as d) {
        <section class="d-section">
          <h2 class="d-section-title">{{ d.title }}</h2>
          <div class="d-card pm-decisions">
            <ul class="pm-list">@for (item of d.items; track item) { <li><bo-icon name="info" [size]="14" />{{ item }}</li> }</ul>
          </div>
        </section>
      }
    } @else {
      <bo-state kind="not-found" title="Módulo não encontrado" text="Este endereço não corresponde a um módulo do painel.">
        <a routerLink="/app" class="d-btn d-btn-secondary">Voltar à visão geral</a>
      </bo-state>
    }
  `,
  styles: [`
    .pm-grid{margin-top:16px}
    .pm-benefit{display:flex;gap:10px;margin:14px 0 0;padding:12px;border-radius:10px;background:var(--d-cream);color:var(--d-ink-2)}
    .pm-benefit bo-icon{color:var(--d-primary);margin-top:2px}
    .pm-list{list-style:none;margin:0;padding:0;display:grid;gap:10px;color:var(--d-ink-2)}
    .pm-list li{display:flex;gap:10px}
    .pm-list bo-icon{color:var(--d-primary);margin-top:3px}
    .pm-decisions .pm-list bo-icon{color:var(--d-muted)}
  `]
})
export class PlannedModulePage {
  readonly moduleId = input('');
  protected readonly module = computed(() => findModule(this.moduleId()));
  protected readonly prices = SUBSCRIPTION_PRICES;
}
