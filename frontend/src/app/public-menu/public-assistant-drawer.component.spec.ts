import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { text } from '../../testing/catalog-fixtures';
import { PublicAssistantDrawerComponent } from './public-assistant-drawer.component';
import { AssistantInteractionResponse, PublicAssistantService } from './public-assistant.service';
import { PublicProduct, PublicRestaurantMenu } from './public-menu.service';

const RESTAURANT: PublicRestaurantMenu = {
  id: 'r1',
  name: 'Cantina Bella Vista',
  slug: 'cantina',
  description: 'Massas artesanais',
  phone: null,
  whatsapp: null,
  instagram: null,
  website: null,
  category: 'Italiana',
  menus: [
    {
      id: 'm1',
      name: 'Almoço',
      description: null,
      displayOrder: 0,
      categories: [
        {
          id: 'c1',
          name: 'Massas',
          description: null,
          displayOrder: 0,
          products: [
            {
              id: 'p1',
              name: 'Carbonara',
              description: 'Guanciale e pecorino',
              priceInCents: 5890,
              availability: 'AVAILABLE',
              displayOrder: 0,
              ingredients: ['Spaghetti', 'Guanciale'],
              addons: [{ name: 'Pecorino extra', priceDeltaInCents: 600 }],
              imageUrl: 'https://ik.imagekit.io/borae/carbonara.webp'
            }
          ]
        }
      ]
    }
  ]
};

function setup(interactImpl = vi.fn(() => of<AssistantInteractionResponse>({
  answer: 'Recomendo o Carbonara da casa.',
  recommendations: [
    {
      productId: 'p1',
      name: 'Carbonara',
      priceInCents: 5890,
      availability: 'AVAILABLE',
      imageUrl: 'https://ik.imagekit.io/borae/carbonara.webp',
      ingredients: ['Spaghetti', 'Guanciale'],
      reason: 'Clássico italiano com massa fresca e pecorino.'
    }
  ],
  quota: { remainingConsumer: 19, remainingRestaurant: 99, resetsAt: '2026-11-20T00:00:00-03:00' },
  fallback: false,
  fallbackCode: null
}))) {
  const api = {
    ensureSession: vi.fn(() => of(void 0)),
    interact: interactImpl,
    trackRecommendationClick: vi.fn(() => of(void 0))
  };

  TestBed.configureTestingModule({
    imports: [PublicAssistantDrawerComponent],
    providers: [{ provide: PublicAssistantService, useValue: api }]
  });

  const fixture = TestBed.createComponent(PublicAssistantDrawerComponent);
  fixture.componentRef.setInput('restaurant', RESTAURANT);
  fixture.componentRef.setInput('selectedMenuId', 'm1');
  fixture.detectChanges();

  const selected: PublicProduct[] = [];
  fixture.componentInstance.selectProduct.subscribe(p => selected.push(p));

  return { fixture, api, selected, el: fixture.nativeElement as HTMLElement };
}

