import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IconComponent } from '../../shared/icon.component';
import { ProfileStore } from '../profile/profile.store';
import { PUBLICATION_LABEL } from '../profile/restaurant-profile.service';
import { RestaurantContext } from '../restaurant-context.service';
import { PageHeaderComponent, RoleBadgeComponent, StateComponent } from '../ui/ui.components';

/**
 * Meu restaurante — layout com abas Perfil, Horários e Imagens.
 * API: GET/PATCH /restaurants/{id}, GET/PUT /business-hours e GET/POST/DELETE /media (OWNER e MANAGER).
 */
@Component({
  selector: 'bo-restaurant-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ProfileStore],
  imports: [RouterLink, RouterLinkActive, RouterOutlet, IconComponent, PageHeaderComponent, StateComponent, RoleBadgeComponent],
  template: `
    <bo-page-header title="Meu restaurante" subtitle="Gerencie as informações que representam seu estabelecimento dentro do Boraê." status="available" />

    @if (ctx.selected(); as r) {
      @if (!ctx.canEditProfile()) {
        <bo-state kind="forbidden" title="Perfil disponível para proprietário e gerentes"
          [text]="'Seu papel em ' + r.restaurantName + ' é Equipe. A edição do perfil, dos horários e das imagens é feita pelo proprietário ou por gerentes.'" />
      } @else {
        <section class="rp-summary d-card">
          <span class="d-avatar d-avatar-restaurant rp-avatar" aria-hidden="true">{{ initials(r.restaurantName) }}</span>
          <div class="rp-id">
            <strong>{{ store.profile()?.name ?? r.restaurantName }}</strong>
            <span class="d-mono">{{ store.profile()?.slug ?? r.restaurantSlug }}</span>
          </div>
          <div class="rp-meta">
            <bo-role-badge [role]="r.role" />
            @if (store.profile(); as p) {
              <span class="rp-pub" [attr.data-status]="p.publicationStatus" title="Status de publicação do perfil">
                <span class="dot"></span>{{ publicationLabel[p.publicationStatus] }}
              </span>
            }
          </div>
        </section>
        @if (store.profile()?.publicationStatus === 'DRAFT') {
          <p class="d-note rp-note"><bo-icon name="info" [size]="14" /> O perfil está em rascunho. A publicação ainda não é feita pelo painel; quando estiver disponível, o perfil completo poderá ser publicado daqui.</p>
        }

        <nav class="rp-tabs" aria-label="Seções do restaurante">
          <a routerLink="/app/restaurante" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }" #t1="routerLinkActive" [attr.aria-current]="t1.isActive ? 'page' : null"><bo-icon name="store" [size]="16" /> Perfil</a>
          <a routerLink="/app/restaurante/horarios" routerLinkActive="active" #t2="routerLinkActive" [attr.aria-current]="t2.isActive ? 'page' : null"><bo-icon name="clock" [size]="16" /> Horários</a>
          <a routerLink="/app/restaurante/imagens" routerLinkActive="active" #t3="routerLinkActive" [attr.aria-current]="t3.isActive ? 'page' : null"><bo-icon name="image" [size]="16" /> Imagens</a>
        </nav>

        <router-outlet />
      }
    } @else {
      <bo-state kind="empty" icon="store" title="Nenhum restaurante selecionado" text="Cadastre seu estabelecimento para começar.">
        <a routerLink="/app/novo-restaurante" class="d-btn d-btn-primary">Cadastrar restaurante</a>
      </bo-state>
    }
  `,
  styles: [`
    .rp-summary{display:flex;flex-wrap:wrap;align-items:center;gap:14px;padding:16px 18px}
    .rp-avatar{width:44px;height:44px;font-size:15px;border-radius:12px}
    .rp-id{flex:1;min-width:180px;display:flex;flex-direction:column}
    .rp-id strong{font-size:16px}
    .rp-id span{color:var(--d-muted);font-size:12.5px;overflow-wrap:anywhere}
    .rp-meta{display:flex;flex-wrap:wrap;align-items:center;gap:10px}
    .rp-pub{display:inline-flex;align-items:center;gap:6px;padding:2px 9px;border-radius:999px;font-size:12px;font-weight:600;border:1px solid var(--d-border);background:var(--d-neutral-soft);color:var(--d-neutral-ink)}
    .rp-pub .dot{width:6px;height:6px;border-radius:50%;background:currentColor}
    .rp-pub[data-status=PUBLISHED]{color:var(--d-success);background:var(--d-success-soft);border-color:var(--d-success-line)}
    .rp-pub[data-status=SUSPENDED]{color:var(--d-error);background:var(--d-error-soft);border-color:var(--d-error-line)}
    .rp-note{margin-top:10px}
    .rp-tabs{display:flex;gap:4px;margin:20px 0 18px;box-shadow:inset 0 -1px var(--d-border);overflow-x:auto;overflow-y:hidden;scrollbar-width:none}
    .rp-tabs a{display:inline-flex;align-items:center;gap:8px;padding:10px 14px;border-bottom:2px solid transparent;color:var(--d-muted);font-weight:600;white-space:nowrap;transition:color .15s,border-color .15s}
    .rp-tabs a:hover{color:var(--d-ink)}
    .rp-tabs a.active{color:var(--d-ink);border-bottom-color:var(--d-primary)}
    .rp-tabs a.active bo-icon{color:var(--d-primary)}
  `]
})
export class RestaurantPage {
  protected readonly ctx = inject(RestaurantContext);
  protected readonly store = inject(ProfileStore);
  protected readonly publicationLabel = PUBLICATION_LABEL;

  protected initials(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase();
  }
}
