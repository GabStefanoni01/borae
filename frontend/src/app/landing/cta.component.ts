import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../shared/icon.component';
import { RevealDirective } from '../shared/reveal.directive';

@Component({
  selector: 'app-cta',
  standalone: true,
  imports: [RouterLink, IconComponent, RevealDirective],
  template: `
    <section id="conheca" class="cta-section" aria-labelledby="cta-title">
      <div class="container">
        <div class="cta-banner" boReveal>
          <div class="cta-content">
            <div class="eyebrow-tag eyebrow-tag-dark">FAÇA PARTE DA EVOLUÇÃO</div>
            
            <h2 id="cta-title" class="cta-title">
              A próxima experiência gastronômica pode começar aqui.
            </h2>
            
            <p class="cta-text">
              Estamos construindo uma nova maneira de conectar pessoas, restaurantes e tecnologia. 
              Conheça o Boraê e acompanhe essa evolução.
            </p>

            <div class="cta-actions">
              <a routerLink="/auth" class="btn btn-primary cta-btn">
                Conheça o Boraê
                <bo-icon class="btn-arrow" name="arrow-right" [size]="18" />
              </a>
              <a href="#restaurantes" class="btn btn-outline-white cta-btn">
                Sou um restaurante
              </a>
            </div>

            <div class="cta-indicators">
              <span><bo-icon name="check-circle" [size]="16" /> Plataforma em desenvolvimento contínuo</span>
              <span><bo-icon name="check-circle" [size]="16" /> Arquitetura escalável e moderna</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .cta-section {
      padding: clamp(64px, 8vw, 96px) 0;
      background-color: var(--color-white);
    }

    .cta-banner {
      background: linear-gradient(135deg, #191919 0%, #2A2420 60%, #362215 100%);
      border-radius: var(--radius-xl);
      padding: clamp(48px, 7vw, 84px) clamp(24px, 5vw, 64px);
      text-align: center;
      position: relative;
      overflow: hidden;
      border: 1px solid #3A322C;
      box-shadow: 0 24px 60px rgba(0, 0, 0, 0.18);
    }

    .cta-banner::before {
      content: '';
      position: absolute;
      top: -120px;
      right: -120px;
      width: 320px;
      height: 320px;
      background: radial-gradient(circle, rgba(255, 114, 0, 0.25) 0%, transparent 70%);
      pointer-events: none;
    }

    .cta-content {
      position: relative;
      z-index: 1;
      max-width: 680px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
    }

    .cta-title {
      font-size: clamp(32px, 4.2vw, 48px);
      font-weight: 800;
      color: var(--color-white);
      line-height: 1.15;
    }

    .cta-text {
      font-size: clamp(16px, 1.8vw, 18px);
      line-height: 1.65;
      color: #D1CEC9;
      max-width: 580px;
    }

    .cta-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 16px;
      margin-top: 10px;
      width: 100%;
    }

    .cta-btn {
      padding: 16px 32px;
      font-size: 16px;
    }

    @media (max-width: 480px) {
      .cta-actions {
        flex-direction: column;
      }
      .cta-btn {
        width: 100%;
      }
    }

    .cta-indicators {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 18px;
      font-size: 13px;
      color: #A8A49E;
      margin-top: 14px;
    }

    .cta-indicators span {
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .cta-indicators bo-icon {
      color: var(--color-primary);
    }
  `]
})
export class CtaComponent {}