describe('PublicAssistantDrawerComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('abre o painel, inicia sessão anônima, mostra saudação com nome do restaurante e fecha pelo botão', () => {
    const { el, fixture, api } = setup();
    const fab = el.querySelector<HTMLButtonElement>('.asst-fab')!;
    expect(text(fab)).toContain('Assistente');
    expect(el.querySelector('.asst-panel')).toBeNull();

    fab.click();
    fixture.detectChanges();

    expect(api.ensureSession).toHaveBeenCalledTimes(1);
    const panel = el.querySelector('.asst-panel')!;
    expect(panel).not.toBeNull();
    expect(text(panel)).toContain('Cantina Bella Vista');
    expect(el.querySelectorAll('.asst-chip').length).toBeGreaterThanOrEqual(3);

    el.querySelector<HTMLButtonElement>('.asst-close')!.click();
    fixture.detectChanges();
    expect(el.querySelector('.asst-panel')).toBeNull();
  });

  it('envia sugestão rápida, exibe cards estruturados, rastreia clique e abre produto completo sem persistir conversa', () => {
    const { el, fixture, api, selected } = setup();
    el.querySelector<HTMLButtonElement>('.asst-fab')!.click();
    fixture.detectChanges();

    const chip = el.querySelector<HTMLButtonElement>('.asst-chip')!;
    chip.click();
    fixture.detectChanges();

    expect(api.interact).toHaveBeenCalledWith('cantina', {
      message: 'Qual prato você recomenda hoje?',
      menuId: 'm1',
      filters: null
    });

    expect(text(el)).toContain('Recomendo o Carbonara da casa.');
    expect(text(el)).toContain('R$ 58,90');
    expect(text(el)).toContain('19 perguntas restantes hoje');

    el.querySelector<HTMLButtonElement>('.asst-rec')!.click();
    fixture.detectChanges();

    expect(api.trackRecommendationClick).toHaveBeenCalledWith('cantina', 'p1');
    expect(selected.length).toBe(1);
    expect(selected[0].id).toBe('p1');
    expect(selected[0].addons.length).toBe(1);

    // D-11: nenhuma conversa salva em localStorage ou sessionStorage
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('evita envio duplicado enquanto uma consulta está em andamento e exibe indicador de carregamento', () => {
    const pending$ = new Subject<AssistantInteractionResponse>();
    const { el, fixture, api } = setup(vi.fn(() => pending$.asObservable()));

    el.querySelector<HTMLButtonElement>('.asst-fab')!.click();
    fixture.detectChanges();

    const input = el.querySelector<HTMLInputElement>('.asst-input')!;
    input.value = 'Quero algo leve';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const form = el.querySelector<HTMLFormElement>('.asst-form')!;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(el.querySelector('.asst-typing')).not.toBeNull();
    expect(el.querySelector<HTMLButtonElement>('.asst-send')!.disabled).toBe(true);

    // Tentativa duplicada durante loading é ignorada
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(api.interact).toHaveBeenCalledTimes(1);

    pending$.next({
      answer: 'Boa pedida!',
      recommendations: [],
      quota: null,
      fallback: false,
      fallbackCode: null
    });
    pending$.complete();
    fixture.detectChanges();

    expect(el.querySelector('.asst-typing')).toBeNull();
    expect(text(el)).toContain('Boa pedida!');
  });

  it('envia filtros opcionais de orçamento e ingredientes excluídos', () => {
    const { el, fixture, api } = setup();
    el.querySelector<HTMLButtonElement>('.asst-fab')!.click();
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.asst-filter-toggle')!.click();
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.asst-pill')!.click();
    const excludeInput = el.querySelector<HTMLInputElement>('#asst-exclude')!;
    excludeInput.value = 'amendoim, pimenta';
    excludeInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.asst-chip')!.click();
    fixture.detectChanges();

    expect(api.interact).toHaveBeenCalledWith('cantina', {
      message: 'Qual prato você recomenda hoje?',
      menuId: 'm1',
      filters: {
        maxPriceInCents: 3500,
        excludedIngredients: ['amendoim', 'pimenta']
      }
    });
  });

  it('trata cota diária esgotada (429 DAILY_QUOTA_EXCEEDED) desabilitando o envio com orientação clara', () => {
    const { el, fixture } = setup(vi.fn(() => throwError(() => new HttpErrorResponse({
      status: 429,
      error: { code: 'DAILY_QUOTA_EXCEEDED', scope: 'CONSUMER', resetsAt: '2026-11-20T00:00:00-03:00' }
    }))));

    el.querySelector<HTMLButtonElement>('.asst-fab')!.click();
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.asst-chip')!.click();
    fixture.detectChanges();

    expect(text(el)).toContain('limite diário');
    expect(el.querySelector<HTMLInputElement>('.asst-input')!.disabled).toBe(true);
    expect(el.querySelector<HTMLButtonElement>('.asst-send')!.disabled).toBe(true);
  });

  it('trata rate limit (429 RATE_LIMIT_EXCEEDED) e falha de rede permitindo tentar novamente', () => {
    const interact = vi.fn()
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({
        status: 429,
        error: { code: 'RATE_LIMIT_EXCEEDED', retryAfterSeconds: 30 }
      })))
      .mockReturnValueOnce(of<AssistantInteractionResponse>({
        answer: 'Tudo certo agora!',
        recommendations: [],
        quota: null,
        fallback: false,
        fallbackCode: null
      }));

    const { el, fixture } = setup(interact);
    el.querySelector<HTMLButtonElement>('.asst-fab')!.click();
    fixture.detectChanges();

    el.querySelector<HTMLButtonElement>('.asst-chip')!.click();
    fixture.detectChanges();

    expect(text(el)).toContain('30 segundos');
    const retryBtn = el.querySelector<HTMLButtonElement>('.asst-retry')!;
    expect(retryBtn).not.toBeNull();

    retryBtn.click();
    fixture.detectChanges();

    expect(interact).toHaveBeenCalledTimes(2);
    expect(text(el)).toContain('Tudo certo agora!');
  });
});
