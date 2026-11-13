import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BrandLogoComponent } from '../shared/brand-logo.component';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink, BrandLogoComponent],
  template: `
    <footer class="footer" role="contentinfo">
      <div class="container footer-container">
        <!-- Coluna da Marca -->
        <div class="footer-brand-col">
          <a routerLink="/" class="footer-logo-link" aria-label="Boraê — Início">
            <bo-brand-logo tone="dark" [size]="34" />
          </a>
          <p class="footer-tagline">
            Boraê — Experiências gastronômicas mais inteligentes.
          </p>
          <p class="footer-description">
            Conectando quem aprecia o sabor a quem faz acontecer, por meio de inovação, 
            descoberta e experiências digitais contemporâneas.
          </p>
        </div>

        <!-- Coluna de Navegação -->
        <div class="footer-nav-col">
          <span class="footer-col-title">Navegação</span>
          <ul class="footer-links">
            <li><a href="#inicio">Início</a></li>
            <li><a href="#restaurantes">Para restaurantes</a></li>
            <li><a href="#consumidores">Para consumidores</a></li>
            <li><a href="#como-funciona">Como funciona</a></li>
            <li><a href="#sobre">Sobre o Boraê</a></li>
            <li><a href="#faq">Perguntas frequentes</a></li>
          </ul>
        </div>

        <!-- Coluna da Plataforma -->
        <div class="footer-platform-col">
          <span class="footer-col-title">Plataforma</span>
          <ul class="footer-links">
            <li><a routerLink="/auth">Acessar minha conta</a></li>
            <li><a routerLink="/auth">Cadastrar restaurante</a></li>
            <li><a href="#ecossistema">Ecossistema Boraê</a></li>
          </ul>
          <div class="platform-status-badge">
            <span class="status-indicator"></span>
            <span>Plataforma Operacional</span>
          </div>
        </div>
      </div>

      <!-- Barra Inferior de Direitos Autorais -->
      <div class="footer-bottom">
        <div class="container footer-bottom-container">
          <p class="copyright">© 2026 Boraê. Todos os direitos reservados.</p>
          <p class="foundation-note">Tecnologia e gastronomia em constante evolução.</p>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    .footer {
      background-color: #171717;
      color: #E0E0E0;
      padding-top: clamp(48px, 6vw, 72px);
      border-top: 1px solid #292929;
    }

    .footer-container {
      display: grid;
      grid-template-columns: 1fr;
      gap: 40px;
      padding-bottom: clamp(40px, 5vw, 56px);
    }

    @media (min-width: 768px) {
      .footer-container {
        grid-template-columns: 1.5fr 1fr 1fr;
        gap: 48px;
      }
    }

    .footer-brand-col {
      display: flex;
      flex-direction: column;
      gap: 14px;
      max-width: 380px;
    }


    .footer-tagline {
      font-size: 15px;
      font-weight: 700;
      color: var(--color-white);
    }

    .footer-description {
      font-size: 13.5px;
      line-height: 1.6;
      color: #A3A3A3;
    }

    .footer-col-title {
      display: block;
      font-size: 14px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--color-white);
      margin-bottom: 16px;
    }

    .footer-links {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .footer-links a {
      font-size: 14px;
      color: #B3B3B3;
      transition: color var(--transition-fast);
    }

    .footer-links a:hover {
      color: var(--color-primary);
    }

    .platform-status-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      margin-top: 20px;
      padding: 6px 12px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: var(--radius-pill);
      font-size: 12px;
      color: #D1D1D1;
    }

    .status-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #4ADE80;
      box-shadow: 0 0 8px rgba(74, 222, 128, 0.6);
    }

    .footer-bottom {
      border-top: 1px solid #262626;
      padding: 24px 0;
    }

    .footer-bottom-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      text-align: center;
    }

    @media (min-width: 640px) {
      .footer-bottom-container {
        flex-direction: row;
        text-align: left;
      }
    }

    .copyright {
      font-size: 13px;
      color: #8C8C8C;
    }

    .foundation-note {
      font-size: 12.5px;
      color: #737373;
    }
  `]
})
export class FooterComponent {}
