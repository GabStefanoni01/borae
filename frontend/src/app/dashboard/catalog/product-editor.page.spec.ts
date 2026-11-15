import { HttpErrorResponse, HttpEventType, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { buttonByText, category, fakeContext, menu, product, text } from '../../../testing/catalog-fixtures';
import { RestaurantContext, RestaurantRole } from '../restaurant-context.service';
import { ToastService } from '../ui/feedback';
import { Product } from './catalog.models';
import { CatalogService } from './catalog.service';
import { ProductEditorPage, validateImage } from './product-editor.page';

function setup(role: RestaurantRole = 'OWNER', productId?: string, products: Product[] = [product()]) {
  const catalog = {
    getMenu: vi.fn(() => of(menu())),
    listCategories: vi.fn(() => of([category(), category({ id: 'c2', name: 'Massas', displayOrder: 1 })])),
    listProducts: vi.fn(() => of(products)),
    createProduct: vi.fn((_r: string, _m: string, body: object) => of(product({ id: 'novo', ...body }))),
    updateProduct: vi.fn((_r: string, _m: string, id: string, body: object) => of(product({ id, ...body }))),
    uploadProductImage: vi.fn((_r: string, _m: string, id: string) => of(new HttpResponse({ body: product({ id, imageUrl: 'https://ik.imagekit.io/x.webp' }) }))),
    removeProductImage: vi.fn(() => of(undefined))
  };
  TestBed.configureTestingModule({
    imports: [ProductEditorPage],
    providers: [provideRouter([]), ToastService, { provide: RestaurantContext, useValue: fakeContext(role) }, { provide: CatalogService, useValue: catalog }]
  });
  const router = TestBed.inject(Router);
  const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  const fixture = TestBed.createComponent(ProductEditorPage);
  fixture.componentRef.setInput('menuId', 'm1');
  if (productId) fixture.componentRef.setInput('productId', productId);
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;
  const type = (selector: string, value: string) => {
    const input = el.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
  };
  const click = (label: string) => { buttonByText(el, label)!.click(); fixture.detectChanges(); };
  return { fixture, catalog, el, type, click, navigate, toast: TestBed.inject(ToastService) };
}

describe('ProductEditorPage', () => {
  it('cria produto convertendo preço em centavos, com ingredientes e adicionais ordenados', async () => {
    const { el, type, click, catalog, navigate, fixture } = setup();
    await fixture.whenStable();
    fixture.detectChanges();
    type('#pe-name', '  Spaghetti   carbonara ');
    type('#pe-price', '58,9');
    await fixture.whenStable();
    expect(el.querySelector<HTMLInputElement>('#pe-price')!.value).toBe('58,90');

    for (const name of ['Guanciale', 'Gema', 'Pecorino']) {
      type('#pe-new-ing', name);
      click('Adicionar ingrediente');
    }
    el.querySelector<HTMLButtonElement>('[aria-label="Mover Pecorino para cima"]')!.click();
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('[aria-label="Remover Gema"]')!.click();
    fixture.detectChanges();

    click('Adicionar adicional');
    await fixture.whenStable();
    type('.c-addon-row input:not([inputmode]):not([type=checkbox])', 'Bacon');
    type('.c-addon-row input[inputmode]', '4');
    expect(text(el.querySelector('.c-addon-preview'))).toBe('Bacon — + R$ 4,00');

    click('Salvar produto');
    await fixture.whenStable();
    expect(catalog.createProduct).toHaveBeenCalledWith('r1', 'm1', {
      categoryId: 'c1', name: 'Spaghetti   carbonara', description: null, priceInCents: 5890, displayOrder: 1,
      ingredients: ['Guanciale', 'Pecorino'], addons: [{ name: 'Bacon', priceDeltaInCents: 400, active: true }]
    });
    expect(navigate).toHaveBeenCalledWith(['/app/cardapios', 'm1'], { queryParams: { categoria: 'c1' } });
  });

  it('bloqueia preço inválido e ingredientes repetidos antes de chamar a API', async () => {
    const { type, click, catalog, el, fixture } = setup();
    await fixture.whenStable();
    fixture.detectChanges();
    type('#pe-name', 'Prato');
    type('#pe-price', '-3');
    click('Salvar produto');
    expect(text(el.querySelector('.c-sticky-actions .d-alert-error'))).toContain('preço válido');
    type('#pe-price', '10');
    for (const name of ['Tomate', 'tomate']) { type('#pe-new-ing', name); click('Adicionar ingrediente'); }
    click('Salvar produto');
    expect(text(el.querySelector('.c-sticky-actions .d-alert-error'))).toBe('Não repita ingredientes no mesmo produto.');
    expect(catalog.createProduct).not.toHaveBeenCalled();
  });

  it('edita produto existente preservando ordem e trata 400 do servidor', async () => {
    const { type, click, catalog, el, fixture } = setup('OWNER', 'p1');
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el.querySelector<HTMLInputElement>('#pe-price')!.value).toBe('28,90');
    expect([...el.querySelectorAll<HTMLInputElement>('.c-row-input')].map(i => i.value)).toEqual(['Pão', 'Tomate']);
    catalog.updateProduct.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 400 })));
    type('#pe-price', '30');
    click('Salvar produto');
    expect(text(el.querySelector('.c-sticky-actions .d-alert-error'))).toContain('Confira as informações');
    expect(catalog.updateProduct).toHaveBeenCalledWith('r1', 'm1', 'p1', expect.objectContaining({ priceInCents: 3000, displayOrder: 0 }));
  });

  it('produto arquivado não pode ser editado', async () => {
    const { el, fixture } = setup('OWNER', 'p1', [product({ lifecycleStatus: 'ARCHIVED' })]);
    await fixture.whenStable();
    fixture.detectChanges();
    expect(text(el)).toContain('Produto arquivado não pode ser editado');
    expect(el.querySelector('#pe-name')).toBeNull();
  });

  it('envia, substitui e remove imagem do produto existente', async () => {
    const { el, fixture, catalog, toast } = setup('OWNER', 'p1');
    await fixture.whenStable();
    fixture.detectChanges();
    const input = el.querySelector<HTMLInputElement>('.c-file input')!;
    const pick = (file: File) => {
      Object.defineProperty(input, 'files', { value: [file], configurable: true });
      input.dispatchEvent(new Event('change'));
      fixture.detectChanges();
    };
    pick(new File(['x'], 'prato.png', { type: 'image/png' }));
    expect(catalog.uploadProductImage).toHaveBeenCalledTimes(1);
    expect(toast.toasts().at(-1)?.text).toBe('Imagem atualizada com sucesso.');
    expect(el.querySelector<HTMLImageElement>('.c-image-box img')!.src).toContain('imagekit');

    pick(new File(['y'], 'outra.webp', { type: 'image/webp' }));
    expect(catalog.uploadProductImage).toHaveBeenCalledTimes(2);

    catalog.uploadProductImage.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 500 })));
    pick(new File(['z'], 'falha.jpg', { type: 'image/jpeg' }));
    expect(text(el.querySelector('.c-product-side .c-field-error'))).toBe('Não foi possível enviar a imagem. Confira o formato e o tamanho do arquivo.');

    buttonByText(el, 'Remover')!.click();
    fixture.detectChanges();
    expect(catalog.removeProductImage).toHaveBeenCalledWith('r1', 'm1', 'p1');
    expect(text(el.querySelector('.c-image-empty'))).toBe('Nenhuma imagem adicionada.');
  });

  it('recusa formato e tamanho inválidos antes do envio', () => {
    expect(validateImage(new File(['%PDF'], 'menu.pdf', { type: 'application/pdf' }))).toContain('Formato não aceito');
    expect(validateImage(new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'grande.jpg', { type: 'image/jpeg' }))).toContain('10 MB');
    expect(validateImage(new File(['x'], 'ok.jpeg', { type: 'image/jpeg' }))).toBeNull();
  });

  it('STAFF não acessa o formulário de produto', () => {
    const { el, catalog } = setup('STAFF', 'p1');
    expect(text(el)).toContain('Edição disponível para proprietário e gerentes');
    expect(catalog.listProducts).not.toHaveBeenCalled();
  });
});

describe('progresso do envio', () => {
  it('reflete eventos de progresso do HttpClient', async () => {
    const { el, fixture, catalog } = setup('OWNER', 'p1');
    await fixture.whenStable();
    fixture.detectChanges();
    catalog.uploadProductImage.mockReturnValueOnce(of({ type: HttpEventType.UploadProgress, loaded: 50, total: 100 }) as never);
    const input = el.querySelector<HTMLInputElement>('.c-file input')!;
    Object.defineProperty(input, 'files', { value: [new File(['x'], 'a.png', { type: 'image/png' })], configurable: true });
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(text(el.querySelector('.c-upload'))).toContain('Enviando imagem... 50%');
  });
});
