import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { API_URL, AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  const auth = { accessToken: 'jwt-de-teste', logout: vi.fn() };
  const router = { navigate: vi.fn() };

  beforeEach(() => {
    auth.logout.mockClear();
    router.navigate.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router }
      ]
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });
  afterEach(() => backend.verify());

  it('não envia JWT para o cardápio público', () => {
    http.get(`${API_URL}/api/v1/public/restaurants/cantina/menu`).subscribe();
    const req = backend.expectOne(`${API_URL}/api/v1/public/restaurants/cantina/menu`);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('envia JWT para rotas administrativas', () => {
    http.get(`${API_URL}/api/v1/restaurants/r1/menus`).subscribe();
    const req = backend.expectOne(`${API_URL}/api/v1/restaurants/r1/menus`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-de-teste');
    req.flush([]);
  });

  it('401 em rota pública não encerra a sessão; em rota autenticada, encerra', () => {
    http.get(`${API_URL}/api/v1/public/restaurants/x/menu`).subscribe({ error: () => undefined });
    backend.expectOne(`${API_URL}/api/v1/public/restaurants/x/menu`).flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).not.toHaveBeenCalled();

    http.get(`${API_URL}/api/v1/restaurants/r1/menus`).subscribe({ error: () => undefined });
    backend.expectOne(`${API_URL}/api/v1/restaurants/r1/menus`).flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['/auth'], { queryParams: { sessao: 'expirada' } });
  });
});
