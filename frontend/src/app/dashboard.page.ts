import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, AuthUser } from './auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
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
          <p class="description">Você está conectado ao Boraê. Em breve, este será o seu espaço para descobrir restaurantes, pratos e benefícios.</p>
          <div class="profile">
            <span class="avatar">{{ user.fullName.charAt(0).toUpperCase() }}</span>
            <span><strong>{{ user.fullName }}</strong><small>{{ user.email }}</small></span>
          </div>
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
    footer{padding:24px 0;border-top:1px solid #34362f;color:#777a70;font-size:12px}
    @media(max-width:600px){.dashboard{width:calc(100% - 32px)}h1{letter-spacing:-2px}}
  `]
})
export class DashboardPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  user: AuthUser | null = null;
  loading = true;

  ngOnInit(): void {
    this.auth.getCurrentUser().subscribe({
      next: user => {
        this.user = user;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/');
  }
}
