import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth.service';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext } from '../restaurant-context.service';
import { PageHeaderComponent, StatusBadgeComponent } from '../ui/ui.components';

/** Configurações: conta pessoal (dados reais de GET /users/me, somente leitura) separada das configurações do restaurante (planejadas). */
@Component({
  selector: 'bo-settings-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent, PageHeaderComponent, StatusBadgeComponent],
  template: `
    <bo-page-header title="Configurações" subtitle="Administre as preferências e os dados associados à sua conta e ao restaurante." status="partial" />

    <div class="d-grid d-grid-2">
      <section class="d-card">
        <div class="d-card-head">
          <span class="d-card-icon"><bo-icon name="user" [size]="18" /></span>
          <h2>Conta pessoal</h2>
          <bo-status-badge status="available" />
        </div>
        @if (ctx.user(); as user) {
          <dl class="d-kv">
            <div><dt>Nome</dt><dd>{{ user.fullName }}</dd></div>
            <div><dt>E-mail</dt><dd>{{ user.email }}</dd></div>
            <div><dt>Sessão</dt><dd>Ativa neste navegador</dd></div>
          </dl>
        }
        <p class="d-note"><bo-icon name="info" [size]="14" /> Alteração de nome, e-mail e senha ainda não está disponível na API. Estes dados são exibidos somente para consulta.</p>
        <div class="d-form-actions st-actions">
          <button type="button" class="d-btn d-btn-secondary" (click)="logout()"><bo-icon name="log-out" [size]="16" /> Sair da conta</button>
        </div>
      </section>

      <section class="d-card">
        <div class="d-card-head">
          <span class="d-card-icon"><bo-icon name="store" [size]="18" /></span>
          <h2>Configurações do restaurante</h2>
          <bo-status-badge status="planned" />
        </div>
        <p class="d-card-text">Aplicam-se somente a {{ ctx.selected()?.restaurantName ?? 'o restaurante selecionado' }}, não à sua conta.</p>
        <ul class="st-list">
          <li><bo-icon name="store" [size]="16" /> Dados do estabelecimento</li>
          <li><bo-icon name="image" [size]="16" /> Identidade visual</li>
          <li><bo-icon name="sliders" [size]="16" /> Preferências operacionais</li>
          <li><bo-icon name="clock" [size]="16" /> Funcionamento</li>
          <li><bo-icon name="globe" [size]="16" /> Publicação</li>
        </ul>
        <a routerLink="/app/restaurante" class="d-link">Ver dados atuais do restaurante <bo-icon name="chevron-right" [size]="14" /></a>
      </section>
    </div>
  `,
  styles: [`
    .st-actions{margin-top:16px}
    .st-list{list-style:none;margin:14px 0 16px;padding:0;display:grid;gap:10px;color:var(--d-muted)}
    .st-list li{display:flex;align-items:center;gap:10px}
  `]
})
export class SettingsPage {
  protected readonly ctx = inject(RestaurantContext);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected logout(): void {
    this.ctx.clear();
    this.auth.logout();
    void this.router.navigateByUrl('/auth');
  }
}
