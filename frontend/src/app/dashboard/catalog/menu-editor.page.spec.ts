import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { buttonByText, category, fakeContext, menu, product, text } from '../../../testing/catalog-fixtures';
import { RestaurantContext, RestaurantRole } from '../restaurant-context.service';
import { ToastService } from '../ui/feedback';
import { Category, Menu, Product } from './catalog.models';
import { CatalogService } from './catalog.service';
import { MenuEditorPage } from './menu-editor.page';

const CATS: Category[] = [category(), category({ id: 'c2', name: 'Massas', displayOrder: 1 }), category({ id: 'c3', name: 'Sobremesas', displayOrder: 2, active: false })];
const PRODUCTS: Product[] = [
  product(),
  product({ id: 'p2', name: 'Burrata', displayOrder: 1, availability: 'UNAVAILABLE' }),
  product({ id: 'p3', name: 'Antigo', displayOrder: 2, lifecycleStatus: 'ARCHIVED' }),
  product({ id: 'p4', name: 'Carbonara', categoryId: 'c2' })
];

function setup(role: RestaurantRole, m: Menu = menu({ status: 'PUBLISHED' })) {
  const catalog = {
    getMenu: vi.fn(() => of(m)),
    listCategories: vi.fn(() => of(CATS)),
    listProducts: vi.fn(() => of(PRODUCTS)),
    createCategory: vi.fn(() => of(category({ id: 'c9', name: 'Bebidas', displayOrder: 3 }))),
    updateCategory: vi.fn((_r: string, _m: string, id: string, body: Partial<Category>) => of({ ...CATS.find(c => c.id === id)!, ...body })),
    reorderCategories: vi.fn((_r: string, _m: string, ids: string[]) => of(ids.map((id, i) => ({ ...CATS.find(c => c.id === id)!, displayOrder: i })))),
    changeAvailability: vi.fn((_r: string, _m: string, id: string, availability: Product['availability']) => of({ ...PRODUCTS.find(p => p.id === id)!, availability })),
    archiveProduct: vi.fn((_r: string, _m: string, id: string) => of({ ...PRODUCTS.find(p => p.id === id)!, lifecycleStatus: 'ARCHIVED' as const })),
    restoreProduct: vi.fn((_r: string, _m: string, id: string) => of({ ...PRODUCTS.find(p => p.id === id)!, lifecycleStatus: 'ACTIVE' as const })),
    reorderProducts: vi.fn((_r: string, _m: string, _c: string, ids: string[]) => of(ids.map((id, i) => ({ ...PRODUCTS.find(p => p.id === id)!, displayOrder: i })))),
    updateMenu: vi.fn(), publishMenu: vi.fn(), unpublishMenu: vi.fn(), archiveMenu: vi.fn()
  };
  TestBed.configureTestingModule({
    imports: [MenuEditorPage],
    providers: [provideRouter([]), ToastService, { provide: RestaurantContext, useValue: fakeContext(role) }, { provide: CatalogService, useValue: catalog }]
  });
  const fixture = TestBed.createComponent(MenuEditorPage);
  fixture.componentRef.setInput('menuId', 'm1');
  fixture.detectChanges();
  return { fixture, catalog, toast: TestBed.inject(ToastService), el: fixture.nativeElement as HTMLElement };
}

const productNames = (el: HTMLElement) => [...el.querySelectorAll('.c-prod-name strong')].map(s => text(s));
const categoryNames = (el: HTMLElement) => [...el.querySelectorAll('.c-cat strong')].map(s => text(s));

