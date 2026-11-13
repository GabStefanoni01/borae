import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService, RestaurantInvitation, RestaurantMember } from '../../auth.service';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext, describeHttpError, ROLE_LABEL } from '../restaurant-context.service';
import {
  ConfirmDialogComponent, PageHeaderComponent, RoleBadgeComponent, StateComponent
} from '../ui/ui.components';

type AssignableRole = 'MANAGER' | 'STAFF';

/**
 * Equipe do restaurante selecionado.
 * - GET    /restaurants/{id}/members                 → OWNER e MANAGER
 * - POST   /restaurants/{id}/invitations             → OWNER (MANAGER/STAFF)
 * - PATCH  /restaurants/{id}/members/{membershipId}  → OWNER
 * - DELETE /restaurants/{id}/members/{membershipId}  → OWNER (desativa o vínculo)
 * A visibilidade das ações espelha a API atual; o backend valida tudo.
 * O token do convite fica apenas em memória e some ao trocar de restaurante ou sair da página.
 */
@Component({
  selector: 'bo-team-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, DatePipe, IconComponent, PageHeaderComponent, StateComponent, RoleBadgeComponent, ConfirmDialogComponent],
  template: `
    <bo-page-header title="Equipe" subtitle="Organize as pessoas que participam da gestão do seu restaurante." status="available">
      <a routerLink="/app/convite" class="d-btn d-btn-secondary"><bo-icon name="ticket" [size]="16" /> Aceitar convite</a>
    </bo-page-header>

    @if (!ctx.selected()) {
      <bo-state kind="empty" icon="store" title="Nenhum restaurante selecionado"
        text="Cadastre um restaurante ou aceite um convite para gerenciar uma equipe.">
        <a routerLink="/app/novo-restaurante" class="d-btn d-btn-primary">Cadastrar restaurante</a>
      </bo-state>
    } @else if (!ctx.canViewTeam()) {
      <bo-state kind="forbidden" title="Sem acesso à equipe"
        text="Na versão atual, a lista da equipe pode ser consultada pelo proprietário e por gerentes. Seu papel neste restaurante é Equipe." />
    } @else {
      <p class="tm-lead">Defina quem faz parte da equipe e acompanhe os vínculos de acesso ao estabelecimento.</p>

      @if (feedback(); as f) {
        <div class="d-alert" [class.d-alert-success]="f.type === 'success'" [class.d-alert-error]="f.type === 'error'" [attr.role]="f.type === 'error' ? 'alert' : 'status'">
          <bo-icon [name]="f.type === 'success' ? 'check-circle' : 'alert'" [size]="18" /><p>{{ f.text }}</p>
        </div>
      }

      <section class="d-section tm-first">
        <div class="tm-head">
          <h2 class="d-section-title">Membros ativos @if (members().length) { <span>· {{ members().length }}</span> }</h2>
          <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="loadMembers()" [disabled]="loading()"><bo-icon name="refresh" [size]="14" /> Atualizar</button>
        </div>

        @if (loading()) {
          <bo-state kind="loading" [compact]="true" title="Carregando equipe" />
        } @else if (loadError()) {
          <bo-state kind="error" [compact]="true" title="Não foi possível carregar a equipe" [text]="loadError()">
            <button type="button" class="d-btn d-btn-secondary" (click)="loadMembers()">Tentar novamente</button>
          </bo-state>
        } @else if (members().length === 0) {
          <bo-state kind="empty" [compact]="true" title="Ainda não há membros por aqui." text="Os membros ativos do restaurante aparecerão nesta lista." />
        } @else {
          <div class="d-table-wrap">
            <table class="d-table">
              <caption class="sr-only">Membros ativos de {{ ctx.selected()?.restaurantName }}</caption>
              <thead><tr><th scope="col">Pessoa</th><th scope="col">Papel</th>@if (ctx.canManageTeam()) {<th scope="col"><span class="sr-only">Ações</span></th>}</tr></thead>
              <tbody>
                @for (m of members(); track m.membershipId + ':' + revision()) {
                  <tr>
                    <td><div class="d-person"><span class="d-avatar">{{ m.fullName.charAt(0).toUpperCase() }}</span><span><strong>{{ m.fullName }}@if (m.userId === ctx.user()?.id) { <em class="tm-you">você</em> }</strong><small>{{ m.email }}</small></span></div></td>
                    <td data-label="Papel">
                      @if (ctx.canManageTeam() && m.role !== 'OWNER') {
                        <label class="sr-only" [for]="'role-' + m.membershipId">Papel de {{ m.fullName }}</label>
                        <select class="d-input d-input-sm" [id]="'role-' + m.membershipId" [ngModel]="m.role"
                          (ngModelChange)="changeRole(m, $event)" [disabled]="busyId() === m.membershipId">
                          <option value="MANAGER">Gerente</option>
                          <option value="STAFF">Equipe</option>
                        </select>
                      } @else {
                        <bo-role-badge [role]="m.role" />
                      }
                    </td>
                    @if (ctx.canManageTeam()) {
                      <td>
                        <div class="d-row-actions">
                          @if (m.role !== 'OWNER') {
                            <button type="button" class="d-btn d-btn-ghost d-btn-sm tm-remove" (click)="pendingRemoval.set(m)" [disabled]="busyId() === m.membershipId">
                              <bo-icon name="trash" [size]="14" /> Remover
                            </button>
                          } @else {
                            <span class="tm-muted">Proprietário</span>
                          }
                        </div>
                      </td>
                    }
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
        @if (!ctx.canManageTeam()) {
          <p class="d-note"><bo-icon name="info" [size]="14" /> Somente o proprietário pode convidar pessoas, alterar papéis ou remover membros.</p>
        }
      </section>

      @if (ctx.canManageTeam()) {
        <section class="d-section" id="convidar">
          <h2 class="d-section-title">Convidar pessoa</h2>
          <div class="d-card">
            <form class="d-form" (ngSubmit)="invite()" #inviteForm="ngForm">
              <div class="d-form-row">
                <div class="d-field">
                  <label for="invite-email">E-mail da pessoa convidada</label>
                  <input id="invite-email" name="email" type="email" class="d-input" [(ngModel)]="inviteEmail" required email maxlength="255" placeholder="pessoa@email.com" autocomplete="off">
                </div>
                <div class="d-field">
                  <label for="invite-role">Papel</label>
                  <select id="invite-role" name="role" class="d-input" [(ngModel)]="inviteRole">
                    <option value="MANAGER">Gerente</option>
                    <option value="STAFF">Equipe</option>
                  </select>
                </div>
              </div>
              <div class="d-form-actions">
                <button type="submit" class="d-btn d-btn-primary" [disabled]="inviting() || !inviteForm.valid">
                  <bo-icon [name]="inviting() ? 'loader' : 'user-plus'" [size]="16" [spin]="inviting()" /> {{ inviting() ? 'Gerando convite...' : 'Gerar convite' }}
                </button>
                <span class="tm-muted">O envio por e-mail ainda não existe: o convite é compartilhado manualmente.</span>
              </div>
            </form>

            @if (invitation(); as inv) {
              <div class="tm-invite" role="status">
                <div class="d-alert d-alert-warning">
                  <bo-icon name="shield-check" [size]="18" />
                  <p><strong>Compartilhe este convite somente com o destinatário.</strong> Este convite possui validade de sete dias. Ele será exibido apenas agora.</p>
                </div>
                <dl class="d-kv">
                  <div><dt>Convidado</dt><dd>{{ inv.email }}</dd></div>
                  <div><dt>Papel</dt><dd><bo-role-badge [role]="inv.role" /></dd></div>
                  <div><dt>Expira em</dt><dd>{{ inv.expiresAt | date: 'dd/MM/yyyy HH:mm' }}</dd></div>
                </dl>
                <div class="d-field">
                  <span class="tm-label">Token do convite</span>
                  <div class="d-token">
                    <code>{{ inv.token }}</code>
                    <button type="button" class="d-btn d-btn-secondary" (click)="copyToken(inv.token)"><bo-icon [name]="copied() ? 'check' : 'copy'" [size]="16" /> {{ copied() ? 'Copiado' : 'Copiar' }}</button>
                  </div>
                  <span class="d-hint">A pessoa deve entrar com o e-mail convidado e usar a opção "Aceitar convite".</span>
                </div>
                <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="invitation.set(null)">Ocultar convite</button>
              </div>
            }
          </div>
        </section>
      }
    }

    <bo-confirm-dialog [open]="!!pendingRemoval()" [danger]="true" [busy]="!!busyId()"
      title="Remover membro da equipe?" confirmLabel="Remover"
      [message]="removalMessage()" (confirmed)="confirmRemoval()" (cancelled)="pendingRemoval.set(null)" />
  `,
  styles: [`
    .tm-lead{margin:-12px 0 20px;color:var(--d-muted)}
    .tm-first{margin-top:16px}
    .tm-head{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:10px}
    .tm-head .d-section-title{margin:0}
    .tm-head span{color:var(--d-ink)}
    .tm-you{margin-left:6px;font-style:normal;font-size:11px;font-weight:600;color:var(--d-primary-text);background:var(--d-primary-soft);padding:1px 6px;border-radius:999px}
    .tm-muted{font-size:12.5px;color:var(--d-muted)}
    .tm-remove{color:var(--d-error)}
    .tm-invite{display:grid;gap:16px;margin-top:20px;padding-top:20px;border-top:1px solid var(--d-border)}
    .tm-label{font-weight:600;font-size:13px}
  `]
})
export class TeamPage {
  protected readonly ctx = inject(RestaurantContext);
  private readonly auth = inject(AuthService);

