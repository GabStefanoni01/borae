import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { API_URL, AuthService } from './auth.service';
import { authGuard } from './auth.guard';

function fakeJwt(expSecondsFromNow: number): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({
    sub: 'owner@borae.com',
    iss: 'borae-api',
    exp: Math.floor(Date.now() / 1000) + expSecondsFromNow
  }));
  return `${header}.${payload}.signature`;
}

describe('AuthService e authGuard', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('persiste sessão ao fazer login e limpa sessão e restaurante selecionado no logout', () => {
    const token = fakeJwt(900);
    localStorage.setItem('borae.dashboard.restaurant', 'r1');

    let loggedEmail = '';
    service.login({ email: 'owner@borae.com', password: 'Password@123' }).subscribe(res => {
      loggedEmail = res.user.email;
    });

    const req = http.expectOne(`${API_URL}/api/v1/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush({
      accessToken: token,
      tokenType: 'Bearer',
      expiresIn: 900,
      user: { id: 'u1', fullName: 'Maria Dona', email: 'owner@borae.com' }
    });

    expect(loggedEmail).toBe('owner@borae.com');
    expect(service.hasSession).toBe(true);
    expect(service.currentUser?.fullName).toBe('Maria Dona');

    service.logout();
    expect(service.hasSession).toBe(false);
    expect(service.currentUser).toBeNull();
    expect(localStorage.getItem('borae.auth')).toBeNull();
    expect(localStorage.getItem('borae.dashboard.restaurant')).toBeNull();
  });

  it('limpa automaticamente JWT expirado no localStorage e redireciona no authGuard', () => {
    const expiredToken = fakeJwt(-60);
    localStorage.setItem('borae.auth', JSON.stringify({
      accessToken: expiredToken,
      tokenType: 'Bearer',
      expiresIn: 900,
      user: { id: 'u1', fullName: 'Maria Dona', email: 'owner@borae.com' }
    }));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    const freshService = TestBed.inject(AuthService);
    const router = TestBed.inject(Router);

    expect(freshService.hasSession).toBe(false);
    expect(localStorage.getItem('borae.auth')).toBeNull();

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/auth');
  });

  it('mapeia erros 400, 401, 403, 409, 429 (com mensagem do bloqueio de login) e 500', () => {
    expect(service.getErrorMessage(new HttpErrorResponse({ status: 400 }), 'register'))
      .toContain('pelo menos 2 caracteres');
    expect(service.getErrorMessage(new HttpErrorResponse({ status: 401 }), 'login'))
      .toContain('E-mail ou senha inválidos');
    expect(service.getErrorMessage(new HttpErrorResponse({ status: 403 }), 'login'))
      .toContain('Acesso negado');
    expect(service.getErrorMessage(new HttpErrorResponse({ status: 409 }), 'register'))
      .toContain('já possui uma conta');
    expect(service.getErrorMessage(
      new HttpErrorResponse({
        status: 429,
        error: { message: 'Muitas tentativas de login inválidas. Aguarde 15 minutos para tentar novamente.' }
      }),
      'login'
    )).toBe('Muitas tentativas de login inválidas. Aguarde 15 minutos para tentar novamente.');
    expect(service.getErrorMessage(new HttpErrorResponse({ status: 500 }), 'login'))
      .toContain('problema');
  });
});
