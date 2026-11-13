import { Component } from '@angular/core';
import { IconComponent } from '../shared/icon.component';
import { RevealDirective } from '../shared/reveal.directive';

@Component({
  selector: 'app-consumers',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  template: `
    <section id="consumidores" class="consumers-section" aria-labelledby="consumers-title">
      <div class="container">
        <div class="consumers-grid">
          <!-- Coluna Visual Fotográfica -->
          <div class="visual-col">
            <div class="visual-card" boReveal>
              <img 
                src="assets/images/consumer-dish.jpg" 
                alt="Mesa gourmet repleta de pratos saborosos e experiências gastronômicas compartilhadas" 
                class="consumer-photo"
                width="600"
                height="620"
                loading="lazy"
              >
              <div class="floating-dish-chip">
                <span class="chip-icon"><bo-icon name="leaf" [size]="22" /></span>
                <div class="chip-info">
                  <strong>Descoberta Visual</strong>
                  <span>Sabor que conquista pelos olhos</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Coluna Textual e Recursos para Consumidores -->
          <div class="content-col" [boReveal]="150">
            <div class="eyebrow-tag">PARA QUEM AMA COMER BEM</div>
            
            <h2 id="consumers-title" class="section-title">
              Sua próxima experiência gastronômica começa aqui.
            </h2>
            
            <p class="section-subtitle">
              Conhecer um restaurante é também descobrir seus sabores, sua proposta 
              e tudo aquilo que torna cada experiência única. O Boraê quer aproximar 
              você dessas descobertas.
            </p>

            <!-- Lista de Pilares da Experiência do Usuário -->
            <div class="experience-list">
              <div [boReveal]="0" class="experience-item">
                <div class="exp-bullet">1</div>
                <div class="exp-content">
                  <strong>Descoberta de estabelecimentos</strong>
                  <p>Encontre restaurantes com propostas autorais, identidades marcantes e ambientes acolhedores.</p>
                </div>
              </div>

              <div [boReveal]="60" class="experience-item">
                <div class="exp-bullet">2</div>
                <div class="exp-content">
                  <strong>Exploração de cardápios interativos</strong>
                  <p>Acesse o cardápio no seu smartphone pelo QR Code, com visual fluido e detalhes práticos.</p>
                </div>
              </div>

              <div [boReveal]="120" class="experience-item">
                <div class="exp-bullet">3</div>
                <div class="exp-content">
                  <strong>Apresentação detalhada de pratos</strong>
                  <p>Conheça fotos reais, composição, ingredientes informados e o visual antes de escolher.</p>
                </div>
              </div>

              <div [boReveal]="180" class="experience-item">
                <div class="exp-bullet exp-bullet-plan">4</div>
                <div class="exp-content">
                  <div class="exp-title-row">
                    <strong>Recomendações inteligentes planejadas</strong>
                    <span class="roadmap-badge">Roadmap IA</span>
                  </div>
                  <p>Futura assistência para sugerir pratos que combinam com seu paladar, momento e preferências.</p>
                </div>
              </div>

              <div [boReveal]="240" class="experience-item">
                <div class="exp-bullet exp-bullet-plan">5</div>
                <div class="exp-content">
                  <div class="exp-title-row">
                    <strong>Benefícios e fidelidade planejados</strong>
                    <span class="roadmap-badge">Boraê Club</span>
                  </div>
                  <p>Proposta futura de fidelização individual e rede de vantagens compartilhadas entre parceiros.</p>
                </div>
              </div>
            </div>

            <div class="cta-box">
              <a href="#conheca" class="btn btn-primary">
                Explore o universo Boraê
                <bo-icon class="btn-arrow" name="arrow-right" [size]="18" />
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .consumers-section {
      padding: clamp(64px, 9vw, 110px) 0;
      background-color: var(--color-surface);
      border-top: 1px solid var(--color-border);
      border-bottom: 1px solid var(--color-border);
    }

    .consumers-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: clamp(40px, 6vw, 64px);
      align-items: center;
    }

    @media (min-width: 960px) {
      .consumers-grid {
        grid-template-columns: 0.9fr 1.1fr;
      }
    }

    /* Coluna Visual */
    .visual-col {
      display: flex;
      justify-content: center;
    }

    .visual-card {
      position: relative;
      border-radius: var(--radius-xl);
      overflow: hidden;
      box-shadow: var(--shadow-lg);
      border: 4px solid var(--color-white);
      background: var(--color-white);
      width: 100%;
      max-width: 480px;
    }

    .consumer-photo {
      width: 100%;
      height: 480px;
      object-fit: cover;
      display: block;
    }

    .floating-dish-chip {
      position: absolute;
      bottom: 20px;
      left: 20px;
      right: 20px;
      background: rgba(255, 255, 255, 0.96);
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      border-radius: var(--radius-md);
      padding: 14px 18px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
      border: 1px solid var(--color-border);
    }

    .chip-icon {
      font-size: 24px;
      background: var(--color-primary-light);
      width: 42px;
      height: 42px;
      border-radius: var(--radius-sm);
      display: grid;
      place-items: center;
      flex-shrink: 0;
    }

    .chip-info strong {
      display: block;
      font-size: 13.5px;
      color: var(--color-graphite);
    }

    .chip-info span {
      font-size: 11.5px;
      color: var(--color-text-muted);
    }

    /* Coluna Conteúdo */
    .content-col {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .section-title {
      font-size: clamp(32px, 4.2vw, 48px);
      font-weight: 800;
      color: var(--color-graphite);
      line-height: 1.15;
    }

    .section-subtitle {
      font-size: clamp(16px, 1.8vw, 18px);
      line-height: 1.65;
      color: var(--color-text-muted);
    }

    .experience-list {
      display: flex;
      flex-direction: column;
      gap: 14px;
      margin-top: 10px;
    }

    .experience-item {
      display: flex;
      align-items: flex-start;
      gap: 14px;
      padding: 14px 16px;
      background: var(--color-white);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      transition: all var(--transition-fast);
    }

    .experience-item:hover {
      border-color: var(--color-primary);
      transform: translateX(4px);
    }

    .exp-bullet {
      width: 32px;
      height: 32px;
      background: var(--color-cream);
      color: var(--color-primary-dark);
      border-radius: var(--radius-pill);
      font-weight: 800;
      font-size: 14px;
      display: grid;
      place-items: center;
      flex-shrink: 0;
      border: 1px solid #FFE7D4;
    }

    .exp-bullet-plan {
      background: #FFF5EA;
      color: #A35A00;
      border-color: #FEDCB4;
    }

    .exp-content {
      flex: 1;
    }

    .exp-title-row {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
      margin-bottom: 2px;
    }

    .exp-content strong {
      display: block;
      font-size: 14.5px;
      color: var(--color-graphite);
      font-weight: 700;
    }

    .exp-content p {
      font-size: 13px;
      color: var(--color-text-muted);
      line-height: 1.5;
      margin-top: 3px;
    }

    .cta-box {
      margin-top: 10px;
    }
  `]
})
export class ConsumersComponent {}
