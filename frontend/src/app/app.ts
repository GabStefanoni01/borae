import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, AuthUser } from './auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule],
  template: `
    <main class="page">
      <nav class="topbar">
        <a class="brand" href="/" aria-label="Boraê início">bor<span>aê</span></a>
        <span class="top-note">COMIDA BOA. ESCOLHAS MELHORES.</span>
      </nav>

      <section class="content">
        <div class="intro">
          <p class="eyebrow">SUA PRÓXIMA EXPERIÊNCIA COMEÇA AQUI</p>
          <h1>Comer bem<br>também é <em>descobrir.</em></h1>
          <p class="description">Encontre lugares, descubra pratos e tenha experiências gastronômicas que combinam com você.</p>
          <div class="visual-note"><span class="spark">✳</span><span>Descubra. Experimente. Boraê.</span></div>
        </div>

        <section class="auth-card" aria-label="Acesso à plataforma">
          @if (user; as signedUser) {
            <div class="welcome-icon">✓</div>
            <p class="eyebrow">ACESSO CONFIRMADO</p>
            <h2>Que bom ter você por aqui, {{ signedUser.fullName.split(' ')[0] }}.</h2>
            <p class="card-copy">Sua conta Boraê está pronta. Em breve você poderá explorar restaurantes, cardápios e benefícios por aqui.</p>
            <div class="user-summary">
              <span class="avatar">{{ signedUser.fullName.charAt(0).toUpperCase() }}</span>
              <span><strong>{{ signedUser.fullName }}</strong><small>{{ signedUser.email }}</small></span>
            </div>
            <button class="button secondary full" type="button" (click)="logout()">Sair da conta</button>
          } @else {
            <div class="card-heading">
              <p class="eyebrow">{{ mode === 'login' ? 'BEM-VINDO DE VOLTA' : 'VAMOS COMEÇAR' }}</p>
              <h2>{{ mode === 'login' ? 'Entre na sua conta.' : 'Crie sua conta.' }}</h2>
              <p class="card-copy">{{ mode === 'login' ? 'Acesse sua experiência Boraê.' : 'É rápido e gratuito. Sua próxima descoberta está mais perto.' }}</p>
            </div>

            <div class="tabs" role="tablist" aria-label="Tipo de acesso">
              <button type="button" role="tab" [attr.aria-selected]="mode === 'login'" [class.active]="mode === 'login'" (click)="setMode('login')">Entrar</button>
              <button type="button" role="tab" [attr.aria-selected]="mode === 'register'" [class.active]="mode === 'register'" (click)="setMode('register')">Criar conta</button>
            </div>

            <form (ngSubmit)="submit()" #authForm="ngForm">
              @if (mode === 'register') {
                <label for="fullName">Nome completo</label>
                <input id="fullName" name="fullName" type="text" [(ngModel)]="fullName" placeholder="Como podemos te chamar?" minlength="2" maxlength="160" autocomplete="name" required>
              }
              <label for="email">E-mail</label>
              <input id="email" name="email" type="email" [(ngModel)]="email" placeholder="voce@email.com" autocomplete="email" maxlength="255" required>
              <label for="password">Senha</label>
              <input id="password" name="password" type="password" [(ngModel)]="password" placeholder="No mínimo 8 caracteres" [attr.autocomplete]="mode === 'login' ? 'current-password' : 'new-password'" [minlength]="mode === 'register' ? 8 : null" required>
              @if (mode === 'register') {
                <p class="hint">Use pelo menos 8 caracteres.</p>
              }
              @if (errorMessage) {
                <div class="error" role="alert">{{ errorMessage }}</div>
              }
              <button class="button primary full" type="submit" [disabled]="loading || !authForm.valid">
                {{ loading ? 'Aguarde...' : (mode === 'login' ? 'Entrar na minha conta' : 'Criar minha conta') }}
                @if (!loading) { <span aria-hidden="true">↗</span> }
              </button>
            </form>
            <p class="legal">Ao continuar, você concorda com os termos de uso e a política de privacidade do Boraê.</p>
          }
        </section>
      </section>
      <footer><span>© 2026 Boraê</span><span>Uma nova forma de viver a gastronomia.</span></footer>
    </main>
  `,
  styleUrl: './app.css'
})
export class App implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  mode: 'login' | 'register' = 'login';
  fullName = '';
  email = '';
  password = '';
  loading = false;
  errorMessage = '';
  user: AuthUser | null = this.auth.currentUser;

  ngOnInit(): void {
    if (this.auth.hasSession) void this.router.navigateByUrl('/app');
  }

  setMode(mode: 'login' | 'register'): void {
    this.mode = mode;
    this.errorMessage = '';
    this.password = '';
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