describe('MenuEditorPage', () => {
  it('mostra cardápio, categorias na ordem e produtos da categoria (arquivados ocultos)', () => {
    const { el } = setup('OWNER');
    expect(text(el.querySelector('.c-hero h1'))).toBe('Almoço');
    expect(categoryNames(el)).toEqual(['Entradas', 'Massas', 'Sobremesas']);
    expect(text(el.querySelectorAll('.c-cat small')[2])).toContain('Inativa');
    expect(productNames(el)).toEqual(['Bruschetta', 'Burrata']);
    expect(text(el.querySelector('.c-tag-off'))).toBe('Indisponível');
    expect(text(el.querySelector('.c-prod-price'))).toBe('R$ 28,90');
  });

  it('cria categoria e trata nome duplicado (409)', async () => {
    const { el, fixture, catalog, toast } = setup('OWNER');
    buttonByText(el, 'Nova categoria')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    const name = el.querySelector<HTMLInputElement>('#cf-name')!;
    name.value = 'entradas';
    name.dispatchEvent(new Event('input'));
    catalog.createCategory.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 409 })));
    buttonByText(el, 'Salvar categoria')!.click();
    fixture.detectChanges();
    expect(text(el.querySelector('.d-panel-dialog .d-alert-error'))).toBe('A categoria já existe neste cardápio. Escolha outro nome.');

    name.value = 'Bebidas';
    name.dispatchEvent(new Event('input'));
    buttonByText(el, 'Salvar categoria')!.click();
    fixture.detectChanges();
    expect(catalog.createCategory).toHaveBeenLastCalledWith('r1', 'm1', { name: 'Bebidas', description: null, displayOrder: 3 });
    expect(categoryNames(el)).toContain('Bebidas');
    expect(toast.toasts().at(-1)?.text).toBe('Categoria salva com sucesso.');
  });

  it('edita e desativa categoria enviando todos os campos', () => {
    const { el, fixture, catalog } = setup('OWNER');
    buttonByText(el, 'Desativar categoria')!.click();
    fixture.detectChanges();
    expect(catalog.updateCategory).toHaveBeenCalledWith('r1', 'm1', 'c1', { name: 'Entradas', description: null, displayOrder: 0, active: false });
    expect(text(el.querySelector('.c-cat small'))).toContain('Inativa');
    expect(buttonByText(el, 'Ativar categoria')).toBeTruthy();
  });

  it('reordena categorias com a lista completa e restaura a ordem do servidor em caso de erro', () => {
    const { el, fixture, catalog, toast } = setup('OWNER');
    el.querySelector<HTMLButtonElement>('[aria-label="Mover Massas para cima"]')!.click();
    fixture.detectChanges();
    expect(catalog.reorderCategories).toHaveBeenCalledWith('r1', 'm1', ['c2', 'c1', 'c3']);
    expect(categoryNames(el)).toEqual(['Massas', 'Entradas', 'Sobremesas']);

    catalog.reorderCategories.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 400 })));
    el.querySelector<HTMLButtonElement>('[aria-label="Mover Sobremesas para cima"]')!.click();
    fixture.detectChanges();
    expect(toast.toasts().at(-1)?.kind).toBe('error');
    expect(categoryNames(el)).toEqual(['Entradas', 'Massas', 'Sobremesas']);
  });

  it('altera disponibilidade somente após a resposta do servidor', () => {
    const { el, fixture, catalog } = setup('OWNER');
    const toggle = el.querySelectorAll<HTMLInputElement>('input[role=switch]')[1];
    expect(toggle.checked).toBe(false);
    toggle.click();
    fixture.detectChanges();
    expect(catalog.changeAvailability).toHaveBeenCalledWith('r1', 'm1', 'p2', 'AVAILABLE');
    expect(el.querySelectorAll<HTMLInputElement>('input[role=switch]')[1].checked).toBe(true);

    catalog.changeAvailability.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 409 })));
    const first = el.querySelectorAll<HTMLInputElement>('input[role=switch]')[0];
    first.click();
    fixture.detectChanges();
    expect(el.querySelectorAll<HTMLInputElement>('input[role=switch]')[0].checked).toBe(true);
  });

  it('reordena produtos enviando todos os IDs da categoria, inclusive arquivados', () => {
    const { el, fixture, catalog } = setup('OWNER');
    el.querySelector<HTMLButtonElement>('[aria-label="Mover Burrata para cima"]')!.click();
    fixture.detectChanges();
    expect(catalog.reorderProducts).toHaveBeenCalledWith('r1', 'm1', 'c1', ['p2', 'p1', 'p3']);
    expect(productNames(el)).toEqual(['Burrata', 'Bruschetta']);
  });

  it('arquiva com confirmação e restaura produto', () => {
    const { el, fixture, catalog } = setup('OWNER');
    el.querySelector<HTMLButtonElement>('[aria-label="Arquivar Bruschetta"]')!.click();
    fixture.detectChanges();
    const dialog = [...el.querySelectorAll('dialog')].find(d => d.hasAttribute('open'))!;
    expect(text(dialog)).toContain('Arquivar este produto?');
    buttonByText(dialog, 'Arquivar')!.click();
    fixture.detectChanges();
    expect(catalog.archiveProduct).toHaveBeenCalledWith('r1', 'm1', 'p1');
    expect(productNames(el)).toEqual(['Burrata']);

    el.querySelector<HTMLInputElement>('.c-check input')!.click();
    fixture.detectChanges();
    expect(productNames(el)).toContain('Antigo');
    buttonByText(el.querySelector('.c-prod.archived')!, 'Restaurar')!.click();
    fixture.detectChanges();
    expect(catalog.restoreProduct).toHaveBeenCalled();
  });

  it('prévia mostra só adicionais ativos com o acréscimo formatado', () => {
    const { el, fixture } = setup('OWNER');
    el.querySelector<HTMLButtonElement>('[aria-label="Prévia de Bruschetta"]')!.click();
    fixture.detectChanges();
    const card = el.querySelector('bo-menu-product-card')!;
    expect(text(card)).toContain('Burrata');
    expect(text(card)).toContain('+ R$ 15,00');
    expect(text(card)).toContain('Ingredientes informados pelo restaurante.');
  });

  it('STAFF consulta e altera disponibilidade, sem controles de edição, ordenação ou arquivamento', () => {
    const { el } = setup('STAFF');
    expect(el.querySelectorAll('input[role=switch]').length).toBe(2);
    expect(buttonByText(el, 'Nova categoria')).toBeUndefined();
    expect(buttonByText(el, 'Publicar cardápio')).toBeUndefined();
    expect(buttonByText(el, 'Despublicar cardápio')).toBeUndefined();
    expect(buttonByText(el, 'Desativar categoria')).toBeUndefined();
    expect(el.querySelector('[aria-label^="Mover"]')).toBeNull();
    expect(el.querySelector('[aria-label^="Arquivar"]')).toBeNull();
    expect(el.querySelector('[aria-label^="Editar"]')).toBeNull();
    expect(text(el)).not.toContain('Adicionar produto');
  });

  it('cardápio arquivado fica somente para consulta', () => {
    const { el } = setup('OWNER', menu({ status: 'ARCHIVED' }));
    expect(text(el)).toContain('Este cardápio está arquivado');
    expect(el.querySelectorAll('input[role=switch]').length).toBe(0);
    expect(buttonByText(el, 'Nova categoria')).toBeUndefined();
  });
});
