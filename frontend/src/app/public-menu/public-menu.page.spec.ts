import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { text } from '../../testing/catalog-fixtures';
import { PublicMenuPage } from './public-menu.page';
import { PublicMenuService, PublicRestaurantMenu } from './public-menu.service';

/** Resposta como o backend entrega: já sem cardápios em rascunho, categorias inativas, arquivados e adicionais inativos. */
const DATA: PublicRestaurantMenu = {
  id: 'r1', name: 'Cantina Bella Vista', slug: 'cantina', description: 'Massas artesanais', phone: '(11) 3333-4444',
  whatsapp: '(11) 99999-0000', instagram: '@cantina', website: null, category: 'Italiana',
  menus: [
    {
      id: 'm1', name: 'Almoço', description: 'Ao meio-dia', displayOrder: 0, categories: [
        { id: 'c1', name: 'Massas', description: null, displayOrder: 0, products: [
          { id: 'p1', name: 'Carbonara', description: 'Guanciale e pecorino', priceInCents: 5890, availability: 'AVAILABLE', displayOrder: 0, ingredients: ['Spaghetti', 'Guanciale'], addons: [{ name: 'Pecorino extra', priceDeltaInCents: 600 }], imageUrl: null },
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
  TestBed.configureTestingModule({ imports: [PublicMenuPage], providers: [provideRouter([]), { provide: PublicMenuService, useValue: api }] });
  const fixture = TestBed.createComponent(PublicMenuPage);
  fixture.componentRef.setInput('slug', 'cantina');
  fixture.detectChanges();
  return { fixture, api, el: fixture.nativeElement as HTMLElement };
}

describe('PublicMenuPage', () => {
  it('consulta pelo slug e mostra o restaurante publicado', () => {
    const { el, api } = setup();
    expect(api.getRestaurantMenu).toHaveBeenCalledWith('cantina');
    expect(text(el.querySelector('h1'))).toBe('Cantina Bella Vista');
    expect(text(el.querySelector('.pm-kicker'))).toBe('Italiana');
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
    expect([...sheet.querySelectorAll('button')].map(b => b.getAttribute('aria-label'))).toEqual(['Fechar detalhes']);
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
});
