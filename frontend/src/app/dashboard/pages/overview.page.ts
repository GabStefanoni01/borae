import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../auth.service';
import { IconComponent } from '../../shared/icon.component';
import { DASHBOARD_MODULES } from '../modules';
import { RestaurantContext } from '../restaurant-context.service';
import { PageHeaderComponent, RoleBadgeComponent, StateComponent, StatusBadgeComponent } from '../ui/ui.components';

/** Início do painel. Exibe apenas dados reais da API — sem métricas de vendas/pedidos (não existem endpoints). */
@Component({
  selector: 'bo-overview-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent, PageHeaderComponent, StateComponent, StatusBadgeComponent, RoleBadgeComponent],
  template: `
    <bo-page-header title="Visão geral" subtitle="Bem-vindo ao seu espaço de gestão no Boraê." />

    @if (!ctx.hasRestaurants()) {
      <bo-state kind="empty" icon="store" title="Vamos começar pelo seu restaurante?"
        text="Cadastre seu estabelecimento para criar seu espaço de gestão no Boraê.">
        <a routerLink="/app/novo-restaurante" class="d-btn d-btn-primary"><bo-icon name="plus" [size]="16" /> Cadastrar restaurante</a>
        <a routerLink="/app/convite" class="d-btn d-btn-secondary"><bo-icon name="ticket" [size]="16" /> Tenho um convite</a>
      </bo-state>
    } @else if (ctx.selected(); as r) {
      @if (ctx.justCreated(); as created) {
        <div class="d-alert d-alert-success ov-created" role="status">
          <bo-icon name="check-circle" [size]="18" />
          <p><strong>Restaurante cadastrado!</strong> Você está no painel de {{ created }} como proprietário.</p>
          <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="ctx.justCreated.set(null)" aria-label="Fechar aviso"><bo-icon name="x" [size]="14" /></button>
        </div>
      }
      <section class="ov-hero">
        <div>
          <p class="ov-eyebrow">Olá, {{ firstName() }}</p>
          <h2>Seu restaurante, conectado a novas experiências.</h2>
          <p>Este é o centro de gestão do seu estabelecimento no Boraê. Aqui você encontrará as ferramentas para administrar sua presença digital e acompanhar a evolução dos recursos da plataforma.</p>
        </div>
        <p class="ov-context">Gerencie seu estabelecimento e acompanhe a evolução da sua presença na plataforma.</p>
      </section>

      <div class="d-grid d-grid-2">
        <!-- Estabelecimento: dados reais de GET /users/me/restaurants -->
        <article class="d-card">
          <div class="d-card-head">
            <span class="d-card-icon"><bo-icon name="store" [size]="18" /></span>
            <h2>Estabelecimento</h2>
            <a routerLink="/app/restaurante" class="d-link">Detalhes <bo-icon name="chevron-right" [size]="14" /></a>
          </div>
          <dl class="d-kv">
            <div><dt>Nome</dt><dd>{{ r.restaurantName }}</dd></div>
            <div><dt>Identificador público</dt><dd class="d-mono">{{ r.restaurantSlug }}</dd></div>
            <div><dt>Situação</dt><dd><span class="ov-active"><span></span>Ativo</span></dd></div>
            <div><dt>Seu papel</dt><dd><bo-role-badge [role]="r.role" /></dd></div>
          </dl>
        </article>

        <!-- Equipe: contagem real somente quando o papel permite consultar -->
        <article class="d-card">
          <div class="d-card-head">
            <span class="d-card-icon"><bo-icon name="users" [size]="18" /></span>
            <h2>Equipe</h2>
            @if (ctx.canViewTeam()) { <a routerLink="/app/equipe" class="d-link">Abrir <bo-icon name="chevron-right" [size]="14" /></a> }
          </div>
          @if (!ctx.canViewTeam()) {
            <p class="d-card-text">Seu papel neste restaurante não inclui acesso à lista da equipe.</p>
          } @else if (teamCount() === null && !teamError()) {
            <p class="d-card-text">Carregando equipe...</p>
          } @else if (teamError()) {
            <p class="d-card-text">Não foi possível consultar a equipe agora.</p>
          } @else {
            <p class="ov-number">{{ teamCount() }}</p>
            <p class="d-card-text">{{ teamCount() === 1 ? 'membro ativo' : 'membros ativos' }} neste restaurante.</p>
          }
        </article>
      </div>

      <section class="d-section">
        <div class="ov-head">
          <h2 class="d-section-title">Seus restaurantes</h2>
          <a routerLink="/app/novo-restaurante" class="d-btn d-btn-secondary d-btn-sm"><bo-icon name="plus" [size]="14" /> Novo restaurante</a>
        </div>
        <ul class="ov-restaurants">
          @for (item of ctx.restaurants(); track item.restaurantId) {
            <li [class.current]="item.restaurantId === ctx.selectedId()">
              <span class="d-avatar d-avatar-restaurant" aria-hidden="true">{{ initials(item.restaurantName) }}</span>
              <span class="ov-r-text"><strong>{{ item.restaurantName }}</strong><small class="d-mono">{{ item.restaurantSlug }}</small></span>
              <bo-role-badge [role]="item.role" />
              @if (item.restaurantId === ctx.selectedId()) {
                <span class="ov-current"><bo-icon name="check" [size]="14" /> Em gestão</span>
              } @else {
                <button type="button" class="d-btn d-btn-primary d-btn-sm" (click)="open(item.restaurantId)">Abrir painel <bo-icon name="arrow-right" [size]="14" /></button>
              }
            </li>
          }
        </ul>
      </section>

      <section class="d-section">
        <h2 class="d-section-title">Ações rápidas</h2>
        <div class="ov-actions">
          @if (ctx.canViewTeam()) {
            <a routerLink="/app/equipe" class="ov-action"><bo-icon name="users" [size]="18" /><span><strong>Ver equipe</strong><small>Membros e papéis do restaurante</small></span></a>
          }
          @if (ctx.canManageTeam()) {
            <a routerLink="/app/equipe" fragment="convidar" class="ov-action"><bo-icon name="user-plus" [size]="18" /><span><strong>Convidar pessoa</strong><small>Gerente ou equipe</small></span></a>
          }
          <a routerLink="/app/novo-restaurante" class="ov-action"><bo-icon name="plus" [size]="18" /><span><strong>Cadastrar restaurante</strong><small>Você será o proprietário</small></span></a>
          <a routerLink="/app/convite" class="ov-action"><bo-icon name="ticket" [size]="18" /><span><strong>Aceitar convite</strong><small>Entrar na equipe de outro restaurante</small></span></a>
        </div>
      </section>

      <section class="d-section">
        <article class="d-card">
          <div class="d-card-head">
            <span class="d-card-icon"><bo-icon name="sparkles" [size]="18" /></span>
            <h2>Seu espaço está evoluindo.</h2>
          </div>
          <p class="d-card-text">Novas ferramentas de cardápio, experiências digitais e relacionamento estão sendo desenvolvidas para ampliar as possibilidades do Boraê.</p>
          <ul class="ov-modules">
            @for (m of modules; track m.id) {
              <li><a [routerLink]="m.path ? '/app/' + m.path : '/app'"><bo-icon [name]="m.icon" [size]="16" /><span>{{ m.label }}</span><bo-status-badge [status]="m.status" /></a></li>
            }
          </ul>
        </article>
      </section>
    }
  `,
  styles: [`
    .ov-created{margin-bottom:16px;align-items:center}
    .ov-created p{flex:1}
    .ov-head{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-bottom:12px}
    .ov-head .d-section-title{margin:0}
    .ov-restaurants{list-style:none;margin:0;padding:0;display:grid;gap:8px}
    .ov-restaurants li{display:flex;flex-wrap:wrap;align-items:center;gap:12px;padding:12px 14px;border:1px solid var(--d-border);border-radius:14px;background:var(--d-surface)}
    .ov-restaurants li.current{border-color:var(--d-primary);box-shadow:0 0 0 3px var(--d-primary-soft)}
    .ov-r-text{flex:1;min-width:160px;display:flex;flex-direction:column}
    .ov-r-text small{color:var(--d-muted);font-size:12px}
    .ov-current{display:inline-flex;align-items:center;gap:6px;font-size:13px;font-weight:600;color:var(--d-primary-text)}
    .ov-hero{display:flex;flex-wrap:wrap;align-items:flex-end;justify-content:space-between;gap:16px;margin-bottom:16px;padding:28px;border-radius:16px;background:linear-gradient(120deg,var(--d-cream) 0%,var(--d-primary-soft) 100%);border:1px solid var(--d-border)}
    .ov-hero h2{margin:4px 0 8px;font-size:22px;font-weight:700;max-width:560px}
    .ov-hero p{margin:0;color:var(--d-ink-2);max-width:620px}
    .ov-eyebrow{font-size:12.5px;font-weight:600;letter-spacing:.06em;text-transform:uppercase;color:var(--d-primary-text) !important}
    .ov-context{font-size:13px;max-width:260px !important}
    .ov-active{display:inline-flex;align-items:center;gap:6px;color:var(--d-success);font-weight:600}
    .ov-active span{width:8px;height:8px;border-radius:50%;background:var(--d-success);box-shadow:0 0 0 3px var(--d-success-soft)}
    .ov-number{margin:0;font-size:34px;font-weight:700;letter-spacing:-.03em;line-height:1.1}
    .ov-actions{display:grid;gap:12px;grid-template-columns:repeat(auto-fit,minmax(220px,1fr))}
    .ov-action{display:flex;align-items:center;gap:12px;padding:14px 16px;border:1px solid var(--d-border);border-radius:14px;background:var(--d-surface);transition:border-color .15s,box-shadow .15s,transform .15s}
    .ov-action:hover{border-color:#FFC999;box-shadow:0 6px 18px rgba(25,25,25,.06);transform:translateY(-1px)}
    .ov-action>bo-icon{display:grid;place-items:center;width:36px;height:36px;border-radius:10px;background:var(--d-primary-soft);color:var(--d-primary)}
    .ov-action strong,.ov-action small{display:block}
    .ov-action small{color:var(--d-muted);font-size:12.5px}
    .ov-modules{list-style:none;margin:16px 0 0;padding:0;display:grid;gap:8px;grid-template-columns:repeat(auto-fill,minmax(250px,1fr))}
    .ov-modules a{display:flex;align-items:center;gap:10px;padding:10px 12px;border:1px solid var(--d-border-soft);border-radius:10px;background:var(--d-head)}
    .ov-modules a:hover{border-color:var(--d-border);background:var(--d-surface)}
    .ov-modules bo-icon{color:var(--d-faint)}
    .ov-modules bo-status-badge{margin-left:auto}
  `]
})
export class OverviewPage {
  protected readonly ctx = inject(RestaurantContext);
  private readonly auth = inject(AuthService);
  protected readonly modules = DASHBOARD_MODULES.filter(m => m.id !== 'inicio');
  protected readonly teamCount = signal<number | null>(null);
  protected readonly teamError = signal(false);
  protected open(restaurantId: string): void {
    this.ctx.justCreated.set(null);
    this.ctx.select(restaurantId);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  protected initials(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase();
  }

  protected readonly firstName = computed(() => this.ctx.user()?.fullName.split(' ')[0] ?? '');

  constructor() {
    // Contagem real da equipe (GET /restaurants/{id}/members), apenas se o papel permitir.
    effect(onCleanup => {
      const restaurant = this.ctx.selected();
      const allowed = this.ctx.canViewTeam();
      this.teamCount.set(null);
      this.teamError.set(false);
      if (!restaurant || !allowed) return;
      const sub = this.auth.getRestaurantMembers(restaurant.restaurantId).subscribe({
        next: members => this.teamCount.set(members.length),
        error: () => this.teamError.set(true)
      });
      onCleanup(() => sub.unsubscribe());
    });
  }
}
