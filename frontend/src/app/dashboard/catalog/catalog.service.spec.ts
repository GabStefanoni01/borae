import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../../auth.service';
import { CatalogService } from './catalog.service';

const BASE = `${API_URL}/api/v1/restaurants/r1/menus`;

describe('CatalogService — contratos da PR #22', () => {
  let service: CatalogService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CatalogService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('cardápios: listar, consultar, criar (sem status), editar, publicar, despublicar e arquivar', () => {
    service.listMenus('r1').subscribe();
    expect(http.expectOne({ method: 'GET', url: BASE }).request.body).toBeNull();

    service.getMenu('r1', 'm1').subscribe();
    http.expectOne({ method: 'GET', url: `${BASE}/m1` }).flush({});

    service.createMenu('r1', { name: 'Almoço', description: null, displayOrder: 2 }).subscribe();
    const create = http.expectOne({ method: 'POST', url: BASE });
    expect(create.request.body).toEqual({ name: 'Almoço', description: null, displayOrder: 2 });
    expect('status' in create.request.body).toBe(false);

    service.updateMenu('r1', 'm1', { name: 'Jantar', description: 'Noite', displayOrder: null }).subscribe();
    expect(http.expectOne({ method: 'PUT', url: `${BASE}/m1` }).request.body).toEqual({ name: 'Jantar', description: 'Noite', displayOrder: null });

    service.publishMenu('r1', 'm1').subscribe();
    http.expectOne({ method: 'POST', url: `${BASE}/m1/publish` });
    service.unpublishMenu('r1', 'm1').subscribe();
    http.expectOne({ method: 'POST', url: `${BASE}/m1/unpublish` });
    service.archiveMenu('r1', 'm1').subscribe();
    http.expectOne({ method: 'POST', url: `${BASE}/m1/archive` });
  });

  it('categorias: criar, atualizar (com active) e reordenar com a lista completa de IDs', () => {
    service.listCategories('r1', 'm1').subscribe();
    http.expectOne({ method: 'GET', url: `${BASE}/m1/categories` });

    service.createCategory('r1', 'm1', { name: 'Entradas', description: null, displayOrder: 0 }).subscribe();
    expect(http.expectOne({ method: 'POST', url: `${BASE}/m1/categories` }).request.body).toEqual({ name: 'Entradas', description: null, displayOrder: 0 });

    service.updateCategory('r1', 'm1', 'c1', { name: 'Entradas', description: null, displayOrder: 0, active: false }).subscribe();
    expect(http.expectOne({ method: 'PUT', url: `${BASE}/m1/categories/c1` }).request.body.active).toBe(false);

    service.reorderCategories('r1', 'm1', ['c2', 'c1', 'c3']).subscribe();
    expect(http.expectOne({ method: 'PUT', url: `${BASE}/m1/categories/order` }).request.body).toEqual(['c2', 'c1', 'c3']);
  });

  it('produtos: listar (com filtro), criar, editar, disponibilidade, arquivar, restaurar e reordenar', () => {
    service.listProducts('r1', 'm1').subscribe();
    http.expectOne({ method: 'GET', url: `${BASE}/m1/products` });
    service.listProducts('r1', 'm1', 'c1').subscribe();
    expect(http.expectOne(req => req.url === `${BASE}/m1/products` && req.params.get('categoryId') === 'c1')).toBeTruthy();

    const payload = { categoryId: 'c1', name: 'Bruschetta', description: null, priceInCents: 2890, displayOrder: 0, ingredients: ['Pão'], addons: [{ name: 'Burrata', priceDeltaInCents: 1500, active: true }] };
    service.createProduct('r1', 'm1', payload).subscribe();
    expect(http.expectOne({ method: 'POST', url: `${BASE}/m1/products` }).request.body).toEqual(payload);
    service.updateProduct('r1', 'm1', 'p1', payload).subscribe();
    expect(http.expectOne({ method: 'PUT', url: `${BASE}/m1/products/p1` }).request.body.priceInCents).toBe(2890);

    service.changeAvailability('r1', 'm1', 'p1', 'UNAVAILABLE').subscribe();
    expect(http.expectOne({ method: 'PATCH', url: `${BASE}/m1/products/p1/availability` }).request.body).toEqual({ availability: 'UNAVAILABLE' });

    service.archiveProduct('r1', 'm1', 'p1').subscribe();
    http.expectOne({ method: 'POST', url: `${BASE}/m1/products/p1/archive` });
    service.restoreProduct('r1', 'm1', 'p1').subscribe();
    http.expectOne({ method: 'POST', url: `${BASE}/m1/products/p1/restore` });

    service.reorderProducts('r1', 'm1', 'c1', ['p2', 'p1', 'p3']).subscribe();
    expect(http.expectOne({ method: 'PUT', url: `${BASE}/m1/products/order` }).request.body).toEqual({ categoryId: 'c1', productIds: ['p2', 'p1', 'p3'] });
  });

  it('imagem: envio multipart com progresso e remoção pelo backend', () => {
    const file = new File(['x'], 'prato.png', { type: 'image/png' });
    const events: HttpEventType[] = [];
    service.uploadProductImage('r1', 'm1', 'p1', file).subscribe(e => events.push(e.type));
    const upload = http.expectOne({ method: 'POST', url: `${BASE}/m1/products/p1/image` });
    expect(upload.request.body).toBeInstanceOf(FormData);
    expect((upload.request.body as FormData).get('file')).toBeInstanceOf(File);
    expect(upload.request.reportProgress).toBe(true);
    upload.flush({ id: 'p1', imageUrl: 'https://ik.imagekit.io/x.webp' });
    expect(events).toContain(HttpEventType.Response);

    service.removeProductImage('r1', 'm1', 'p1').subscribe();
    http.expectOne({ method: 'DELETE', url: `${BASE}/m1/products/p1/image` });
  });
});
