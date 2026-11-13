import { Component } from '@angular/core';

@Component({
  selector: 'app-purpose',
  standalone: true,
  template: `
    <section id="sobre" class="purpose-section" aria-labelledby="purpose-title">
      <div class="container">
        <div class="purpose-wrapper">
          <!-- Coluna Visual Fotográfica -->
          <div class="visual-col">
            <div class="image-box">
              <img 
                src="assets/images/restaurant-kitchen.jpg" 
                alt="Ambiente acolhedor e vibrante de um restaurante iluminado" 
                class="purpose-image"
                width="600"
                height="540"
                loading="lazy"
              >
              <div class="image-badge">
                <span class="badge-icon">🧡</span>
                <span>Paixão pela Gastronomia</span>
              </div>
            </div>
          </div>

          <!-- Coluna Textual Emocional -->
          <div class="content-col">
            <div class="eyebrow-tag">NOSSO PROPÓSITO</div>

            <h2 id="purpose-title" class="section-title">
              Acreditamos que boas experiências merecem novas possibilidades.
            </h2>

            <p class="section-text">
              O Boraê surgiu da ideia de aproximar gastronomia e tecnologia. Queremos 
              construir uma plataforma que valorize os estabelecimentos, facilite a descoberta 
              e transforme a relação das pessoas com as experiências gastronômicas.
            </p>

            <div class="values-grid">
              <div class="value-item">
                <span class="value-bullet">01</span>
                <div>
                  <strong>Valorização Autêntica</strong>
                  <p>Cada receita, restaurante e ambiente carrega dedicação e identidade única.</p>
                </div>
              </div>

              <div class="value-item">
                <span class="value-bullet">02</span>
                <div>
                  <strong>Tecnologia com Propósito</strong>
                  <p>Inovação digital que simplifica a escolha e aproxima pessoas sem complicações.</p>
                </div>
              </div>

              <div class="value-item">
                <span class="value-bullet">03</span>
                <div>
                  <strong>Evolução Transparente</strong>
                  <p>Construção passo a passo com foco em qualidade de engenharia e respeito à gastronomia.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .purpose-section {
      padding: clamp(64px, 9vw, 110px) 0;
      background-color: var(--color-white);
    }

    .purpose-wrapper {
      display: grid;
      grid-template-columns: 1fr;
      gap: clamp(40px, 6vw, 68px);
      align-items: center;
    }

    @media (min-width: 960px) {
      .purpose-wrapper {
        grid-template-columns: 0.95fr 1.05fr;
      }
    }

    .visual-col {
      display: flex;
      justify-content: center;
    }

    .image-box {
      position: relative;
      border-radius: var(--radius-xl);
      overflow: hidden;
      box-shadow: var(--shadow-lg);
      border: 4px solid var(--color-white);
      width: 100%;
      max-width: 500px;
    }

    .purpose-image {
      width: 100%;
      height: 440px;
      object-fit: cover;
      display: block;
    }

    .image-badge {
      position: absolute;
      bottom: 20px;
      left: 20px;
      background: rgba(25, 25, 25, 0.92);
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      border-radius: var(--radius-pill);
      padding: 10px 18px;
      display: flex;
      align-items: center;
      gap: 10px;
      color: var(--color-white);
      font-size: 13px;
      font-weight: 700;
      border: 1px solid rgba(255, 255, 255, 0.2);
    }

    .content-col {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .section-title {
      font-size: clamp(32px, 4.2vw, 46px);
      font-weight: 800;
      color: var(--color-graphite);
      line-height: 1.15;
    }

    .section-text {
      font-size: clamp(16px, 1.8vw, 18px);
      line-height: 1.7;
      color: var(--color-text-muted);
    }

    .values-grid {
      display: flex;
      flex-direction: column;
      gap: 16px;
      margin-top: 10px;
    }

    .value-item {
      display: flex;
      align-items: flex-start;
      gap: 14px;
      padding: 14px 18px;
      background: var(--color-cream);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
    }

    .value-bullet {
      font-size: 13px;
      font-weight: 800;
      color: var(--color-primary);
      background: var(--color-white);
      width: 30px;
      height: 30px;
      border-radius: var(--radius-pill);
      display: grid;
      place-items: center;
      flex-shrink: 0;
      box-shadow: var(--shadow-sm);
    }

    .value-item strong {
      display: block;
      font-size: 14.5px;
      color: var(--color-graphite);
      margin-bottom: 2px;
    }

    .value-item p {
      font-size: 13px;
      color: var(--color-text-muted);
      line-height: 1.5;
    }
  `]
})
export class PurposeComponent {}
