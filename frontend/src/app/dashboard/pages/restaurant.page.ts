import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BoraeIconName, IconComponent } from '../../shared/icon.component';
import { RestaurantContext } from '../restaurant-context.service';
import { PageHeaderComponent, RoleBadgeComponent, StateComponent, StatusBadgeComponent } from '../ui/ui.components';

interface PlannedSection { title: string; icon: BoraeIconName; fields: string[]; }

/**
 * Meu restaurante. Hoje a API devolve apenas nome, slug e papel (GET /users/me/restaurants).
 * As seções do perfil completo (docs/SCREEN_FLOWS.md §5, D-01) aparecem como estrutura
 * planejada, sem campos editáveis e sem botão de salvar.
 */
@Component({
  selector: 'bo-restaurant-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent, PageHeaderComponent, StateComponent, StatusBadgeComponent, RoleBadgeComponent],
  template: `
    <bo-page-header title="Meu restaurante" subtitle="Gerencie as informações que representam seu estabelecimento dentro do Boraê." status="partial" />

    @if (ctx.selected(); as r) {
      <section class="d-card">
        <div class="d-card-head">
          <span class="d-card-icon"><bo-icon name="store" [size]="18" /></span>
          <h2>Informações básicas</h2>
          <bo-status-badge status="available" />
        </div>
        <dl class="d-kv">
          <div><dt>Nome do restaurante</dt><dd>{{ r.restaurantName }}</dd></div>
          <div><dt>Identificador público (slug)</dt><dd class="d-mono">{{ r.restaurantSlug }}</dd></div>
          <div><dt>Situação</dt><dd>Ativo</dd></div>
          <div><dt>Seu papel</dt><dd><bo-role-badge [role]="r.role" /></dd></div>
        </dl>
        <p class="d-note"><bo-icon name="info" [size]="14" /> O slug é gerado pelo servidor. A descrição informada no cadastro é salva, mas ainda não é retornada pela API para exibição. A edição destes dados ainda não está disponível.</p>
      </section>

      <section class="d-section">
        <div class="rp-head">
          <h2 class="d-section-title">Perfil completo do estabelecimento</h2>
          <bo-status-badge status="planned" />
        </div>
        <p class="rp-lead">Estas seções estão definidas na especificação do produto e serão liberadas quando a API oferecer suporte. Por enquanto, nenhum destes campos pode ser preenchido.</p>
        <div class="d-grid d-grid-3">
          @for (s of planned; track s.title) {
            <article class="d-card rp-planned">
              <div class="d-card-head"><span class="rp-icon"><bo-icon [name]="s.icon" [size]="16" /></span><h3>{{ s.title }}</h3></div>
              <ul>@for (f of s.fields; track f) { <li>{{ f }}</li> }</ul>
            </article>
          }
        </div>
        <p class="d-note"><bo-icon name="clock" [size]="14" /> Publicação prevista em três estados: rascunho, publicado e suspenso, com prévia pública e indicação de campos obrigatórios pendentes.</p>
      </section>
    } @else {
      <bo-state kind="empty" icon="store" title="Nenhum restaurante selecionado" text="Cadastre seu estabelecimento para começar.">
        <a routerLink="/app/novo-restaurante" class="d-btn d-btn-primary">Cadastrar restaurante</a>
      </bo-state>
    }
  `,
  styles: [`
    .rp-head{display:flex;align-items:center;gap:10px;margin-bottom:6px}
    .rp-head .d-section-title{margin:0}
    .rp-lead{margin:0 0 14px;color:var(--d-muted);max-width:720px}
    .rp-planned{background:var(--d-head);box-shadow:none}
    .rp-planned ul{margin:0;padding-left:18px;color:var(--d-muted);display:grid;gap:4px}
    .rp-icon{display:grid;place-items:center;width:30px;height:30px;border-radius:8px;background:var(--d-neutral-soft);color:var(--d-muted)}
  `]
})
export class RestaurantPage {
  protected readonly ctx = inject(RestaurantContext);
  protected readonly planned: PlannedSection[] = [
    { title: 'Informações básicas', icon: 'store', fields: ['Nome público / fantasia', 'Descrição', 'Categoria gastronômica', 'Slug editável (se único e válido)'] },
    { title: 'Localização', icon: 'map-pin', fields: ['Endereço', 'Cidade e estado', 'Localização no mapa'] },
    { title: 'Contato', icon: 'smartphone', fields: ['Telefone', 'WhatsApp', 'Instagram', 'Site'] },
    { title: 'Identidade visual', icon: 'image', fields: ['Logo', 'Imagem de capa'] },
    { title: 'Funcionamento', icon: 'clock', fields: ['Horários por dia da semana'] },
    { title: 'Publicação', icon: 'globe', fields: ['Rascunho, publicado ou suspenso', 'Prévia pública', 'Campos obrigatórios pendentes'] }
  ];
}
