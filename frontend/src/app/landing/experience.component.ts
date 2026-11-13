import { Component } from '@angular/core';

@Component({
  selector: 'app-experience',
  standalone: true,
  template: `
    <section class="experience-section" aria-labelledby="experience-title">
      <div class="container">
        <!-- Cabeçalho da Seção -->
        <div class="section-header">
          <div class="eyebrow-tag">INOVAÇÃO GASTRONÔMICA</div>
          <h2 id="experience-title" class="section-title">A tecnologia encontra o sabor.</h2>
          <p class="section-lead">
            O futuro da gastronomia também passa pela forma como conhecemos cada prato. 
            O Boraê está sendo desenvolvido para transformar cardápios tradicionais 
            em experiências digitais mais ricas, conectadas e intuitivas.
          </p>
        </div>

        <!-- Grade de 4 Pilares -->
        <div class="pillars-grid">
          <!-- Card 1: Cardápio Digital -->
          <article class="pillar-card">
            <div class="card-top">
              <div class="pillar-icon">📜</div>
              <span class="roadmap-badge">Roadmap: Catálogo</span>
            </div>
            <h3 class="pillar-title">Seu cardápio, em uma nova dimensão.</h3>
            <p class="pillar-text">
              Uma experiência digital pensada para apresentar categorias, pratos, 
              informações e identidade visual de cada estabelecimento.
            </p>
            <div class="pillar-meta">
              <span>Navegação responsiva & QR Code</span>
            </div>
          </article>

          <!-- Card 2: Experiência 3D e AR -->
          <article class="pillar-card highlight-pillar">
            <div class="card-top">
              <div class="pillar-icon">🧊</div>
              <span class="roadmap-badge">Roadmap: Imersão</span>
            </div>
            <h3 class="pillar-title">Veja além da descrição.</h3>
            <p class="pillar-text">
              Estamos construindo experiências de visualização tridimensional e realidade 
              aumentada para aproximar o consumidor dos detalhes de cada prato.
            </p>
            <div class="pillar-meta">
              <span>Modelos GLB & Realidade Aumentada</span>
            </div>
          </article>

          <!-- Card 3: Inteligência Gastronômica -->
          <article class="pillar-card">
            <div class="card-top">
              <div class="pillar-icon">🧠</div>
              <span class="roadmap-badge">Roadmap: IA Contextual</span>
            </div>
            <h3 class="pillar-title">Descobrir pode ser mais inteligente.</h3>
            <p class="pillar-text">
              Uma proposta de recomendações contextualizadas, conectando preferências 
              e opções disponíveis nos estabelecimentos.
            </p>
            <div class="pillar-meta">
              <span>Sugestões baseadas no cardápio real</span>
            </div>
          </article>

          <!-- Card 4: Fidelidade -->
          <article class="pillar-card">
            <div class="card-top">
              <div class="pillar-icon">⭐</div>
              <span class="roadmap-badge">Roadmap: Fidelização</span>
            </div>
            <h3 class="pillar-title">Relacionamentos que continuam.</h3>
            <p class="pillar-text">
              Uma experiência de fidelidade pensada para aproximar restaurantes 
              e consumidores, valorizando a recorrência e o relacionamento.
            </p>
            <div class="pillar-meta">
              <span>Programas individuais & engajamento</span>
            </div>
          </article>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .experience-section {
      padding: clamp(64px, 9vw, 104px) 0;
      background-color: var(--color-surface);
      border-top: 1px solid var(--color-border);
      border-bottom: 1px solid var(--color-border);
    }

    .section-header {
      text-align: center;
      max-width: 740px;
      margin: 0 auto clamp(40px, 6vw, 64px);
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
      line-height: 1.7;
      color: var(--color-text-muted);
    }

    .pillars-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 24px;
    }

    @media (min-width: 600px) {
      .pillars-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (min-width: 1040px) {
      .pillars-grid {
        grid-template-columns: repeat(4, 1fr);
      }
    }

    .pillar-card {
      background: var(--color-white);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
      padding: 30px 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
      transition: all var(--transition-fast);
      position: relative;
    }

    .pillar-card:hover {
      transform: translateY(-4px);
      box-shadow: var(--shadow-md);
      border-color: #E2D9CE;
    }

    .highlight-pillar {
      border-color: #FFD8BA;
      background: linear-gradient(180deg, #FFFFFF 0%, #FFFDFB 100%);
    }

    .highlight-pillar:hover {
      border-color: var(--color-primary);
    }

    .card-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
    }

    .pillar-icon {
      width: 48px;
      height: 48px;
      background: var(--color-cream);
      border-radius: var(--radius-md);
      display: grid;
      place-items: center;
      font-size: 24px;
    }

    .pillar-title {
      font-size: 20px;
      font-weight: 800;
      color: var(--color-graphite);
      line-height: 1.3;
    }

    .pillar-text {
      font-size: 14.5px;
      line-height: 1.65;
      color: var(--color-text-muted);
      flex-grow: 1;
    }

    .pillar-meta {
      padding-top: 14px;
      border-top: 1px solid var(--color-surface);
      font-size: 12px;
      font-weight: 600;
      color: var(--color-primary-dark);
    }
  `]
})
export class ExperienceComponent {}
