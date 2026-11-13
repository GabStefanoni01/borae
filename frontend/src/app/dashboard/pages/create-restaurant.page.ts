import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth.service';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext, describeHttpError } from '../restaurant-context.service';
import { PageHeaderComponent } from '../ui/ui.components';

/** Cadastro de restaurante — POST /restaurants (nome obrigatório até 180, descrição opcional até 1000). */
@Component({
  selector: 'bo-create-restaurant-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, IconComponent, PageHeaderComponent],
  template: `
    <bo-page-header title="Cadastrar restaurante" subtitle="Cadastre seu estabelecimento para criar seu espaço de gestão no Boraê." />

    <div class="cr-layout">
      <form class="d-card d-form" (ngSubmit)="submit()" #f="ngForm">
        <div class="d-field">
          <label for="r-name">Nome do estabelecimento</label>
          <input id="r-name" name="name" class="d-input" [(ngModel)]="name" required maxlength="180" placeholder="Ex.: Café da Praça" autocomplete="organization">
          <span class="d-hint"><span>Obrigatório.</span><span>{{ name.length }}/180</span></span>
        </div>
        <div class="d-field">
          <label for="r-desc">Descrição <span>(opcional)</span></label>
          <textarea id="r-desc" name="description" class="d-input" [(ngModel)]="description" maxlength="1000" rows="4" placeholder="Conte um pouco sobre o estabelecimento"></textarea>
          <span class="d-hint"><span></span><span>{{ description.length }}/1000</span></span>
        </div>
        @if (error()) {
          <div class="d-alert d-alert-error" role="alert"><bo-icon name="alert" [size]="18" /><p>{{ error() }}</p></div>
        }
        <div class="d-form-actions">
          <button type="submit" class="d-btn d-btn-primary" [disabled]="saving() || !name.trim()">
            <bo-icon [name]="saving() ? 'loader' : 'check'" [size]="16" [spin]="saving()" /> {{ saving() ? 'Cadastrando...' : 'Cadastrar restaurante' }}
          </button>
          <a routerLink="/app" class="d-btn d-btn-ghost">Cancelar</a>
        </div>
      </form>

      <aside class="d-card cr-aside">
        <h2>Como funciona</h2>
        <ul>
          <li><bo-icon name="shield-check" [size]="16" /> Você será o <strong>proprietário</strong> do novo restaurante, definido pelo servidor.</li>
          <li><bo-icon name="globe" [size]="16" /> O identificador público (slug) é gerado automaticamente.</li>
          <li><bo-icon name="users" [size]="16" /> Depois, você poderá convidar gerentes e equipe.</li>
        </ul>
      </aside>
    </div>
  `,
  styles: [`
    .cr-layout{display:grid;gap:16px;grid-template-columns:minmax(0,1fr) 300px;align-items:start}
    .cr-aside h2{margin:0 0 12px;font-size:15px}
    .cr-aside ul{list-style:none;margin:0;padding:0;display:grid;gap:12px;color:var(--d-ink-2)}
    .cr-aside li{display:flex;gap:10px}
    .cr-aside bo-icon{color:var(--d-primary);margin-top:2px}
    @media (max-width:900px){.cr-layout{grid-template-columns:1fr}}
  `]
})
export class CreateRestaurantPage {
  private readonly auth = inject(AuthService);
  private readonly ctx = inject(RestaurantContext);
  private readonly router = inject(Router);
  protected name = '';
  protected description = '';
  protected readonly saving = signal(false);
  protected readonly error = signal('');

  protected submit(): void {
    const name = this.name.trim();
    if (!name || this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    this.auth.createRestaurant({ name, description: this.description.trim() || undefined }).subscribe({
      next: access => {
        this.ctx.addRestaurant(access);
        this.saving.set(false);
        void this.router.navigateByUrl('/app');
      },
      error: error => {
        this.saving.set(false);
        this.error.set(describeHttpError(error, 'cadastrar o restaurante'));
      }
    });
  }
}
