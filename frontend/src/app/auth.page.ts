import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { environment } from '../environments/environment';
import { AuthService, AuthUser } from './auth.service';
import { BrandLogoComponent } from './shared/brand-logo.component';
import { IconComponent } from './shared/icon.component';
import { ThemeToggleComponent } from './shared/theme-toggle.component';

@Component({
  selector: 'app-auth-page',
  standalone: true,
  imports: [FormsModule, RouterLink, BrandLogoComponent, IconComponent, ThemeToggleComponent],
  template: `
    <main class="auth">
      <!-- Painel de marca -->
      <aside class="showcase">
        <div class="showcase-top">
          <a routerLink="/" aria-label="Boraê — voltar ao início"><bo-brand-logo tone="dark" [size]="34" /></a>
          <a routerLink="/" class="back"><bo-icon name="arrow-left" [size]="16" /> Voltar ao site</a>
        </div>

        <div class="showcase-copy">
          <p class="eyebrow">Sua próxima experiência começa aqui</p>
          <h1>Comer bem também é <em>descobrir.</em></h1>
          <p class="lead">Encontre lugares, descubra pratos e tenha experiências gastronômicas que combinam com você.</p>
          <ul class="perks">
            <li><bo-icon name="map-pin" [size]="18" /> Descubra restaurantes com identidade própria</li>
            <li><bo-icon name="book-open" [size]="18" /> Cardápios digitais visuais e interativos</li>
            <li><bo-icon name="shield-check" [size]="18" /> Acesso protegido com autenticação segura</li>
          </ul>
        </div>

        <p class="showcase-foot">© 2026 Boraê · Uma nova forma de viver a gastronomia.</p>
      </aside>

      <!-- Painel de acesso -->
      <section class="panel" aria-label="Acesso à plataforma">
        <div class="panel-tools">
          @if (demo) { <span class="demo-flag" title="Dados fictícios locais; nada é enviado à API real">Modo demonstração</span> }
          <bo-theme-toggle />
        </div>
        <div class="card">
          @if (user; as signedUser) {
            <div class="welcome"><bo-icon name="check" [size]="26" [strokeWidth]="2.5" /></div>
            <p class="eyebrow">Acesso confirmado</p>
            <h2>Que bom ter você por aqui, {{ signedUser.fullName.split(' ')[0] }}.</h2>
            <p class="copy">Sua conta Boraê está pronta. Acesse seu painel de restaurantes ou explore as novidades da plataforma.</p>
            <div class="user">
              <span class="avatar">{{ signedUser.fullName.charAt(0).toUpperCase() }}</span>
              <span><strong>{{ signedUser.fullName }}</strong><small>{{ signedUser.email }}</small></span>
            </div>
            <div class="actions">
              <a class="btn btn-primary" routerLink="/app">Ir para o painel <bo-icon class="btn-arrow" name="arrow-right" [size]="18" /></a>
              <button class="btn btn-secondary" type="button" (click)="logout()"><bo-icon name="log-out" [size]="16" /> Sair</button>
            </div>
          } @else {
            <p class="eyebrow">{{ mode === 'login' ? 'Bem-vindo de volta' : 'Vamos começar' }}</p>
            <h2>{{ mode === 'login' ? 'Entre na sua conta.' : 'Crie sua conta.' }}</h2>
            <p class="copy">{{ mode === 'login' ? 'Acesse sua experiência Boraê.' : 'É rápido e gratuito. Sua próxima descoberta está mais perto.' }}</p>
            @if (sessionExpired) {
              <div class="error session" role="status"><bo-icon name="clock" [size]="18" /> <span>Sua sessão expirou. Entre novamente para continuar.</span></div>
            }

            <div class="tabs" [class.is-register]="mode === 'register'" role="tablist" aria-label="Tipo de acesso">
              <button type="button" role="tab" [attr.aria-selected]="mode === 'login'" [class.active]="mode === 'login'" (click)="setMode('login')">Entrar</button>
              <button type="button" role="tab" [attr.aria-selected]="mode === 'register'" [class.active]="mode === 'register'" (click)="setMode('register')">Criar conta</button>
            </div>

            <form (ngSubmit)="submit()" #authForm="ngForm" novalidate>
              @if (mode === 'register') {
                <div>
                  <label for="fullName">Nome completo</label>
                  <div class="field">
                    <bo-icon name="user" [size]="18" />
                    <input id="fullName" name="fullName" type="text" [(ngModel)]="fullName" placeholder="Como podemos te chamar?" minlength="2" maxlength="160" autocomplete="name" required>
                  </div>
                </div>
              }
              <div>
                <label for="email">E-mail</label>
                <div class="field">
                  <bo-icon name="mail" [size]="18" />
                  <input id="email" name="email" type="email" [(ngModel)]="email" placeholder="voce@email.com" autocomplete="email" maxlength="255" required email>
                </div>
              </div>
              <div>
                <label for="password">Senha</label>
                <div class="field">
                  <bo-icon name="lock" [size]="18" />
                  <input id="password" name="password" [type]="showPassword ? 'text' : 'password'" [(ngModel)]="password" placeholder="No mínimo 8 caracteres" [attr.autocomplete]="mode === 'login' ? 'current-password' : 'new-password'" [minlength]="mode === 'register' ? 8 : null" required>
                  <button type="button" class="toggle" (click)="showPassword = !showPassword" [attr.aria-label]="showPassword ? 'Ocultar senha' : 'Mostrar senha'" [attr.aria-pressed]="showPassword">
                    <bo-icon [name]="showPassword ? 'eye-off' : 'eye'" [size]="18" />
                  </button>
                </div>
                @if (mode === 'register') {
                  <p class="hint">Use pelo menos 8 caracteres.</p>
                }
              </div>
              @if (errorMessage) {
                <div class="error" role="alert"><bo-icon name="alert" [size]="18" /> <span>{{ errorMessage }}</span></div>
              }
              <button class="btn btn-primary full" type="submit" [disabled]="loading || !authForm.valid" [attr.aria-busy]="loading">
                @if (loading) {
                  <bo-icon name="loader" [size]="18" [spin]="true" /> Aguarde...
                } @else {
                  {{ mode === 'login' ? 'Entrar na minha conta' : 'Criar minha conta' }}
                  <bo-icon class="btn-arrow" name="arrow-right" [size]="18" />
                }
              </button>
            </form>
            <p class="legal">Ao continuar, você concorda com os termos de uso e a política de privacidade do Boraê.</p>
          }
        </div>
      </section>
    </main>
  `,
  styleUrls: ['./auth.tokens.css', './auth.showcase.css', './auth.page.css']
})
export class AuthPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  mode: 'login' | 'register' = 'login';
  fullName = '';
  email = '';
  password = '';
  showPassword = false;
  protected readonly demo = environment.demo;
  loading = false;
  errorMessage = '';
  user: AuthUser | null = this.auth.currentUser;

  sessionExpired = inject(ActivatedRoute).snapshot.queryParamMap.get('sessao') === 'expirada';

  ngOnInit(): void {
    if (this.auth.hasSession) void this.router.navigateByUrl('/app');
  }

  setMode(mode: 'login' | 'register'): void {
    this.mode = mode;
    this.errorMessage = '';
    this.password = '';
    this.showPassword = false;
  }

  submit(): void {
    if (this.loading) return;
    this.loading = true;
    this.errorMessage = '';
    const request = this.mode === 'login'
      ? this.auth.login({ email: this.email.trim(), password: this.password })
      : this.auth.register({ fullName: this.fullName.trim(), email: this.email.trim(), password: this.password });

    request.subscribe({
      next: (response) => {
        this.user = response.user;
        this.loading = false;
        this.fullName = '';
        this.password = '';
        void this.router.navigateByUrl('/app');
      },
      error: (error: unknown) => {
        this.loading = false;
        this.errorMessage = this.auth.getErrorMessage(error, this.mode);
      }
    });
  }

  logout(): void {
    this.auth.logout();
    this.user = null;
    this.email = '';
    this.password = '';
    this.mode = 'login';
  }
}
