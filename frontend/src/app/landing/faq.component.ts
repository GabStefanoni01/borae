import { Component, signal } from '@angular/core';
import { IconComponent } from '../shared/icon.component';
import { RevealDirective } from '../shared/reveal.directive';

interface FaqItem {
  id: string;
  question: string;
  answer: string;
}

@Component({
  selector: 'app-faq',
  standalone: true,
  imports: [IconComponent, RevealDirective],
  template: `
    <section id="faq" class="faq-section" aria-labelledby="faq-title">
      <div class="container">
        <!-- Cabeçalho -->
        <div class="section-header" boReveal>
          <div class="eyebrow-tag">DÚVIDAS & TRANSPARÊNCIA</div>
          <h2 id="faq-title" class="section-title">Perguntas frequentes</h2>
          <p class="section-lead">
            Tire suas dúvidas sobre o propósito do Boraê, o estágio atual do desenvolvimento 
            e os planos para o futuro da plataforma.
          </p>
        </div>

        <!-- Accordion Acessível -->
        <div class="accordion-wrapper" role="presentation">
          @for (item of faqs; track item.id; let idx = $index) {
            <div class="faq-card" [class.open]="isOpen(item.id)" [boReveal]="idx * 60">
              <button 
                type="button" 
                class="faq-trigger" 
                [id]="'faq-trigger-' + item.id"
                [attr.aria-expanded]="isOpen(item.id)"
                [attr.aria-controls]="'faq-content-' + item.id"
                (click)="toggle(item.id)"
              >
                <span class="faq-question">{{ item.question }}</span>
                <span class="faq-icon" aria-hidden="true" [class.rotated]="isOpen(item.id)"><bo-icon name="plus" [size]="16" [strokeWidth]="2.5" /></span>
              </button>

              <div 
                [id]="'faq-content-' + item.id"
                class="faq-content" 
                role="region"
                [attr.aria-labelledby]="'faq-trigger-' + item.id"
                [hidden]="!isOpen(item.id)"
              >
                <div class="faq-answer-inner">
                  <p>{{ item.answer }}</p>
                </div>
              </div>
            </div>
          }
        </div>
      </div>
    </section>
  `,
  styles: [`
    .faq-section {
      padding: clamp(64px, 9vw, 110px) 0;
      background-color: var(--color-surface);
      border-top: 1px solid var(--color-border);
      border-bottom: 1px solid var(--color-border);
    }

    .section-header {
      text-align: center;
      max-width: 720px;
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

    .accordion-wrapper {
      max-width: 820px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .faq-card {
      background: var(--color-white);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      transition: all var(--transition-fast);
      overflow: hidden;
    }

    .faq-card.open {
      border-color: var(--color-primary);
      box-shadow: var(--shadow-sm);
    }

    .faq-trigger {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 20px 24px;
      background: transparent;
      border: 0;
      text-align: left;
      font: inherit;
      cursor: pointer;
    }

    .faq-question {
      font-size: 16.5px;
      font-weight: 700;
      color: var(--color-graphite);
      line-height: 1.35;
      font-family: var(--font-family-headings);
    }

    .faq-icon {
      font-size: 20px;
      font-weight: 400;
      color: var(--color-primary);
      width: 28px;
      height: 28px;
      border-radius: var(--radius-pill);
      background: var(--color-primary-light);
      display: grid;
      place-items: center;
      flex-shrink: 0;
      transition: transform var(--transition-fast);
    }

    .faq-icon.rotated {
      transform: rotate(45deg);
      background: var(--color-primary);
      color: var(--color-white);
    }

    .faq-content {
      border-top: 1px solid var(--color-surface);
    }

    .faq-answer-inner {
      padding: 16px 24px 22px;
    }

    .faq-answer-inner p {
      font-size: 15px;
      line-height: 1.7;
      color: var(--color-text-muted);
    }
  `]
})
export class FaqComponent {
  openItem = signal<string | null>('1');

  faqs: FaqItem[] = [
    {
      id: '1',
      question: 'O que é o Boraê?',
      answer: 'O Boraê é uma plataforma gastronômica em desenvolvimento que busca conectar restaurantes e consumidores por meio de experiências digitais, descoberta de estabelecimentos e tecnologia.'
    },
    {
      id: '2',
      question: 'O Boraê é um aplicativo de delivery?',
      answer: 'A proposta do Boraê vai além do delivery tradicional. O projeto busca conectar a descoberta gastronômica, os cardápios digitais, experiências interativas e ferramentas de relacionamento entre consumidores e estabelecimentos.'
    },
    {
      id: '3',
      question: 'Como um restaurante poderá participar?',
      answer: 'A plataforma possui uma estrutura inicial de cadastro e gestão de restaurantes. O processo de participação e as condições comerciais devem ser apresentados conforme a operação de parceiros for oficialmente disponibilizada.'
    },
    {
      id: '4',
      question: 'O Boraê terá cardápio digital?',
      answer: 'Sim. O cardápio digital faz parte do roadmap do Boraê e está previsto para permitir uma apresentação mais completa dos estabelecimentos e seus pratos.'
    },
    {
      id: '5',
      question: 'Como funcionará a visualização 3D e realidade aumentada?',
      answer: 'A proposta é permitir que determinados pratos tenham modelos tridimensionais e experiências de realidade aumentada em dispositivos compatíveis. Essa funcionalidade ainda faz parte do desenvolvimento futuro.'
    },
    {
      id: '6',
      question: 'O que é o Boraê Club?',
      answer: 'É a proposta de um ecossistema de benefícios compartilhados entre estabelecimentos parceiros, separado dos programas de fidelidade individuais.'
    },
    {
      id: '7',
      question: 'O consumidor precisará pagar para utilizar a plataforma?',
      answer: 'As condições comerciais e os eventuais modelos de assinatura ainda dependem da definição oficial do produto. Não anuncie planos ou preços sem aprovação.'
    },
    {
      id: '8',
      question: 'Quais funcionalidades já estão disponíveis?',
      answer: 'O Boraê está em desenvolvimento incremental. Atualmente, sua fundação tecnológica contempla cadastro, autenticação e estrutura inicial de gestão de restaurantes. Os módulos gastronômicos estão sendo desenvolvidos conforme o roadmap.'
    }
  ];

  isOpen(id: string): boolean {
    return this.openItem() === id;
  }

  toggle(id: string): void {
    this.openItem.update(current => (current === id ? null : id));
  }
}
