import { Component, HostListener, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="header" [class.scrolled]="isScrolled()">
      <div class="container header-container">
        <!-- Logo Oficial Boraê -->
        <a routerLink="/" class="brand-link" aria-label="Boraê — Início">
          <img 
            src="assets/images/borae-logo.png" 
            alt="Logo Boraê" 
            class="brand-logo"
            width="120"
            height="120"
          >
          <span class="sr-only">Boraê</span>
        </a>

        <!-- Navegação Desktop -->
        <nav class="desktop-nav" aria-label="Navegação principal">
          <ul class="nav-list">
            <li><a href="#inicio" class="nav-link">Início</a></li>
            <li><a href="#restaurantes" class="nav-link">Para restaurantes</a></li>
            <li><a href="#consumidores" class="nav-link">Para consumidores</a></li>
            <li><a href="#como-funciona" class="nav-link">Como funciona</a></li>
            <li><a href="#sobre" class="nav-link">Sobre o Boraê</a></li>
          </ul>
        </nav>

        <!-- Ações do Header -->
        <div class="header-actions">
          <a routerLink="/auth" class="login-link">Entrar</a>
          <a href="#conheca" class="btn btn-primary header-cta">Conheça o Boraê</a>
          
          <!-- Botão Menu Mobile -->
          <button 
            type="button" 
            class="mobile-toggle" 
            [attr.aria-expanded]="mobileMenuOpen()"
            aria-controls="mobile-drawer"
            (click)="toggleMobileMenu()"
            [attr.aria-label]="mobileMenuOpen() ? 'Fechar menu' : 'Abrir menu de navegação'"
          >
            <span class="hamburger-bar" [class.open]="mobileMenuOpen()"></span>
            <span class="hamburger-bar" [class.open]="mobileMenuOpen()"></span>
            <span class="hamburger-bar" [class.open]="mobileMenuOpen()"></span>
          </button>
        </div>
      </div>

      <!-- Drawer de Navegação Mobile -->
      @if (mobileMenuOpen()) {
        <div class="mobile-backdrop" (click)="closeMobileMenu()" aria-hidden="true"></div>
      }
      <div 
        id="mobile-drawer"
        class="mobile-drawer" 
        [class.active]="mobileMenuOpen()"
        role="dialog"
        aria-label="Menu móvel"
        [attr.aria-modal]="mobileMenuOpen()"
      >
        <div class="mobile-drawer-header">
          <img src="assets/images/borae-logo.png" alt="Logo Boraê" class="drawer-logo" width="48" height="48">
          <button type="button" class="close-btn" (click)="closeMobileMenu()" aria-label="Fechar menu">✕</button>
        </div>
        <nav class="mobile-nav" aria-label="Navegação móvel">
          <ul class="mobile-nav-list">
            <li><a href="#inicio" class="mobile-link" (click)="closeMobileMenu()">Início</a></li>
            <li><a href="#restaurantes" class="mobile-link" (click)="closeMobileMenu()">Para restaurantes</a></li>
            <li><a href="#consumidores" class="mobile-link" (click)="closeMobileMenu()">Para consumidores</a></li>
            <li><a href="#como-funciona" class="mobile-link" (click)="closeMobileMenu()">Como funciona</a></li>
            <li><a href="#sobre" class="mobile-link" (click)="closeMobileMenu()">Sobre o Boraê</a></li>
            <li><a href="#faq" class="mobile-link" (click)="closeMobileMenu()">Perguntas frequentes</a></li>
          </ul>
        </nav>
        <div class="mobile-actions">
          <a routerLink="/auth" class="btn btn-secondary full-width" (click)="closeMobileMenu()">Acessar conta</a>
          <a href="#conheca" class="btn btn-primary full-width" (click)="closeMobileMenu()">Conheça o Boraê</a>
        </div>
      </div>
    </header>
  `,
  styles: [`
    :host {
      display: block;
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .header {
      background-color: rgba(255, 255, 255, 0.94);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
      border-bottom: 1px solid transparent;
      transition: all var(--transition-fast);
      height: var(--header-height);
      display: flex;
      align-items: center;
    }

    .header.scrolled {
      background-color: rgba(255, 255, 255, 0.98);
      border-bottom-color: var(--color-border);
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
    }

    .header-container {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;
    }

    .brand-link {
      display: flex;
      align-items: center;
      text-decoration: none;
    }

    .brand-logo {
      height: 48px;
      width: auto;
      object-fit: contain;
      border-radius: var(--radius-sm);
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      border: 0;
    }

    .desktop-nav {
      display: none;
    }

    @media (min-width: 960px) {
      .desktop-nav {
        display: block;
      }
    }

    .nav-list {
      display: flex;
      align-items: center;
      gap: 28px;
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .nav-link {
      font-size: 14.5px;
      font-weight: 600;
      color: var(--color-text-muted);
      transition: color var(--transition-fast);
      padding: 8px 0;
      position: relative;
    }

    .nav-link:hover {
      color: var(--color-primary);
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .login-link {
      font-size: 14.5px;
      font-weight: 700;
      color: var(--color-graphite);
      padding: 8px 14px;
      border-radius: var(--radius-pill);
      transition: all var(--transition-fast);
    }

    .login-link:hover {
      color: var(--color-primary);
      background-color: var(--color-primary-light);
    }

    .header-cta {
      display: none;
      padding: 10px 20px;
      font-size: 14px;
    }

    @media (min-width: 640px) {
      .header-cta {
        display: inline-flex;
      }
    }

    .mobile-toggle {
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 5px;
      width: 40px;
      height: 40px;
      background: transparent;
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      padding: 8px;
    }

    @media (min-width: 960px) {
      .mobile-toggle {
        display: none;
      }
    }

    .hamburger-bar {
      width: 100%;
      height: 2px;
      background-color: var(--color-graphite);
      border-radius: 2px;
      transition: all var(--transition-fast);
    }

    .hamburger-bar.open:nth-child(1) {
      transform: translateY(7px) rotate(45deg);
    }

    .hamburger-bar.open:nth-child(2) {
      opacity: 0;
    }

    .hamburger-bar.open:nth-child(3) {
      transform: translateY(-7px) rotate(-45deg);
    }

    .mobile-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.4);
      z-index: 101;
    }

    .mobile-drawer {
      position: fixed;
      top: 0;
      right: 0;
      bottom: 0;
      width: min(320px, 85vw);
      background: var(--color-white);
      z-index: 102;
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 28px;
      box-shadow: -8px 0 30px rgba(0, 0, 0, 0.15);
      transform: translateX(100%);
      transition: transform var(--transition-normal);
    }

    .mobile-drawer.active {
      transform: translateX(0);
    }

    .mobile-drawer-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .drawer-logo {
      height: 38px;
      width: auto;
      object-fit: contain;
    }

    .close-btn {
      background: transparent;
      border: 0;
      font-size: 20px;
      color: var(--color-graphite);
      padding: 6px;
    }

    .mobile-nav-list {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .mobile-link {
      font-size: 17px;
      font-weight: 600;
      color: var(--color-text-main);
      display: block;
      padding: 6px 0;
    }

    .mobile-link:hover {
      color: var(--color-primary);
    }

    .mobile-actions {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-top: auto;
    }

    .full-width {
      width: 100%;
    }
  `]
})
export class HeaderComponent {
  isScrolled = signal(false);
  mobileMenuOpen = signal(false);

  @HostListener('window:scroll')
  onScroll(): void {
    if (typeof window !== 'undefined') {
      this.isScrolled.set(window.scrollY > 20);
    }
  }

  @HostListener('window:keydown.escape')
  onEscape(): void {
    if (this.mobileMenuOpen()) {
      this.closeMobileMenu();
    }
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}
