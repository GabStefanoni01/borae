import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, AuthUser, RestaurantAccess, RestaurantMember } from './auth.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule],
  template: `
    <main class="dashboard">
      <header class="topbar">
        <a class="brand" href="/" aria-label="Boraê início">bor<span>aê</span></a>
        <button class="logout" type="button" (click)="logout()">Sair</button>
      </header>

      <section class="welcome">
        <p class="eyebrow">SUA EXPERIÊNCIA BORAÊ</p>
        @if (loading) {
          <h1>Carregando sua conta...</h1>
          <p class="description">Estamos confirmando sua sessão com segurança.</p>
        } @else if (user) {
          <h1>Olá, {{ user.fullName.split(' ')[0] }}.</h1>
          <p class="description">Gerencie seus restaurantes e organize sua equipe em um só lugar.</p>
          <div class="profile">
            <span class="avatar">{{ user.fullName.charAt(0).toUpperCase() }}</span>
            <span><strong>{{ user.fullName }}</strong><small>{{ user.email }}</small></span>
          </div>

          <section class="access">
            <h2>Seus restaurantes</h2>
            @if (restaurants.length === 0) {
              <p class="empty">Você ainda não possui vínculo com nenhum restaurante. Cadastre o primeiro estabelecimento para começar.</p>
            } @else {
              @for (access of restaurants; track access.membershipId) {
                <article class="restaurant">
                  <div><strong>{{ access.restaurantName }}</strong><small>{{ access.restaurantSlug }}</small></div>
                  <span class="role">{{ roleLabel(access.role) }}</span>
                </article>
              }
            }
            <form class="create-form" (ngSubmit)="createRestaurant()">
              <h3>Cadastrar restaurante</h3>
              <label for="restaurant-name">Nome do estabelecimento</label>
              <input id="restaurant-name" name="restaurantName" [(ngModel)]="restaurantName" required maxlength="180" placeholder="Ex.: Café da Praça">
              <label for="restaurant-description">Descrição <span>(opcional)</span></label>
              <textarea id="restaurant-description" name="restaurantDescription" [(ngModel)]="restaurantDescription" maxlength="1000" rows="3" placeholder="Conte um pouco sobre o estabelecimento"></textarea>
              @if (creationError) { <p class="error" role="alert">{{ creationError }}</p> }
              <button class="create-button" type="submit" [disabled]="creating || !restaurantName.trim()">
                {{ creating ? 'Cadastrando...' : 'Cadastrar restaurante' }}
              </button>
            </form>
          </section>

          @if (restaurants.length > 0) {
            <section class="team">
              <div class="section-heading"><div><p class="eyebrow">GESTÃO DE ACESSO</p><h2>Equipe do restaurante</h2></div></div>
              <label for="team-restaurant">Restaurante</label>
              <select id="team-restaurant" name="teamRestaurant" [(ngModel)]="selectedRestaurantId" (ngModelChange)="loadMembers()">
                @for (access of restaurants; track access.restaurantId) {
                  <option [value]="access.restaurantId">{{ access.restaurantName }}</option>
                }
              </select>

              @if (teamLoading) {
                <p class="empty">Carregando equipe...</p>
              } @else {
                @for (member of members; track member.membershipId) {
                  <article class="member">
                    <div><strong>{{ member.fullName }}</strong><small>{{ member.email }}</small></div>
                    <span class="role">{{ roleLabel(member.role) }}</span>
                    @if (selectedRole === 'OWNER') {
                      @if (member.role !== 'OWNER') {
                        <div class="member-actions">
                          <button type="button" (click)="changeRole(member, member.role === 'MANAGER' ? 'STAFF' : 'MANAGER')">
                            {{ member.role === 'MANAGER' ? 'Tornar equipe' : 'Tornar gerente' }}
                          </button>
                          <button class="remove" type="button" (click)="removeMember(member)">Remover</button>
                        </div>
                      }
                    }
                  </article>
                }
                @if (selectedRole === 'OWNER') {
                  <form class="create-form" (ngSubmit)="inviteMember()">
                    <h3>Convidar pessoa</h3>
                    <p class="hint">O convite vale por 7 dias. O link/token deve ser compartilhado com a pessoa convidada.</p>
                    <label for="invite-email">E-mail da conta Boraê</label>
                    <input id="invite-email" name="inviteEmail" type="email" [(ngModel)]="inviteEmail" required maxlength="255" placeholder="pessoa@email.com">
                    <label for="invite-role">Permissão</label>
                    <select id="invite-role" name="inviteRole" [(ngModel)]="inviteRole">
                      <option value="STAFF">Equipe</option>
                      <option value="MANAGER">Gerente</option>
                    </select>
                    @if (teamError) { <p class="error" role="alert">{{ teamError }}</p> }
                    @if (inviteToken) {
                      <div class="token-box"><strong>Convite criado</strong><textarea readonly rows="3" [value]="inviteToken"></textarea><small>Copie e envie este token à pessoa convidada. Ele só poderá ser aceito por uma conta com o e-mail informado.</small></div>
                    }
                    <button class="create-button" type="submit" [disabled]="inviting || !inviteEmail.trim()">
                      {{ inviting ? 'Gerando convite...' : 'Gerar convite' }}
                    </button>
                  </form>
                }
              }
              @if (teamError && selectedRole !== 'OWNER') { <p class="error" role="alert">{{ teamError }}</p> }
            </section>
          }

          <section class="accept">
            <h2>Aceitar convite</h2>
            <p class="hint">Se alguém compartilhou um convite com você, entre com o e-mail correspondente e informe o token.</p>
            <form (ngSubmit)="acceptInvitation()">
              <textarea name="acceptToken" [(ngModel)]="acceptToken" rows="3" placeholder="Cole aqui o token do convite"></textarea>
              @if (acceptError) { <p class="error" role="alert">{{ acceptError }}</p> }
              @if (acceptSuccess) { <p class="success" role="status">{{ acceptSuccess }}</p> }
              <button class="create-button" type="submit" [disabled]="accepting || !acceptToken.trim()">
                {{ accepting ? 'Validando...' : 'Aceitar convite' }}
              </button>
            </form>
          </section>
          <p class="status">Sessão autenticada e perfil carregado pela API.</p>
        } @else {
          <h1>Não foi possível carregar seu perfil.</h1>
          <p class="description">Tente entrar novamente.</p>
          <button class="logout" type="button" (click)="logout()">Voltar ao login</button>
        }
      </section>
      <footer>© 2026 Boraê · Uma nova forma de viver a gastronomia.</footer>
    </main>
  `,
  styles: [`
    :host{display:block;min-height:100vh;background:#191b16;color:#f5f0e8;font-family:Arial,sans-serif}
    .dashboard{width:min(1120px,calc(100% - 48px));min-height:100vh;margin:0 auto;display:flex;flex-direction:column}
    .topbar{display:flex;align-items:center;justify-content:space-between;padding:28px 0;border-bottom:1px solid #34362f}
    .brand{color:#f5f0e8;font-size:30px;font-weight:850;letter-spacing:-2px;text-decoration:none}.brand span{color:#d6f36a}
    .logout{border:1px solid #555b46;border-radius:9px;background:transparent;color:#e9f5c9;padding:11px 18px;font-weight:700;cursor:pointer}
    .welcome{flex:1;display:flex;flex-direction:column;justify-content:center;max-width:760px;padding:70px 0}
    .eyebrow{color:#b6d75c;font-size:11px;font-weight:800;letter-spacing:2px}
    h1{font-size:clamp(48px,8vw,82px);line-height:1;letter-spacing:-4px;margin:12px 0 22px}
    .description{max-width:590px;color:#a7a99e;font-size:17px;line-height:1.8}
    .profile{display:flex;align-items:center;gap:14px;max-width:420px;margin-top:26px;padding:18px;background:#20221c;border:1px solid #35382e;border-radius:14px}
    .avatar{width:44px;height:44px;border-radius:50%;display:grid;place-items:center;background:#d6f36a;color:#1b1d16;font-weight:800}
    .profile strong,.profile small{display:block}.profile small{color:#929588;margin-top:5px}
    .status{color:#a6c84e;font-size:12px;margin-top:20px}
    .access,.team,.accept{margin-top:30px;max-width:620px}.access h2,.team h2,.accept h2{font-size:20px;letter-spacing:0;margin:0 0 12px}
    .empty{color:#929588;font-size:14px;line-height:1.6;background:#20221c;border:1px solid #35382e;border-radius:12px;padding:16px}
    .restaurant,.member{display:flex;justify-content:space-between;align-items:center;gap:12px;padding:14px 16px;margin-top:8px;background:#20221c;border:1px solid #35382e;border-radius:12px;flex-wrap:wrap}
    .restaurant strong,.restaurant small,.member strong,.member small{display:block}.restaurant small,.member small{color:#929588;margin-top:4px}
    .role{font-size:11px;font-weight:700;color:#d6f36a;background:#343a25;border-radius:20px;padding:6px 9px;white-space:nowrap}
    .create-form{display:grid;gap:9px;margin-top:20px;padding:20px;background:#20221c;border:1px solid #35382e;border-radius:14px}
    .create-form h3{margin:0 0 6px;font-size:17px}.create-form label,.team>label{font-size:12px;font-weight:700;color:#d8dccd}
    .create-form label span{font-weight:400;color:#858a7b}
    input,textarea,select{width:100%;box-sizing:border-box;border:1px solid #454a3c;border-radius:8px;background:#191b16;color:#f5f0e8;padding:12px;font:inherit;resize:vertical}
    input:focus,textarea:focus,select:focus{outline:1px solid #b6d75c;border-color:#b6d75c}
    .create-button{margin-top:6px;border:0;border-radius:9px;background:#d6f36a;color:#1b1d16;padding:13px 16px;font-weight:800;cursor:pointer}
    .create-button:disabled{opacity:.55;cursor:not-allowed}
    .error{margin:0;color:#ff9b8f;font-size:13px}.success{color:#b6d75c;font-size:13px}
    .hint{margin:0 0 8px;color:#929588;font-size:13px;line-height:1.6}
    .member-actions{display:flex;gap:7px;margin-left:auto}.member-actions button{border:1px solid #555b46;border-radius:7px;background:transparent;color:#e9f5c9;padding:7px 9px;font-size:11px;cursor:pointer}.member-actions .remove{color:#ff9b8f;border-color:#70453f}
    .team select{margin:8px 0 12px}.token-box{display:grid;gap:8px;padding:12px;background:#191b16;border:1px solid #454a3c;border-radius:8px}.token-box small{color:#929588;line-height:1.5}
    .accept form{display:grid;gap:9px}.section-heading{margin-bottom:12px}
    footer{padding:24px 0;border-top:1px solid #34362f;color:#777a70;font-size:12px}
    @media(max-width:600px){.dashboard{width:calc(100% - 32px)}h1{letter-spacing:-2px}.member-actions{width:100%;margin:0}}
  `]
})
export class DashboardPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  user: AuthUser | null = null;
  restaurants: RestaurantAccess[] = [];
  members: RestaurantMember[] = [];
  loading = true;
  creating = false;
  restaurantName = '';
  restaurantDescription = '';
  creationError = '';
  selectedRestaurantId = '';
  selectedRole: RestaurantAccess['role'] | null = null;
  teamLoading = false;
  inviting = false;
  inviteEmail = '';
  inviteRole: 'MANAGER' | 'STAFF' = 'STAFF';
  inviteToken = '';
  teamError = '';
  accepting = false;
  acceptToken = '';
  acceptError = '';
  acceptSuccess = '';

  ngOnInit(): void {
    forkJoin({
      user: this.auth.getCurrentUser(),
      restaurants: this.auth.getMyRestaurants()
    }).subscribe({
      next: result => {
        this.user = result.user;
        this.restaurants = result.restaurants;
        this.loading = false;
        if (this.restaurants.length > 0) {
          this.selectedRestaurantId = this.restaurants[0].restaurantId;
          this.selectedRole = this.restaurants[0].role;
          this.loadMembers();
        }
      },
      error: () => { this.loading = false; }
    });
  }

  createRestaurant(): void {
    const name = this.restaurantName.trim();
    if (!name || this.creating) return;
    this.creating = true;
    this.creationError = '';
    this.auth.createRestaurant({ name, description: this.restaurantDescription.trim() || undefined }).subscribe({
      next: restaurant => {
        this.restaurants = [...this.restaurants, restaurant];
        this.restaurantName = '';
        this.restaurantDescription = '';
        this.creating = false;
        this.selectedRestaurantId = restaurant.restaurantId;
        this.selectedRole = restaurant.role;
        this.loadMembers();
      },
      error: error => {
        this.creationError = error?.status === 400 ? 'Confira o nome e a descrição do restaurante.' :
          error?.status === 401 ? 'Sua sessão expirou. Entre novamente.' : 'Não foi possível cadastrar o restaurante. Tente novamente.';
        this.creating = false;
      }
    });
  }

  loadMembers(): void {
    const access = this.restaurants.find(item => item.restaurantId === this.selectedRestaurantId);
    this.selectedRole = access?.role ?? null;
    if (!this.selectedRestaurantId) { this.members = []; return; }
    this.teamLoading = true;
    this.teamError = '';
    this.auth.getRestaurantMembers(this.selectedRestaurantId).subscribe({
      next: members => { this.members = members; this.teamLoading = false; },
      error: error => {
        this.members = [];
        this.teamLoading = false;
        this.teamError = error?.status === 403 ? 'Seu papel não permite consultar esta equipe.' : 'Não foi possível carregar a equipe.';
      }
    });
  }

  inviteMember(): void {
    if (!this.selectedRestaurantId || !this.inviteEmail.trim() || this.inviting) return;
    this.inviting = true;
    this.teamError = '';
    this.inviteToken = '';
    this.auth.inviteRestaurantMember(this.selectedRestaurantId, this.inviteEmail.trim(), this.inviteRole).subscribe({
      next: invitation => {
        this.inviteToken = invitation.token;
        this.inviteEmail = '';
        this.inviting = false;
      },
      error: error => {
        this.teamError = error?.status === 409 ? 'Já existe um vínculo ou convite pendente para este e-mail.' :
          error?.status === 403 ? 'Somente o proprietário pode convidar pessoas.' : 'Não foi possível gerar o convite.';
        this.inviting = false;
      }
    });
  }

  changeRole(member: RestaurantMember, role: 'MANAGER' | 'STAFF'): void {
    this.auth.updateRestaurantMemberRole(this.selectedRestaurantId, member.membershipId, role).subscribe({
      next: updated => { this.members = this.members.map(item => item.membershipId === updated.membershipId ? updated : item); },
      error: () => { this.teamError = 'Não foi possível alterar a permissão deste membro.'; }
    });
  }

  removeMember(member: RestaurantMember): void {
    if (!this.selectedRestaurantId || !confirm('Remover ' + member.fullName + ' da equipe deste restaurante?')) return;
    this.auth.removeRestaurantMember(this.selectedRestaurantId, member.membershipId).subscribe({
      next: () => { this.members = this.members.filter(item => item.membershipId !== member.membershipId); },
      error: () => { this.teamError = 'Não foi possível remover este membro.'; }
    });
  }

  acceptInvitation(): void {
    if (!this.acceptToken.trim() || this.accepting) return;
    this.accepting = true;
    this.acceptError = '';
    this.acceptSuccess = '';
    this.auth.acceptRestaurantInvitation(this.acceptToken.trim()).subscribe({
      next: member => {
        this.accepting = false;
        this.acceptToken = '';
        this.acceptSuccess = 'Convite aceito! Você agora faz parte da equipe como ' + this.roleLabel(member.role) + '.';
        this.auth.getMyRestaurants().subscribe(restaurants => {
          this.restaurants = restaurants;
          this.selectedRestaurantId = member.membershipId ? restaurants.find(item => item.membershipId === member.membershipId)?.restaurantId ?? this.selectedRestaurantId : this.selectedRestaurantId;
          this.loadMembers();
        });
      },
      error: error => {
        this.acceptError = error?.status === 403 ? 'Este convite foi enviado para outro e-mail.' :
          error?.status === 410 ? 'Este convite expirou ou já foi utilizado.' :
          error?.status === 404 ? 'Token de convite inválido.' : 'Não foi possível aceitar o convite.';
        this.accepting = false;
      }
    });
  }

  roleLabel(role: RestaurantAccess['role'] | RestaurantMember['role']): string {
    return { OWNER: 'Proprietário', MANAGER: 'Gerente', STAFF: 'Equipe' }[role];
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/');
  }
}
