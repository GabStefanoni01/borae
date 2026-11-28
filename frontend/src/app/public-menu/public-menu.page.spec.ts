import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { text } from '../../testing/catalog-fixtures';
import { MODEL_VIEWER_LOADER } from '../shared/model-3d-viewer.component';
import { PublicAssistantService } from './public-assistant.service';
import { PublicMenuPage } from './public-menu.page';
import { PublicMenuService, PublicRestaurantMenu } from './public-menu.service';

/** Resposta como o backend entrega: já sem cardápios em rascunho, categorias inativas, arquivados e adicionais inativos. */
const DATA: PublicRestaurantMenu = {
  id: 'r1', name: 'Cantina Bella Vista', slug: 'cantina', description: 'Massas artesanais', phone: '(11) 3333-4444',
  whatsapp: '(11) 99999-0000', instagram: '@cantina', website: null, category: 'Italiana',
  address: 'Rua das Flores, 123 - São Paulo/SP', logoUrl: 'https://ik.imagekit.io/borae/cantina-logo.webp',
  menus: [
    {
      id: 'm1', name: 'Almoço', description: 'Ao meio-dia', displayOrder: 0, categories: [
        { id: 'c1', name: 'Massas', description: null, displayOrder: 0, products: [
          { id: 'p1', name: 'Carbonara', description: 'Guanciale e pecorino', priceInCents: 5890, availability: 'AVAILABLE', displayOrder: 0, ingredients: ['Spaghetti', 'Guanciale'], addons: [{ name: 'Pecorino extra', priceDeltaInCents: 600 }], imageUrl: 'https://ik.imagekit.io/borae/carbonara.webp',
            model3d: { modelUrl: 'https://ik.imagekit.io/borae/carbonara.glb', contentType: 'model/gltf-binary', widthMm: 280, heightMm: 70, depthMm: 280 } },
          { id: 'p2', name: 'Ragu', description: null, priceInCents: 6490, availability: 'UNAVAILABLE', displayOrder: 1, ingredients: [], addons: [], imageUrl: null }
        ] },
        { id: 'c2', name: 'Sem produtos', description: null, displayOrder: 1, products: [] }
      ]
    },
    { id: 'm2', name: 'Vinhos', description: null, displayOrder: 1, categories: [
      { id: 'c3', name: 'Tintos', description: null, displayOrder: 0, products: [
        { id: 'p3', name: 'Chianti', description: null, priceInCents: 3900, availability: 'AVAILABLE', displayOrder: 0, ingredients: [], addons: [], imageUrl: null }
      ] }
    ] }
  ]
};

function setup(response = of(DATA)) {
  const api = { getRestaurantMenu: vi.fn(() => response) };
  const assistantApi = {
    ensureSession: vi.fn(() => of(void 0)),
    interact: vi.fn(() => of({
      answer: 'Recomendo o Carbonara.',
      recommendations: [
        {
          productId: 'p1', name: 'Carbonara', priceInCents: 5890, availability: 'AVAILABLE' as const,
          imageUrl: 'https://ik.imagekit.io/borae/carbonara.webp', ingredients: ['Spaghetti', 'Guanciale'],
          reason: 'Massa clássica disponível.'
        }
      ],
      quota: { remainingConsumer: 19, remainingRestaurant: 99, resetsAt: '2026-11-20T00:00:00-03:00' },
      fallback: false,
      fallbackCode: null
    })),
    trackRecommendationClick: vi.fn(() => of(void 0))
  };
  const loader = vi.fn(() => Promise.resolve());
  TestBed.configureTestingModule({
    imports: [PublicMenuPage],
    providers: [
      provideRouter([]),
      { provide: PublicMenuService, useValue: api },
      { provide: PublicAssistantService, useValue: assistantApi },
      { provide: MODEL_VIEWER_LOADER, useValue: loader }
    ]
  });
  const fixture = TestBed.createComponent(PublicMenuPage);
  fixture.componentRef.setInput('slug', 'cantina');
  fixture.detectChanges();
  return { fixture, api, assistantApi, loader, el: fixture.nativeElement as HTMLElement };
}

