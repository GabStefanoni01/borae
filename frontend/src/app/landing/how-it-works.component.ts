import { Component } from '@angular/core';
import { IconComponent } from '../shared/icon.component';
import { RevealDirective } from '../shared/reveal.directive';

@Component({
  selector: 'app-how-it-works',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  template: `
    <section id="como-funciona" class="how-section" aria-labelledby="how-title">
      <div class="container">
        <!-- Cabeçalho -->
        <div class="section-header" boReveal>
          <div class="eyebrow-tag">JORNADA GASTRONÔMICA</div>
          <h2 id="how-title" class="section-title">Uma experiência conectada em cada etapa.</h2>
          <p class="section-lead">
            Conheça o fluxo planejado para conectar estabelecimentos e apreciadores da boa mesa, 
            desde a primeira busca até o relacionamento pós-visita.
          </p>
          <div class="roadmap-badge">Experiência proposta pelo produto</div>
        </div>

        <!-- Grade das 3 Etapas -->
        <div class="steps-grid">
          <!-- Etapa 1: Descubra -->
          <article [boReveal]="0" class="step-card">
            <div class="step-badge">
              <span class="step-num">01</span>
              <span class="step-line" aria-hidden="true"></span>
            </div>
            <div class="step-icon-box"><bo-icon name="search" [size]="24" /></div>
            <h3 class="step-title">Descubra</h3>
            <p class="step-text">
              Encontre estabelecimentos e conheça suas propostas gastronômicas.
            </p>
            <div class="step-detail">
              <span>Diretório & Identidade do local</span>
            </div>
          </article>

          <!-- Etapa 2: Explore -->
          <article [boReveal]="120" class="step-card highlight-step">
            <div class="step-badge">
              <span class="step-num num-highlight">02</span>
              <span class="step-line" aria-hidden="true"></span>
            </div>
            <div class="step-icon-box box-highlight"><bo-icon name="book-open" [size]="24" /></div>
            <h3 class="step-title">Explore</h3>
            <p class="step-text">
              Conheça cardápios e descubra os detalhes de cada experiência.
            </p>
            <div class="step-detail">
              <span>Cardápio digital, fotos & visual 3D</span>
            </div>
          </article>

          <!-- Etapa 3: Conecte-se -->
          <article [boReveal]="240" class="step-card">
            <div class="step-badge">
              <span class="step-num">03</span>
            </div>
            <div class="step-icon-box"><bo-icon name="link" [size]="24" /></div>
            <h3 class="step-title">Conecte-se</h3>
            <p class="step-text">
              Uma experiência que aproxima consumidores e restaurantes, com possibilidades 
              futuras de recomendações e fidelidade.
            </p>
            <div class="step-detail">
              <span>Relacionamento contínuo & Boraê Club</span>
            </div>
          </article>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .how-section {
      padding: clamp(64px, 9vw, 108px) 0;
      background-color: var(--color-white);
    }

    .section-header {
      text-align: center;
      max-width: 720px;
      margin: 0 auto clamp(40px, 6vw, 68px);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
    }

    .section-title {
      font-size: clamp(32px, 4.2vw, 46px);
      font-weight: 800;
      color: var(--color-graphite);
    }

    .section-lead {
      font-size: clamp(16px, 1.8vw, 18px);
      line-height: 1.65;
      color: var(--color-text-muted);
    }

    .steps-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 28px;
      position: relative;
    }

    @media (min-width: 840px) {
      .steps-grid {
        grid-template-columns: repeat(3, 1fr);
      }
    }

    .step-card {
      background: var(--color-cream);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-xl);
      padding: clamp(30px, 4vw, 40px) 28px;
      display: flex;
      flex-direction: column;
      gap: 18px;
      position: relative;
      transition: all var(--transition-normal);
    }

    .step-card:hover {
      transform: translateY(-4px);
      box-shadow: var(--shadow-md);
      border-color: #E2D9CE;
    }

    .highlight-step {
      background: #FFFFFF;
      border: 2px solid var(--color-primary);
      box-shadow: 0 8px 30px rgba(255, 114, 0, 0.08);
    }

    .step-badge {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .step-num {
      font-size: 18px;
      font-weight: 800;
      font-family: var(--font-family-headings);
      color: var(--color-text-subtle);
      width: 44px;
      height: 44px;
      border-radius: var(--radius-pill);
      background: var(--color-white);
      display: grid;
      place-items: center;
      box-shadow: var(--shadow-sm);
    }

    .num-highlight {
      background: var(--color-primary);
      color: var(--color-white);
    }

    .step-line {
      display: none;
    }

    @media (min-width: 840px) {
      .step-line {
        display: block;
        flex: 1;
        height: 2px;
        background: var(--color-border);
      }
    }

    .step-icon-box {
      width: 52px;
      height: 52px;
      background: var(--color-white);
      border-radius: var(--radius-md);
      display: grid;
      place-items: center;
      font-size: 26px;
      box-shadow: var(--shadow-sm);
    }

    .box-highlight {
      background: var(--color-primary-light);
    }

    .step-title {
      font-size: 24px;
      font-weight: 800;
      color: var(--color-graphite);
    }

    .step-text {
      font-size: 15px;
      line-height: 1.6;
      color: var(--color-text-muted);
      flex-grow: 1;
    }

    .step-detail {
      padding-top: 14px;
      border-top: 1px dashed var(--color-border);
      font-size: 12.5px;
      font-weight: 600;
      color: var(--color-primary-dark);
    }
  `]
})
export class HowItWorksComponent {}
