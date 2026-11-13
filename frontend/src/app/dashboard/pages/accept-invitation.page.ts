import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService, RestaurantMember } from '../../auth.service';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext, describeHttpError, ROLE_LABEL } from '../restaurant-context.service';
import { PageHeaderComponent } from '../ui/ui.components';

/** Aceitar convite — POST /restaurant-invitations/accept. O e-mail da conta precisa corresponder ao convite. */
@Component({
  selector: 'bo-accept-invitation-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, IconComponent, PageHeaderComponent],
  template: `
    <bo-page-header title="Aceitar convite" subtitle="Entre na equipe de um restaurante usando o convite que você recebeu." />

    <div class="d-card ac-card">
      @if (accepted(); as member) {
        <div class="d-alert d-alert-success" role="status">
          <bo-icon name="check-circle" [size]="18" />
          <p>Convite aceito! Você agora faz parte da equipe como <strong>{{ roleLabel[member.role] }}</strong>.</p>
        </div>
        <div class="d-form-actions ac-actions">
          <a routerLink="/app" class="d-btn d-btn-primary">Ir para a visão geral</a>
          <button type="button" class="d-btn d-btn-ghost" (click)="accepted.set(null)">Aceitar outro convite</button>
        </div>
      } @else {
        <form class="d-form" (ngSubmit)="submit()">
          <div class="d-field">
            <label for="inv-token">Token do convite</label>
            <textarea id="inv-token" name="token" class="d-input d-mono" rows="3" [(ngModel)]="token" required placeholder="Cole aqui o token recebido" autocomplete="off" spellcheck="false"></textarea>
            <span class="d-hint">Você precisa estar conectado com o mesmo e-mail para o qual o convite foi criado{{ email() ? ' (' + email() + ')' : '' }}.</span>
          </div>
          @if (error()) {
            <div class="d-alert d-alert-error" role="alert"><bo-icon name="alert" [size]="18" /><p>{{ error() }}</p></div>
          }
          <div class="d-form-actions">
            <button type="submit" class="d-btn d-btn-primary" [disabled]="saving() || !token.trim()">
              <bo-icon [name]="saving() ? 'loader' : 'ticket'" [size]="16" [spin]="saving()" /> {{ saving() ? 'Validando...' : 'Aceitar convite' }}
            </button>
          </div>
        </form>
      }
    </div>
  `,
  styles: [`.ac-card{max-width:640px}.ac-actions{margin-top:16px}`]
})
export class AcceptInvitationPage {
  private readonly auth = inject(AuthService);
  private readonly ctx = inject(RestaurantContext);
  protected readonly roleLabel = ROLE_LABEL;
  protected readonly email = () => this.ctx.user()?.email ?? '';
  protected token = '';
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly accepted = signal<RestaurantMember | null>(null);

  protected submit(): void {
    const token = this.token.trim();
    if (!token || this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    this.auth.acceptRestaurantInvitation(token).subscribe({
      next: member => {
        this.saving.set(false);
        this.token = '';
        this.accepted.set(member);
        this.ctx.reloadRestaurants(member.restaurantId);
      },
      error: error => {
        this.saving.set(false);
        this.error.set(this.message(error));
      }
    });
  }

  private message(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 403) return 'Este convite foi criado para outro e-mail. Entre com a conta convidada.';
      if (error.status === 404) return 'Token de convite inválido. Confira se ele foi copiado por completo.';
      if (error.status === 409) return 'Você já faz parte da equipe deste restaurante.';
      if (error.status === 410) return 'Este convite expirou ou já foi utilizado.';
    }
    return describeHttpError(error, 'aceitar o convite');
  }
}