describe('PublicMenuPage', () => {
  it('consulta pelo slug e mostra o restaurante publicado', () => {
    const { el, api } = setup();
    expect(api.getRestaurantMenu).toHaveBeenCalledWith('cantina');
    expect(text(el.querySelector('h1'))).toBe('Cantina Bella Vista');
    expect(text(el.querySelector('.pm-kicker'))).toBe('Italiana');
    expect(el.querySelector<HTMLImageElement>('.pm-logo')?.src).toBe('https://ik.imagekit.io/borae/cantina-logo.webp');
    const address = [...el.querySelectorAll<HTMLAnchorElement>('.pm-contacts a')].find(a => text(a).includes('Rua das Flores'));
    expect(address?.href).toContain('google.com/maps/search/');
    expect(text(address)).toContain('Rua das Flores, 123 - São Paulo/SP');
    expect([...el.querySelectorAll('.pm-contacts a')].map(a => a.getAttribute('href'))).toEqual(['https://wa.me/5511999990000', 'tel:1133334444', 'https://instagram.com/cantina']);
  });

  it('permite escolher entre vários cardápios publicados', () => {
    const { el, fixture } = setup();
    const tabs = el.querySelectorAll<HTMLButtonElement>('.pm-menu-tab');
    expect([...tabs].map(t => text(t))).toEqual(['Almoço', 'Vinhos']);
    expect(text(el.querySelector('.pm-menu-head h2'))).toBe('Almoço');
    tabs[1].click();
    fixture.detectChanges();
    expect(text(el.querySelector('.pm-menu-head h2'))).toBe('Vinhos');
    expect(text(el)).toContain('Chianti');
  });

  it('mostra categorias com produtos, preço em BRL e produto indisponível sinalizado', () => {
    const { el } = setup();
    expect([...el.querySelectorAll('.pm-section-title')].map(h => text(h))).toEqual(['Massas']);
    const items = el.querySelectorAll('.pm-item');
    expect(items.length).toBe(2);
    expect(text(items[0])).toContain('R$ 58,90');
    expect(items[1].classList.contains('off')).toBe(true);
    expect(text(items[1])).toContain('Indisponível no momento');
    expect(items[1].getAttribute('aria-label')).toContain('indisponível no momento');
    expect(el.querySelector('button[class*="pedido"], .pm-cart')).toBeNull();
  });

  it('detalhe do produto mostra ingredientes e adicionais com acréscimo, sem ações de compra', () => {
    const { el, fixture } = setup();
    el.querySelector<HTMLButtonElement>('.pm-item')!.click();
    fixture.detectChanges();
    const sheet = el.querySelector('.pm-sheet')!;
    expect(sheet.hasAttribute('open')).toBe(true);
    expect(text(sheet)).toContain('Spaghetti, Guanciale');
    expect(text(sheet)).toContain('Ingredientes informados pelo restaurante.');
    expect(text(sheet.querySelector('.mp-addons li'))).toMatch(/^Pecorino extra\s*\+ R\$ 6,00$/);
    const buttons = [...sheet.querySelectorAll('button')].map(b => b.getAttribute('aria-label') ?? text(b));
    expect(buttons).toEqual(['Fechar detalhes', 'Ver em 3D']);
    expect(buttons.some(b => /pedi|comprar|carrinho|adicionar/i.test(b))).toBe(false);
  });

  it('restaurante inexistente ou não publicado (404)', () => {
    const { el } = setup(throwError(() => new HttpErrorResponse({ status: 404 })));
    expect(text(el)).toContain('Não foi possível encontrar este restaurante.');
    expect(text(el)).toContain('A página pode não estar disponível no momento ou o endereço pode estar incorreto.');
  });

  it('sem cardápios publicados', () => {
    const { el } = setup(of({ ...DATA, menus: [] }));
    expect(text(el)).toContain('Este restaurante ainda não possui cardápios disponíveis.');
  });

  it('falha de conexão permite tentar novamente', () => {
    const { el, fixture, api } = setup(throwError(() => new HttpErrorResponse({ status: 0 })));
    expect(text(el)).toContain('Não foi possível carregar o cardápio.');
    api.getRestaurantMenu.mockReturnValue(of(DATA));
    [...el.querySelectorAll('button')].find(b => text(b).includes('Tentar novamente'))!.click();
    fixture.detectChanges();
    expect(text(el.querySelector('h1'))).toBe('Cantina Bella Vista');
  });

  it('produto com modelo publicado: selo 3D na lista e visualizador só sob demanda', async () => {
    const { el, fixture, loader } = setup();
    const [withModel, withoutModel] = Array.from(el.querySelectorAll<HTMLButtonElement>('.pm-item'));
    expect(text(withModel.querySelector('.pm-item-3d'))).toBe('3D');
    expect(withModel.getAttribute('aria-label')).toContain('disponível em 3D');
    expect(withoutModel.querySelector('.pm-item-3d')).toBeNull();

    withModel.click();
    fixture.detectChanges();
    expect(loader).not.toHaveBeenCalled();
    expect(el.querySelector('.pm-sheet model-viewer')).toBeNull();
    [...el.querySelectorAll<HTMLButtonElement>('.pm-sheet button')].find(b => text(b) === 'Ver em 3D')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(loader).toHaveBeenCalledTimes(1);
    const mv = el.querySelector('.pm-sheet model-viewer')!;
    expect(mv.getAttribute('src')).toContain('carbonara.glb');
    expect(mv.getAttribute('poster')).toContain('carbonara.webp');
    expect(mv.hasAttribute('ar')).toBe(true);

    mv.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(el.querySelector<HTMLImageElement>('.pm-sheet .mv-fallback img')!.src).toContain('carbonara.webp');
  });

  it('produto sem modelo não oferece 3D', () => {
    const { el, fixture } = setup();
    el.querySelectorAll<HTMLButtonElement>('.pm-item')[1].click();
    fixture.detectChanges();
    expect([...el.querySelectorAll('.pm-sheet button')].some(b => text(b) === 'Ver em 3D')).toBe(false);
  });

  it('integra o assistente gastronômico ao cardápio público e abre detalhes ao clicar na recomendação', () => {
    const { el, fixture, assistantApi } = setup();
    const fab = el.querySelector<HTMLButtonElement>('.asst-fab')!;
    expect(fab).not.toBeNull();

    fab.click();
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('.asst-chip')!.click();
    fixture.detectChanges();

    expect(assistantApi.interact).toHaveBeenCalledTimes(1);
    el.querySelector<HTMLButtonElement>('.asst-rec')!.click();
    fixture.detectChanges();

    expect(assistantApi.trackRecommendationClick).toHaveBeenCalledWith('cantina', 'p1');
    const sheet = el.querySelector('.pm-sheet')!;
    expect(sheet.hasAttribute('open')).toBe(true);
    expect(text(sheet)).toContain('Carbonara');
  });

  it('oculta o assistente quando o restaurante não tem cardápios publicados', () => {
    const { el } = setup(of({ ...DATA, menus: [] }));
    expect(el.querySelector('.asst-fab')).toBeNull();
  });
});
