import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { buttonByText, category, fakeContext, menu, text } from '../../../testing/catalog-fixtures';
import { RestaurantContext, RestaurantRole } from '../restaurant-context.service';
import { ToastService } from '../ui/feedback';
import { CatalogService } from './catalog.service';
import { MenusPage } from './menus.page';

function setup(role: RestaurantRole, menus$ = of([menu(), menu({ id: 'm2', name: 'Bebidas', status: 'PUBLISHED' })])) {
  const catalog = {
    listMenus: vi.fn(() => menus$),
    listCategories: vi.fn(() => of([category()])),
    createMenu: vi.fn(() => of(menu({ id: 'm3', name: 'Jantar' }))),
    updateMenu: vi.fn(() => of(menu({ name: 'Almoço executivo' }))),
    publishMenu: vi.fn(() => of(menu({ status: 'PUBLISHED' }))),
    unpublishMenu: vi.fn(() => of(menu({ id: 'm2', name: 'Bebidas', status: 'DRAFT' }))),
    archiveMenu: vi.fn(() => of(menu({ status: 'ARCHIVED' })))
  };
  TestBed.configureTestingModule({
    imports: [MenusPage],
    providers: [
      provideRouter([]),
      ToastService,
      { provide: RestaurantContext, useValue: fakeContext(role) },
      { provide: CatalogService, useValue: catalog }
    ]
  });
  const fixture = TestBed.createComponent(MenusPage);
  fixture.detectChanges();
  const toast = TestBed.inject(ToastService);
  return { fixture, catalog, toast, el: fixture.nativeElement as HTMLElement };
}

describe('MenusPage', () => {
  it('lista cardápios com estado e quantidade de categorias', () => {
    const { el } = setup('OWNER');
    const cards = el.querySelectorAll('.c-menu-card');
    expect(cards.length).toBe(2);
    expect(text(cards[0].querySelector('.c-status'))).toBe('Rascunho');
    expect(text(cards[1].querySelector('.c-status'))).toBe('Publicado');
    expect(text(cards[0].querySelector('.c-menu-meta'))).toBe('1 categoria');
  });

  it('mostra o estado vazio com ação para o primeiro cardápio', () => {
    const { el } = setup('OWNER', of([]));
    expect(text(el)).toContain('Seu primeiro cardápio começa aqui.');
    expect(buttonByText(el, 'Criar primeiro cardápio')).toBeTruthy();
  });

  it('erro de carregamento não vira lista vazia e permite tentar novamente', () => {
    const { el, catalog, fixture } = setup('OWNER', throwError(() => new HttpErrorResponse({ status: 500 })));
    expect(text(el)).toContain('Não foi possível carregar seus cardápios.');
    expect(text(el)).not.toContain('Seu primeiro cardápio começa aqui.');
    catalog.listMenus.mockReturnValue(of([menu()]));
    buttonByText(el, 'Tentar novamente')!.click();
    fixture.detectChanges();
    expect(el.querySelectorAll('.c-menu-card').length).toBe(1);
  });

  it('cria cardápio sem status no payload e confirma com o servidor', async () => {
    const { el, fixture, catalog, toast } = setup('OWNER');
    buttonByText(el, 'Novo cardápio')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    buttonByText(el, 'Salvar cardápio')!.click();
    fixture.detectChanges();
    expect(text(el)).toContain('Informe o nome do cardápio.');
    expect(catalog.createMenu).not.toHaveBeenCalled();

    const name = el.querySelector<HTMLInputElement>('#mf-name')!;
    name.value = '  Jantar ';
    name.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    buttonByText(el, 'Salvar cardápio')!.click();
    fixture.detectChanges();
    expect(catalog.createMenu).toHaveBeenCalledWith('r1', { name: 'Jantar', description: null, displayOrder: 1 });
    expect(toast.toasts().at(-1)?.text).toBe('Cardápio criado com sucesso.');
    expect(el.querySelectorAll('.c-menu-card').length).toBe(3);
  });

  it('edita cardápio mantendo a ordem (displayOrder nulo)', async () => {
    const { el, fixture, catalog, toast } = setup('OWNER');
    buttonByText(el.querySelector('.c-menu-card')!, 'Editar')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    const name = el.querySelector<HTMLInputElement>('#mf-name')!;
    expect(name.value).toBe('Almoço');
    name.value = 'Almoço executivo';
    name.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    buttonByText(el, 'Salvar cardápio')!.click();
    fixture.detectChanges();
    expect(catalog.updateMenu).toHaveBeenCalledWith('r1', 'm1', { name: 'Almoço executivo', description: 'Servido ao meio-dia', displayOrder: null });
    expect(toast.toasts().at(-1)?.text).toBe('Alterações salvas com sucesso.');
  });

  it('publica, despublica e arquiva com confirmação', () => {
    const { el, fixture, catalog } = setup('OWNER');
    const [draft, published] = Array.from(el.querySelectorAll('.c-menu-card'));
    buttonByText(draft, 'Publicar')!.click();
    fixture.detectChanges();
    expect(catalog.publishMenu).toHaveBeenCalledWith('r1', 'm1');
    buttonByText(published, 'Despublicar')!.click();
    fixture.detectChanges();
    expect(catalog.unpublishMenu).toHaveBeenCalledWith('r1', 'm2');

    buttonByText(el.querySelector('.c-menu-card')!, 'Arquivar')!.click();
    fixture.detectChanges();
    expect(catalog.archiveMenu).not.toHaveBeenCalled();
    const dialog = [...el.querySelectorAll('dialog')].find(d => d.hasAttribute('open'))!;
    expect(text(dialog)).toContain('Arquivar este cardápio?');
    buttonByText(dialog, 'Arquivar')!.click();
    fixture.detectChanges();
    expect(catalog.archiveMenu).toHaveBeenCalledWith('r1', 'm1');
    expect(text(el.querySelector('.c-archived summary'))).toBe('Arquivados (1)');
  });

  it('publicação recusada (409) mostra a regra e mantém o estado', () => {
    const { el, fixture, catalog, toast } = setup('OWNER');
    catalog.publishMenu.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    buttonByText(el.querySelector('.c-menu-card')!, 'Publicar')!.click();
    fixture.detectChanges();
    expect(toast.toasts().at(-1)?.text).toContain('ao menos uma categoria ativa');
    expect(text(el.querySelector('.c-menu-card .c-status'))).toBe('Rascunho');
  });

  it('MANAGER tem as mesmas ações de gestão do OWNER', () => {
    const { el } = setup('MANAGER');
    expect(buttonByText(el, 'Novo cardápio')).toBeTruthy();
    expect(buttonByText(el.querySelector('.c-menu-card')!, 'Publicar')).toBeTruthy();
  });

  it('STAFF consulta sem controles de criação, edição, publicação ou arquivamento', () => {
    const { el } = setup('STAFF');
    expect(text(el)).toContain('acesso de Equipe');
    expect(buttonByText(el, 'Novo cardápio')).toBeUndefined();
    const card = el.querySelector('.c-menu-card')!;
    expect(card.querySelectorAll('button').length).toBe(0);
    expect(text(card.querySelector('a.d-btn'))).toBe('Consultar');
  });
});
