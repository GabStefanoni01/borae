import { ChangeDetectionStrategy, Component, OnDestroy, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth.service';
import { SLOW_REQUEST_MS, SLOW_SERVER_MESSAGE } from '../../shared/api-warmup';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext, describeHttpError } from '../restaurant-context.service';
import { PageHeaderComponent } from '../ui/ui.components';

/**
 * Cadastro de restaurante — POST /restaurants (nome obrigatório até 180, descrição opcional até 1000).
 * Ao concluir, o restaurante criado fica selecionado e a pessoa entra direto no painel dele.
 */
@Component({
  selector: 'bo-create-restaurant-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, IconComponent, PageHeaderComponent],
  template: `
    @if (welcome) {
      <div class="d-alert d-alert-success cr-welcome" role="status">
        <bo-icon name="check-circle" [size]="18" />
        <p><strong>Conta criada com sucesso{{ firstName() ? ', ' + firstName() : '' }}!</strong> Agora cadastre seu restaurante para abrir seu espaço de gestão.</p>
      </div>
    }

    <bo-page-header title="Cadastrar restaurante" subtitle="Cadastre seu estabelecimento para criar seu espaço de gestão no Boraê." />

    <ol class="cr-steps" aria-label="Etapas">
      <li class="done"><span><bo-icon name="check" [size]="14" /></span>Conta Boraê</li>
      <li class="current" aria-current="step"><span>2</span>Dados do restaurante</li>
      <li><span>3</span>Painel do restaurante</li>
    </ol>

    <div class="cr-layout">
      <form class="d-card d-form" (ngSubmit)="submit()" #f="ngForm">
        <div class="d-field">
          <label for="r-name">Nome do estabelecimento</label>
          <input id="r-name" name="name" class="d-input" [(ngModel)]="name" required maxlength="180" placeholder="Ex.: Café da Praça" autocomplete="organization" [disabled]="saving()">
          <span class="d-hint"><span>Obrigatório. É assim que o restaurante aparecerá no painel.</span><span>{{ name.length }}/180</span></span>
        </div>
        <div class="d-field">
          <label for="r-desc">Descrição <span>(opcional)</span></label>
          <textarea id="r-desc" name="description" class="d-input" [(ngModel)]="description" maxlength="1000" rows="4" placeholder="Conte um pouco sobre o estabelecimento" [disabled]="saving()"></textarea>
          <span class="d-hint"><span></span><span>{{ description.length }}/1000</span></span>
        </div>
        @if (error()) {
          <div class="d-alert d-alert-error" role="alert"><bo-icon name="alert" [size]="18" /><p>{{ error() }}</p></div>
        }
        @if (slow()) {
          <div class="d-alert d-alert-warning" role="status"><bo-icon name="clock" [size]="18" /><p>{{ slowMessage }}</p></div>
        }
        <div class="d-form-actions">
          <button type="submit" class="d-btn d-btn-primary" [disabled]="saving() || !name.trim()">
            <bo-icon [name]="saving() ? 'loader' : 'arrow-right'" [size]="16" [spin]="saving()" />
            {{ saving() ? 'Cadastrando restaurante...' : 'Cadastrar e abrir o painel' }}
          </button>
          @if (ctx.hasRestaurants()) {
            <a routerLink="/app" class="d-btn d-btn-ghost">Cancelar</a>
          }
        </div>
      </form>

      <aside class="d-card cr-aside">
        <h2>Como funciona</h2>
        <ul>
          <li><bo-icon name="shield-check" [size]="16" /> <span>Você será o <strong>proprietário</strong> do novo restaurante, definido pelo servidor.</span></li>
          <li><bo-icon name="globe" [size]="16" /> <span>O identificador público (slug) é gerado automaticamente.</span></li>
          <li><bo-icon name="home" [size]="16" /> <span>Ao concluir, o painel do restaurante abre na hora.</span></li>
          <li><bo-icon name="users" [size]="16" /> <span>Depois, você poderá convidar gerentes e equipe.</span></li>
        </ul>
        <p class="cr-invite">Vai trabalhar no restaurante de outra pessoa? <a routerLink="/app/convite" class="d-link">Aceitar convite</a></p>
      </aside>
    </div>
  `,
  styles: [`
    .cr-welcome{margin-bottom:20px}
    .cr-steps{display:flex;flex-wrap:wrap;gap:8px 20px;list-style:none;margin:-8px 0 20px;padding:0;color:var(--d-muted);font-size:13px;font-weight:500}
    .cr-steps li{display:flex;align-items:center;gap:8px}
    .cr-steps span{display:grid;place-items:center;width:24px;height:24px;border-radius:50%;border:1px solid var(--d-border-strong);font-size:12px;font-weight:600}
    .cr-steps .done span{background:var(--d-success-soft);border-color:var(--d-success-line);color:var(--d-success)}
    .cr-steps .current{color:var(--d-ink);font-weight:600}
    .cr-steps .current span{background:var(--d-primary);border-color:var(--d-primary);color:#fff}
    .cr-layout{display:grid;gap:16px;grid-template-columns:minmax(0,1fr) 300px;align-items:start}
    .cr-aside h2{margin:0 0 12px;font-size:15px}
    .cr-aside ul{list-style:none;margin:0;padding:0;display:grid;gap:12px;color:var(--d-ink-2)}
    .cr-aside li{display:flex;gap:10px}
    .cr-aside bo-icon{color:var(--d-primary);margin-top:2px;flex-shrink:0}
    .cr-invite{margin:16px 0 0;padding-top:14px;border-top:1px solid var(--d-border);font-size:13px;color:var(--d-muted)}
    @media (max-width:900px){.cr-layout{grid-template-columns:1fr}}
  `]
})
export class CreateRestaurantPage implements OnDestroy {
  private readonly auth = inject(AuthService);
  protected readonly ctx = inject(RestaurantContext);
  private readonly router = inject(Router);
  protected readonly welcome = inject(ActivatedRoute).snapshot.queryParamMap.has('boas-vindas');
  protected readonly firstName = () => this.ctx.user()?.fullName.split(' ')[0] ?? '';
  protected readonly slowMessage = SLOW_SERVER_MESSAGE;
  protected name = '';
  protected description = '';
  protected readonly saving = signal(false);
  protected readonly slow = signal(false);
  protected readonly error = signal('');
  private slowTimer?: ReturnType<typeof setTimeout>;

  protected submit(): void {
    const name = this.name.trim();
    if (!name || this.saving()) return;
    this.saving.set(true);
    this.slow.set(false);
    this.error.set('');
    this.slowTimer = setTimeout(() => this.slow.set(true), SLOW_REQUEST_MS);
    this.auth.createRestaurant({ name, description: this.description.trim() || undefined }).subscribe({
      next: access => {
        this.stop();
        this.ctx.addRestaurant(access);
        void this.router.navigateByUrl('/app');
      },
      error: error => {
        this.stop();
        this.error.set(describeHttpError(error, 'cadastrar o restaurante'));
      }
    });
  }

  ngOnDestroy(): void {
    clearTimeout(this.slowTimer);
  }

  private stop(): void {
    clearTimeout(this.slowTimer);
    this.saving.set(false);
    this.slow.set(false);
  }
}