  protected readonly members = signal<RestaurantMember[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal('');
  protected readonly busyId = signal<string | null>(null);
  protected readonly feedback = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  protected readonly pendingRemoval = signal<RestaurantMember | null>(null);
  protected readonly invitation = signal<RestaurantInvitation | null>(null);
  protected readonly inviting = signal(false);
  protected readonly copied = signal(false);
  protected readonly revision = signal(0);
  protected inviteEmail = '';
  protected inviteRole: AssignableRole = 'STAFF';

  protected readonly removalMessage = computed(() => {
    const m = this.pendingRemoval();
    return m ? `${m.fullName} perderá o acesso a ${this.ctx.selected()?.restaurantName ?? 'este restaurante'}. O vínculo é desativado e o histórico é preservado.` : '';
  });

  constructor() {
    effect(() => {
      // Ao trocar de restaurante: limpa convite/feedback e recarrega a equipe.
      const restaurant = this.ctx.selected();
      const allowed = this.ctx.canViewTeam();
      this.invitation.set(null);
      this.feedback.set(null);
      this.members.set([]);
      if (restaurant && allowed) this.loadMembers();
    });
  }

  protected loadMembers(): void {
    const id = this.ctx.selectedId();
    if (!id) return;
    this.loading.set(true);
    this.loadError.set('');
    this.auth.getRestaurantMembers(id).subscribe({
      next: members => { this.members.set(members); this.loading.set(false); },
      error: error => { this.loading.set(false); this.loadError.set(describeHttpError(error, 'carregar a equipe')); }
    });
  }

  protected changeRole(member: RestaurantMember, role: AssignableRole): void {
    const id = this.ctx.selectedId();
    if (!id || role === member.role) return;
    this.busyId.set(member.membershipId);
    this.feedback.set(null);
    this.auth.updateRestaurantMemberRole(id, member.membershipId, role).subscribe({
      next: updated => {
        this.members.update(list => list.map(m => m.membershipId === updated.membershipId ? updated : m));
        this.busyId.set(null);
        this.feedback.set({ type: 'success', text: `${updated.fullName} agora é ${ROLE_LABEL[updated.role]}.` });
      },
      error: error => {
        // Recria as linhas para o select voltar ao papel confirmado pelo servidor.
        this.revision.update(v => v + 1);
        this.busyId.set(null);
        this.feedback.set({ type: 'error', text: describeHttpError(error, 'alterar o papel') });
      }
    });
  }

  protected confirmRemoval(): void {
    const id = this.ctx.selectedId();
    const member = this.pendingRemoval();
    if (!id || !member) return;
    this.busyId.set(member.membershipId);
    this.auth.removeRestaurantMember(id, member.membershipId).subscribe({
      next: () => {
        this.members.update(list => list.filter(m => m.membershipId !== member.membershipId));
        this.busyId.set(null);
        this.pendingRemoval.set(null);
        this.feedback.set({ type: 'success', text: `${member.fullName} foi removido da equipe.` });
      },
      error: error => {
        this.busyId.set(null);
        this.pendingRemoval.set(null);
        this.feedback.set({ type: 'error', text: describeHttpError(error, 'remover o membro') });
      }
    });
  }

  protected invite(): void {
    const id = this.ctx.selectedId();
    const email = this.inviteEmail.trim();
    if (!id || !email || this.inviting()) return;
    this.inviting.set(true);
    this.invitation.set(null);
    this.feedback.set(null);
    this.copied.set(false);
    this.auth.inviteRestaurantMember(id, email, this.inviteRole).subscribe({
      next: invitation => {
        this.invitation.set(invitation);
        this.inviteEmail = '';
        this.inviting.set(false);
      },
      error: error => {
        this.inviting.set(false);
        const conflict = error instanceof HttpErrorResponse && error.status === 409;
        this.feedback.set({
          type: 'error',
          text: conflict ? 'Já existe um vínculo ativo ou um convite pendente para este e-mail.' : describeHttpError(error, 'gerar o convite')
        });
      }
    });
  }

  protected copyToken(token: string): void {
    navigator.clipboard?.writeText(token).then(
      () => { this.copied.set(true); setTimeout(() => this.copied.set(false), 2000); },
      () => this.copied.set(false)
    );
  }
}
