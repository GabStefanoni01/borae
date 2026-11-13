import { Component } from '@angular/core';
import { IconComponent } from '../shared/icon.component';
import { RevealDirective } from '../shared/reveal.directive';

@Component({
  selector: 'app-ecosystem',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  template: `
    <section id="ecossistema" class="ecosystem-section" aria-labelledby="ecosystem-title">
      <div class="container">
        <!-- Cabeçalho -->
        <div class="section-header" boReveal>
          <div class="eyebrow-tag">ARQUITETURA INTEGRADA</div>
          <h2 id="ecosystem-title" class="section-title">
            Mais do que uma plataforma. Um ecossistema gastronômico.
          </h2>
          <p class="section-lead">
            O Boraê está sendo construído para conectar diferentes partes da experiência 
            gastronômica em um ambiente digital integrado.
          </p>
        </div>

        <!-- Grade dos 6 Pilares do Ecossistema -->
        <div class="ecosystem-grid">
          <!-- Pilar 1 -->
          <article [boReveal]="0" class="eco-card">
            <div class="eco-icon-wrap"><bo-icon name="building" [size]="24" /></div>
            <h3 class="eco-title">Restaurantes Parceiros</h3>
            <p class="eco-desc">
              Gestão de presença digital, controle de equipe e valorização dos diferenciais culinários.
            </p>
            <span class="eco-status active-status">Fundação ativa</span>
          </article>

          <!-- Pilar 2 -->
          <article [boReveal]="70" class="eco-card">
            <div class="eco-icon-wrap"><bo-icon name="users" [size]="24" /></div>
            <h3 class="eco-title">Consumidores</h3>
            <p class="eco-desc">
              Comunidade de amantes da gastronomia que buscam novos sabores com facilidade e transparência.
            </p>
            <span class="eco-status active-status">Acesso unificado</span>
          </article>

          <!-- Pilar 3 -->
          <article [boReveal]="140" class="eco-card">
            <div class="eco-icon-wrap"><bo-icon name="smartphone" [size]="24" /></div>
            <h3 class="eco-title">Experiências Digitais</h3>
            <p class="eco-desc">
              Cardápios responsivos por QR Code e exploração visual de pratos em dispositivos modernos.
            </p>
            <span class="eco-status roadmap-status">Em desenvolvimento</span>
          </article>

          <!-- Pilar 4 -->
          <article [boReveal]="210" class="eco-card">
            <div class="eco-icon-wrap"><bo-icon name="sparkles" [size]="24" /></div>
            <h3 class="eco-title">Inteligência Gastronômica</h3>
            <p class="eco-desc">
              Recomendações contextuais de itens reais com base em preferências e ingredientes informados.
            </p>
            <span class="eco-status roadmap-status">Visão planejada</span>
          </article>

          <!-- Pilar 5 -->
          <article [boReveal]="280" class="eco-card">
            <div class="eco-icon-wrap"><bo-icon name="target" [size]="24" /></div>
            <h3 class="eco-title">Fidelidade Individual</h3>
            <p class="eco-desc">
              Programas exclusivos configurados por cada estabelecimento para valorizar seus clientes frequentes.
            </p>
            <span class="eco-status roadmap-status">Visão planejada</span>
          </article>

          <!-- Pilar 6 — Destaque Boraê Club -->
          <article [boReveal]="350" class="eco-card club-card">
            <div class="eco-icon-wrap club-icon"><bo-icon name="star" [size]="24" /></div>
            <h3 class="eco-title club-title">Boraê Club</h3>
            <p class="eco-desc">
              Proposta de benefícios compartilhados entre estabelecimentos parceiros, criando uma rede gastronômica colaborativa.
            </p>
            <span class="eco-status club-status">Visão de futuro compartilhado</span>
          </article>
        </div>

        <!-- Alerta de Transparência -->
        <div class="transparency-note" role="note" boReveal>
          <bo-icon class="note-icon" name="info" [size]="20" />
          <p>
            <strong>Transparência Boraê:</strong> A rede de benefícios Boraê Club e os módulos de inteligência e 3D/AR 
            fazem parte da visão futura da plataforma e estão sendo construídos incrementalmente.
          </p>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .ecosystem-section {
      padding: clamp(64px, 9vw, 110px) 0;
      background-color: var(--color-surface);
      border-top: 1px solid var(--color-border);
      border-bottom: 1px solid var(--color-border);
    }

    .section-header {
      text-align: center;
      max-width: 780px;
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
      line-height: 1.65;
      color: var(--color-text-muted);
    }

    .ecosystem-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 24px;
    }

    @media (min-width: 640px) {
      .ecosystem-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (min-width: 1024px) {
      .ecosystem-grid {
        grid-template-columns: repeat(3, 1fr);
      }
    }

    .eco-card {
      background: var(--color-white);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
      padding: 30px 26px;
      display: flex;
      flex-direction: column;
      gap: 14px;
      transition: all var(--transition-fast);
      position: relative;
    }

    .eco-card:hover {
      transform: translateY(-3px);
      box-shadow: var(--shadow-md);
      border-color: #E2D9CE;
    }

    .club-card {
      background: linear-gradient(180deg, #FFFFFF 0%, #FFF8F0 100%);
      border-color: #FED4B2;
    }

    .club-card:hover {
      border-color: var(--color-primary);
    }

    .eco-icon-wrap {
      width: 48px;
      height: 48px;
      background: var(--color-cream);
      border-radius: var(--radius-md);
      display: grid;
      place-items: center;
      font-size: 24px;
    }

    .club-icon {
      background: var(--color-primary-light);
    }

    .eco-title {
      font-size: 20px;
      font-weight: 800;
      color: var(--color-graphite);
    }

    .club-title {
      color: var(--color-primary-dark);
    }

    .eco-desc {
      font-size: 14px;
      line-height: 1.6;
      color: var(--color-text-muted);
      flex-grow: 1;
    }

    .eco-status {
      font-size: 11.5px;
      font-weight: 700;
      padding: 4px 10px;
      border-radius: var(--radius-pill);
      align-self: flex-start;
      margin-top: 6px;
    }

    .active-status {
      background: #E8F5E9;
      color: #2E7D32;
    }

    .roadmap-status {
      background: #FFF3E0;
      color: #E65100;
    }

    .club-status {
      background: var(--color-primary-light);
      color: var(--color-primary-dark);
    }

    .transparency-note {
      margin-top: 36px;
      padding: 16px 20px;
      background: var(--color-white);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      gap: 14px;
      font-size: 13.5px;
      color: var(--color-text-muted);
      max-width: 860px;
      margin-inline: auto;
    }

    .note-icon {
      font-size: 20px;
      flex-shrink: 0;
    }
  `]
})
export class EcosystemComponent {}
