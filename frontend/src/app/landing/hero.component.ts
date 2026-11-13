import { Component } from '@angular/core';
import { IconComponent } from '../shared/icon.component';

@Component({
  selector: 'app-hero',
  standalone: true,
  imports: [IconComponent],
  template: `
    <section id="inicio" class="hero-section" aria-labelledby="hero-title">
      <div class="container hero-container">
        <!-- Coluna de Texto e Conversão -->
        <div class="hero-content">
          <div class="eyebrow-tag">
            <bo-icon class="eyebrow-icon" name="sparkles" [size]="14" />
            <span>TECNOLOGIA E GASTRONOMIA EM UMA NOVA EXPERIÊNCIA</span>
          </div>

          <h1 id="hero-title" class="hero-title">
            A gastronomia vai além do prato. 
            <span class="text-highlight">Descubra o Boraê.</span>
          </h1>

          <p class="hero-subtitle">
            Uma nova maneira de conectar pessoas, restaurantes e experiências gastronômicas. 
            Mais descoberta, mais interação e tecnologia aproximando quem ama comer de quem faz acontecer.
          </p>

          <div class="hero-actions">
            <a href="#conheca" class="btn btn-primary hero-btn">
              Conheça o Boraê
              <bo-icon class="btn-arrow" name="arrow-right" [size]="18" />
            </a>
            <a href="#restaurantes" class="btn btn-secondary hero-btn">
              Sou um restaurante
            </a>
          </div>

          <!-- Micro Provas e Indicadores -->
          <div class="hero-indicators">
            <div class="indicator-item">
              <span class="indicator-check"><bo-icon name="check" [size]="12" [strokeWidth]="3" /></span>
              <span>Cardápio digital ágil</span>
            </div>
            <div class="indicator-item">
              <span class="indicator-check"><bo-icon name="check" [size]="12" [strokeWidth]="3" /></span>
              <span>Visão 3D & AR em preparo</span>
            </div>
            <div class="indicator-item">
              <span class="indicator-check"><bo-icon name="check" [size]="12" [strokeWidth]="3" /></span>
              <span>Ecossistema Boraê Club</span>
            </div>
          </div>
        </div>

        <!-- Coluna Visual / Composição Fotográfica e Mockup Conceitual -->
        <div class="hero-visual" aria-hidden="true">
          <div class="visual-wrapper">
            <!-- Imagem Gastronômica de Destaque -->
            <div class="image-frame">
              <img 
                src="assets/images/hero-dish.jpg" 
                alt="Prato gastronômico de alta culinária apresentado com sofisticação" 
                class="hero-image"
                width="600"
                height="450"
                loading="eager"
              >
              <div class="image-overlay"></div>
            </div>

            <!-- Card Flutuante / Demonstração Conceitual de Cardápio Mobile -->
            <div class="concept-mockup">
              <div class="mockup-header">
                <span class="mockup-badge">Demonstração conceitual</span>
                <span class="mockup-status">Em desenvolvimento</span>
              </div>
              <div class="mockup-body">
                <div class="dish-preview">
                  <div class="dish-avatar"><bo-icon name="cloche" [size]="22" /></div>
                  <div class="dish-info">
                    <strong>Risoto Trufado com Cogumelos</strong>
                    <span class="dish-meta">Visualização 3D Interativa prevista</span>
                  </div>
                </div>
                <div class="mockup-footer">
                  <div class="qr-pill">
                    <bo-icon class="qr-icon" name="qr-code" [size]="13" />
                    <span>QR Code Boraê</span>
                  </div>
                  <span class="dish-price">Experiência Imersiva</span>
                </div>
              </div>
            </div>

            <!-- Selo Gastronômico Laranja -->
            <div class="floating-badge">
              <span class="floating-badge-symbol"><bo-icon name="chef-hat" [size]="18" /></span>
              <div class="floating-badge-text">
                <strong>Alta Gastronomia</strong>
                <small>Encontra a inovação</small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .hero-section {
      padding: clamp(48px, 8vw, 84px) 0 clamp(56px, 8vw, 96px);
      background: radial-gradient(circle at 80% 20%, #FFF5EC 0%, #FFFFFF 60%);
      position: relative;
      overflow: hidden;
    }
    .hero-container {
      display: grid;
      grid-template-columns: 1fr;
      align-items: center;
      gap: clamp(40px, 6vw, 68px);
    }
    @media (min-width: 992px) {
      .hero-container { grid-template-columns: 1.15fr 0.85fr; }
    }
    .hero-content {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 22px;
      max-width: 620px;
    }
    .hero-title {
      font-size: clamp(38px, 5.2vw, 60px);
      font-weight: 800;
      line-height: 1.1;
      letter-spacing: -0.03em;
      color: var(--color-graphite);
    }
    .text-highlight { color: var(--color-primary); }
    .hero-subtitle {
      font-size: clamp(17px, 2vw, 19px);
      line-height: 1.65;
      color: var(--color-text-muted);
      max-width: 540px;
    }
    .hero-actions {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 16px;
      margin-top: 10px;
      width: 100%;
    }
    .hero-btn { padding: 16px 32px; font-size: 16px; }
    @media (max-width: 480px) {
      .hero-actions { flex-direction: column; }
      .hero-btn { width: 100%; }
    }
    .btn-arrow { font-size: 18px; }
    .hero-indicators {
      display: flex;
      flex-wrap: wrap;
      gap: 18px;
      margin-top: 14px;
      padding-top: 20px;
      border-top: 1px solid var(--color-border);
      width: 100%;
    }
    .indicator-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13.5px;
      font-weight: 600;
      color: var(--color-text-muted);
    }
    .indicator-check {
      display: grid;
      place-items: center;
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background: var(--color-primary-light);
      color: var(--color-primary-dark);
    }
    /* Entrada escalonada do conteúdo e flutuação suave dos selos */
    .hero-content > * { animation: hero-up .8s cubic-bezier(.16, 1, .3, 1) both; }
    .hero-content > :nth-child(2) { animation-delay: .08s; }
    .hero-content > :nth-child(3) { animation-delay: .16s; }
    .hero-content > :nth-child(4) { animation-delay: .24s; }
    .hero-content > :nth-child(5) { animation-delay: .32s; }
    .visual-wrapper { animation: hero-zoom 1s cubic-bezier(.16, 1, .3, 1) .15s both; }
    .concept-mockup { animation: hero-float 6s ease-in-out 1.2s infinite; }
    .floating-badge { animation: hero-float 7s ease-in-out .6s infinite reverse; }
    @keyframes hero-up { from { opacity: 0; transform: translateY(22px); } }
    @keyframes hero-zoom { from { opacity: 0; transform: scale(.96) translateY(16px); } }
    @keyframes hero-float { 50% { transform: translateY(-8px); } }
    .hero-visual { position: relative; display: flex; justify-content: center; }
    .visual-wrapper { position: relative; width: 100%; max-width: 520px; }
    .image-frame {
      position: relative;
      border-radius: var(--radius-xl);
      overflow: hidden;
      box-shadow: var(--shadow-lg);
      border: 6px solid var(--color-white);
      aspect-ratio: 4 / 3.3;
      background-color: var(--color-surface);
    }
    .hero-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transform: scale(1.02);
      transition: transform var(--transition-normal);
    }
    .image-frame:hover .hero-image { transform: scale(1.05); }
    .image-overlay {
      position: absolute;
      inset: 0;
      background: linear-gradient(180deg, rgba(0,0,0,0) 60%, rgba(0,0,0,0.4) 100%);
    }
    .concept-mockup {
      position: absolute;
      bottom: -28px;
      left: -20px;
      background: var(--color-white);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
      padding: 16px 20px;
      box-shadow: var(--shadow-lg);
      width: min(320px, 90%);
      z-index: 2;
    }
    @media (max-width: 520px) {
      .concept-mockup { position: static; margin-top: -24px; width: 100%; }
    }
    .mockup-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 1px dashed var(--color-border);
    }
    .mockup-badge {
      font-size: 11px;
      font-weight: 800;
      text-transform: uppercase;
      color: var(--color-primary-dark);
      background: var(--color-primary-light);
      padding: 2px 8px;
      border-radius: var(--radius-pill);
    }
    .mockup-status { font-size: 10.5px; color: var(--color-text-subtle); font-weight: 500; }
    .dish-preview { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
    .dish-avatar {
      width: 40px;
      height: 40px;
      border-radius: var(--radius-md);
      background: var(--color-primary-light);
      display: grid;
      place-items: center;
      font-size: 20px;
    }
    .dish-info strong { display: block; font-size: 13.5px; color: var(--color-graphite); font-weight: 700; }
    .dish-meta { font-size: 11px; color: var(--color-primary); font-weight: 600; }
    .mockup-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-top: 8px;
      border-top: 1px solid var(--color-surface);
    }
    .qr-pill {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 11px;
      font-weight: 700;
      color: var(--color-graphite);
      background: var(--color-surface);
      padding: 3px 8px;
      border-radius: var(--radius-pill);
    }
    .dish-price { font-size: 11.5px; font-weight: 700; color: var(--color-text-muted); }
    .floating-badge {
      position: absolute;
      top: -16px;
      right: -14px;
      background: var(--color-graphite);
      color: var(--color-white);
      border-radius: var(--radius-pill);
      padding: 10px 18px;
      display: flex;
      align-items: center;
      gap: 10px;
      box-shadow: var(--shadow-md);
      border: 2px solid var(--color-white);
      z-index: 2;
    }
    @media (max-width: 520px) { .floating-badge { display: none; } }
    .floating-badge-symbol {
      display: grid;
      place-items: center;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: var(--color-primary);
      color: var(--color-white);
    }
    .floating-badge-text strong { display: block; font-size: 12px; font-weight: 700; }
    .floating-badge-text small { font-size: 10.5px; color: #D4D4D4; }
  `]
})
export class HeroComponent {}
