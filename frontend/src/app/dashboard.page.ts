import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, AuthUser, RestaurantAccess } from './auth.service';
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
          <p class="description">Você está conectado ao Boraê. Este é o início do seu espaço para descobrir restaurantes, pratos e benefícios.</p>
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
    .welcome{flex:1;display:flex;flex-direction:column;justify-content:center;max-width:720px;padding:70px 0}
    .eyebrow{color:#b6d75c;font-size:11px;font-weight:800;letter-spacing:2px}
    h1{font-size:clamp(48px,8vw,82px);line-height:1;letter-spacing:-4px;margin:12px 0 22px}
    .description{max-width:590px;color:#a7a99e;font-size:17px;line-height:1.8}
    .profile{display:flex;align-items:center;gap:14px;max-width:420px;margin-top:26px;padding:18px;background:#20221c;border:1px solid #35382e;border-radius:14px}
    .avatar{width:44px;height:44px;border-radius:50%;display:grid;place-items:center;background:#d6f36a;color:#1b1d16;font-weight:800}
    .profile strong,.profile small{display:block}.profile small{color:#929588;margin-top:5px}
    .status{color:#a6c84e;font-size:12px;margin-top:20px}
    .access{margin-top:30px;max-width:560px}.access h2{font-size:18px;letter-spacing:0;margin:0 0 12px}
    .empty{color:#929588;font-size:14px;line-height:1.6;background:#20221c;border:1px solid #35382e;border-radius:12px;padding:16px}
    .restaurant{display:flex;justify-content:space-between;align-items:center;gap:12px;padding:14px 16px;margin-top:8px;background:#20221c;border:1px solid #35382e;border-radius:12px}
    .restaurant strong,.restaurant small{display:block}.restaurant small{color:#929588;margin-top:4px}
    .role{font-size:11px;font-weight:700;color:#d6f36a;background:#343a25;border-radius:20px;padding:6px 9px}
    .create-form{display:grid;gap:9px;margin-top:20px;padding:20px;background:#20221c;border:1px solid #35382e;border-radius:14px}
    .create-form h3{margin:0 0 6px;font-size:17px}.create-form label{font-size:12px;font-weight:700;color:#d8dccd}
    .create-form label span{font-weight:400;color:#858a7b}
    .create-form input,.create-form textarea{width:100%;box-sizing:border-box;border:1px solid #454a3c;border-radius:8px;background:#191b16;color:#f5f0e8;padding:12px;font:inherit;resize:vertical}
    .create-form input:focus,.create-form textarea:focus{outline:1px solid #b6d75c;border-color:#b6d75c}
    .create-button{margin-top:6px;border:0;border-radius:9px;background:#d6f36a;color:#1b1d16;padding:13px 16px;font-weight:800;cursor:pointer}
    .create-button:disabled{opacity:.55;cursor:not-allowed}
    .error{margin:0;color:#ff9b8f;font-size:13px}
    footer{padding:24px 0;border-top:1px solid #34362f;color:#777a70;font-size:12px}
    @media(max-width:600px){.dashboard{width:calc(100% - 32px)}h1{letter-spacing:-2px}}
  `]
})
export class DashboardPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  user: AuthUser | null = null;
  restaurants: RestaurantAccess[] = [];
  loading = true;
  creating = false;
  restaurantName = '';
  restaurantDescription = '';
  creationError = '';

  ngOnInit(): void {
    forkJoin({
      user: this.auth.getCurrentUser(),
      restaurants: this.auth.getMyRestaurants()
    }).subscribe({
      next: result => {
        this.user = result.user;
        this.restaurants = result.restaurants;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  createRestaurant(): void {
    const name = this.restaurantName.trim();
    if (!name || this.creating) return;

    this.creating = true;
    this.creationError = '';
    this.auth.createRestaurant({
      name,
      description: this.restaurantDescription.trim() || undefined
    }).subscribe({
      next: restaurant => {
        this.restaurants = [...this.restaurants, restaurant];
        this.restaurantName = '';
        this.restaurantDescription = '';
        this.creating = false;
      },
      error: error => {
        this.creationError = error?.status === 400
          ? 'Confira o nome e a descrição do restaurante.'
          : error?.status === 401
            ? 'Sua sessão expirou. Entre novamente.'
            : 'Não foi possível cadastrar o restaurante. Tente novamente.';
        this.creating = false;
      }
    });
  }

  roleLabel(role: RestaurantAccess['role']): string {
    return {
      OWNER: 'Proprietário',
      MANAGER: 'Gerente',
      STAFF: 'Equipe'
    }[role];
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/');
  }
}
