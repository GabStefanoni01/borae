import { Component } from '@angular/core';

@Component({
  selector: 'app-restaurants',
  standalone: true,
  template: `
    <section id="restaurantes" class="restaurants-section" aria-labelledby="restaurants-title">
      <div class="container">
        <div class="restaurants-grid">
          <!-- Coluna Textual e Comercial -->
          <div class="content-col">
            <div class="eyebrow-tag">PARA ESTABELECIMENTOS</div>
            
            <h2 id="restaurants-title" class="section-title">
              Seu restaurante tem muito mais para mostrar.
            </h2>
            
            <p class="section-subtitle">
              Uma presença digital não precisa ser apenas uma lista de produtos. 
              Ela pode representar a identidade do seu estabelecimento e tornar cada descoberta mais interessante.
            </p>

            <!-- Lista de Benefícios Reais e Transparentes -->
            <div class="benefits-list" role="list">
              <div class="benefit-item" role="listitem">
                <div class="benefit-icon-badge">✨</div>
                <div class="benefit-text">
                  <strong>Valorização da identidade do estabelecimento</strong>
                  <p>Apresente sua história, seu ambiente e o cuidado por trás de cada criação gastronômica.</p>
                </div>
              </div>

              <div class="benefit-item" role="listitem">
                <div class="benefit-icon-badge">📸</div>
                <div class="benefit-text">
                  <strong>Apresentação visual dos pratos</strong>
                  <p>Fotos ricas, descrições claras e espaço para o cliente entender a composição de cada item.</p>
                </div>
              </div>

              <div class="benefit-item" role="listitem">
                <div class="benefit-icon-badge">📱</div>
                <div class="benefit-text">
                  <strong>Experiência digital responsiva</strong>
                  <p>Cardápios que carregam rápido no celular do cliente através de QR Codes práticos.</p>
                </div>
              </div>

              <div class="benefit-item" role="listitem">
                <div class="benefit-icon-badge">🤝</div>
                <div class="benefit-text">
                  <strong>Maior conexão com consumidores</strong>
                  <p>Aproxime o cliente que está na mesa ou pesquisando o que comer de forma intuitiva.</p>
                </div>
              </div>

              <div class="benefit-item" role="listitem">
                <div class="benefit-icon-badge">🧊</div>
                <div class="benefit-text">
                  <strong>Preparação para experiências interativas</strong>
                  <p>Arquitetura projetada para suportar modelos 3D e visualizações em realidade aumentada.</p>
                </div>
              </div>

              <div class="benefit-item" role="listitem">
                <div class="benefit-icon-badge">🎁</div>
                <div class="benefit-text">
                  <strong>Evolução para relacionamento e fidelidade</strong>
                  <p>Futuras opções de programas de benefícios próprios e integração ao Boraê Club.</p>
                </div>
              </div>
            </div>

            <div class="cta-box">
              <a href="#conheca" class="btn btn-primary">
                Quero conhecer o Boraê para meu restaurante
                <span aria-hidden="true">→</span>
              </a>
            </div>
          </div>

          <!-- Coluna Visual Fotográfica -->
          <div class="visual-col">
            <div class="visual-card">
              <img 
                src="assets/images/chef-prep.jpg" 
                alt="Chef de cozinha finalizando a apresentação impecável de um prato" 
                class="restaurant-photo"
                width="600"
                height="650"
                loading="lazy"
              >
              <div class="visual-caption">
                <div class="caption-header">
                  <span class="caption-tag">Parceria & Tecnologia</span>
                  <span class="caption-state">Boraê para Parceiros</span>
                </div>
                <p class="caption-quote">
                  "Construído para valorizar quem acorda cedo para cozinhar e encantar clientes todos os dias."
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .restaurants-section {
      padding: clamp(64px, 9vw, 110px) 0;
      background-color: var(--color-white);
    }

    .restaurants-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: clamp(40px, 6vw, 64px);
      align-items: center;
    }

    @media (min-width: 960px) {
      .restaurants-grid {
        grid-template-columns: 1.15fr 0.85fr;
      }
    }

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

    .benefits-list {
      display: grid;
      grid-template-columns: 1fr;
      gap: 18px;
      margin-top: 10px;
    }

    @media (min-width: 600px) {
      .benefits-list {
        grid-template-columns: 1fr 1fr;
      }
    }

    .benefit-item {
      display: flex;
      align-items: flex-start;
      gap: 14px;
      padding: 16px;
      background: var(--color-cream);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      transition: all var(--transition-fast);
    }

    .benefit-item:hover {
      border-color: #DFD5C8;
      background: #FFF5EA;
      transform: translateY(-2px);
    }

    .benefit-icon-badge {
      width: 36px;
      height: 36px;
      background: var(--color-white);
      border-radius: var(--radius-sm);
      display: grid;
      place-items: center;
      font-size: 18px;
      flex-shrink: 0;
      box-shadow: var(--shadow-sm);
    }

    .benefit-text strong {
      display: block;
      font-size: 14.5px;
      color: var(--color-graphite);
      margin-bottom: 4px;
      font-weight: 700;
    }

    .benefit-text p {
      font-size: 13px;
      color: var(--color-text-muted);
      line-height: 1.5;
    }

    .cta-box {
      margin-top: 14px;
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
      background: var(--color-surface);
      width: 100%;
      max-width: 480px;
    }

    .restaurant-photo {
      width: 100%;
      height: 480px;
      object-fit: cover;
      display: block;
    }

    .visual-caption {
      position: absolute;
      bottom: 16px;
      left: 16px;
      right: 16px;
      background: rgba(25, 25, 25, 0.9);
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      border-radius: var(--radius-lg);
      padding: 16px 20px;
      color: var(--color-white);
      border: 1px solid rgba(255, 255, 255, 0.15);
    }

    .caption-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 6px;
    }

    .caption-tag {
      font-size: 11px;
      font-weight: 800;
      text-transform: uppercase;
      color: var(--color-primary);
    }

    .caption-state {
      font-size: 11px;
      color: #A3A3A3;
    }

    .caption-quote {
      font-size: 13.5px;
      line-height: 1.45;
      color: #E5E5E5;
      font-style: italic;
    }
  `]
})
export class RestaurantsComponent {}
