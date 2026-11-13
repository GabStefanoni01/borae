import { Component } from '@angular/core';

@Component({
  selector: 'app-proposal',
  standalone: true,
  template: `
    <section class="proposal-section" aria-labelledby="proposal-title">
      <div class="container">
        <!-- Cabeçalho da Proposta -->
        <div class="section-header">
          <div class="eyebrow-tag">CONECTANDO UNIVERSOS</div>
          <h2 id="proposal-title" class="section-title">Um novo jeito de viver a gastronomia.</h2>
          <p class="section-lead">
            O Boraê nasce para aproximar dois universos que fazem parte da mesma experiência: 
            quem prepara e quem aprecia. Com tecnologia, criatividade e uma visão conectada, 
            queremos tornar a descoberta gastronômica mais visual, inteligente e envolvente.
          </p>
        </div>

        <!-- Grade de Dois Blocos (A e B) -->
        <div class="cards-grid">
          <!-- Bloco A: Para Restaurantes -->
          <article class="audience-card card-restaurant">
            <div class="card-pill">
              <span class="pill-dot"></span>
              <span>Para Restaurantes</span>
            </div>
            
            <div class="card-icon-wrapper">
              <span class="card-icon" aria-hidden="true">🏪</span>
            </div>

            <h3 class="card-title">Sua marca merece ser descoberta.</h3>
            
            <p class="card-text">
              Seu estabelecimento tem uma história, uma identidade e sabores únicos. 
              O Boraê nasce para ajudar restaurantes a construir uma presença digital mais rica, 
              aproximando seus pratos e sua marca das pessoas.
            </p>

            <ul class="feature-checklist" aria-label="Benefícios para restaurantes">
              <li>Cardápios digitais interativos e atualizáveis</li>
              <li>Apresentação estética e valorização da marca</li>
              <li>Preparação para modelos 3D e realidade aumentada</li>
            </ul>

            <div class="card-action">
              <a href="#restaurantes" class="btn btn-secondary full-width">
                Conheça a proposta para restaurantes
                <span aria-hidden="true">→</span>
              </a>
            </div>
          </article>

          <!-- Bloco B: Para Consumidores -->
          <article class="audience-card card-consumer">
            <div class="card-pill pill-consumer">
              <span class="pill-dot dot-consumer"></span>
              <span>Para Consumidores</span>
            </div>
            
            <div class="card-icon-wrapper icon-consumer">
              <span class="card-icon" aria-hidden="true">🍽️</span>
            </div>

            <h3 class="card-title">Descubra o próximo lugar que vai conquistar você.</h3>
            
            <p class="card-text">
              Explore experiências gastronômicas, conheça estabelecimentos e descubra pratos 
              de uma maneira mais visual, interativa e conectada.
            </p>

            <ul class="feature-checklist" aria-label="Benefícios para consumidores">
              <li>Acesso rápido ao cardápio por QR Code</li>
              <li>Detalhes visuais e clareza sobre ingredientes</li>
              <li>Recomendações e benefícios em evolução</li>
            </ul>

            <div class="card-action">
              <a href="#consumidores" class="btn btn-primary full-width">
                Descubra o universo Boraê
                <span aria-hidden="true">→</span>
              </a>
            </div>
          </article>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .proposal-section {
      padding: clamp(64px, 9vw, 108px) 0;
      background-color: var(--color-white);
    }

    .section-header {
      text-align: center;
      max-width: 760px;
      margin: 0 auto clamp(40px, 6vw, 64px);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
    }

    .section-title {
      font-size: clamp(32px, 4.2vw, 48px);
      font-weight: 800;
      color: var(--color-graphite);
    }

    .section-lead {
      font-size: clamp(16px, 1.8vw, 18px);
      line-height: 1.7;
      color: var(--color-text-muted);
    }

    .cards-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 32px;
      max-width: 1080px;
      margin: 0 auto;
    }

    @media (min-width: 820px) {
      .cards-grid {
        grid-template-columns: 1fr 1fr;
      }
    }

    .audience-card {
      border-radius: var(--radius-xl);
      padding: clamp(32px, 5vw, 44px);
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 20px;
      position: relative;
      transition: transform var(--transition-normal), box-shadow var(--transition-normal);
    }

    .card-restaurant {
      background: var(--color-cream);
      border: 1px solid var(--color-border);
    }

    .card-restaurant:hover {
      transform: translateY(-4px);
      box-shadow: var(--shadow-md);
      border-color: #E2D9CE;
    }

    .card-consumer {
      background: #FFFFFF;
      border: 1.5px solid var(--color-primary-light);
      box-shadow: var(--shadow-sm);
    }

    .card-consumer:hover {
      transform: translateY(-4px);
      box-shadow: var(--shadow-md);
      border-color: var(--color-primary);
    }

    .card-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--color-graphite);
      background: rgba(25, 25, 25, 0.06);
      padding: 6px 14px;
      border-radius: var(--radius-pill);
    }

    .pill-consumer {
      background: var(--color-primary-light);
      color: var(--color-primary-dark);
    }

    .pill-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--color-graphite);
    }

    .dot-consumer {
      background: var(--color-primary);
    }

    .card-icon-wrapper {
      width: 56px;
      height: 56px;
      border-radius: var(--radius-md);
      background: var(--color-white);
      display: grid;
      place-items: center;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }

    .icon-consumer {
      background: var(--color-primary-light);
    }

    .card-icon {
      font-size: 28px;
    }

    .card-title {
      font-size: clamp(22px, 2.6vw, 28px);
      font-weight: 800;
      line-height: 1.25;
      color: var(--color-graphite);
    }

    .card-text {
      font-size: 15.5px;
      line-height: 1.65;
      color: var(--color-text-muted);
      flex-grow: 1;
    }

    .feature-checklist {
      list-style: none;
      padding: 0;
      margin: 4px 0 12px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      width: 100%;
    }

    .feature-checklist li {
      font-size: 14px;
      color: var(--color-text-main);
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .feature-checklist li::before {
      content: '✓';
      color: var(--color-primary);
      font-weight: 800;
    }

    .card-action {
      width: 100%;
      margin-top: 10px;
    }

    .full-width {
      width: 100%;
      padding: 14px 20px;
      font-size: 14.5px;
    }
  `]
})
export class ProposalComponent {}
