import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { API_URL } from '../auth.service';
import { PublicAssistantService } from './public-assistant.service';

describe('PublicAssistantService', () => {
  let service: PublicAssistantService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PublicAssistantService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('inicia sessão anônima com withCredentials e reutiliza na interação seguinte', () => {
    let responseAnswer = '';
    service.interact('cantina', { message: 'Quero uma massa leve' }).subscribe(res => {
      responseAnswer = res.answer;
    });

    const sessionReq = http.expectOne(`${API_URL}/api/v1/public/assistant/sessions`);
    expect(sessionReq.request.method).toBe('POST');
    expect(sessionReq.request.withCredentials).toBe(true);
    sessionReq.flush(null, { status: 204, statusText: 'No Content' });

    const interactReq = http.expectOne(`${API_URL}/api/v1/public/restaurants/cantina/assistant/interactions`);
    expect(interactReq.request.method).toBe('POST');
    expect(interactReq.request.withCredentials).toBe(true);
    expect(interactReq.request.body).toEqual({ message: 'Quero uma massa leve' });
    interactReq.flush({
      answer: 'Recomendo o Carbonara.',
      recommendations: [],
      quota: { remainingConsumer: 19, remainingRestaurant: 99, resetsAt: '2026-11-20T00:00:00-03:00' },
      fallback: false,
      fallbackCode: null
    });

    expect(responseAnswer).toBe('Recomendo o Carbonara.');

    // Segunda chamada não refaz bootstrap da sessão
    service.interact('cantina', { message: 'Outra opção?' }).subscribe();
    const secondReq = http.expectOne(`${API_URL}/api/v1/public/restaurants/cantina/assistant/interactions`);
    secondReq.flush({
      answer: 'Veja também as entradas.',
      recommendations: [],
      quota: null,
      fallback: false,
      fallbackCode: null
    });
  });

  it('renova a sessão anônima automaticamente ao receber 401 e reenvia a interação', () => {
    let answer = '';
    service.ensureSession().subscribe();
    http.expectOne(`${API_URL}/api/v1/public/assistant/sessions`).flush(null, { status: 204, statusText: 'No Content' });

    service.interact('cantina', { message: 'O que recomenda?' }).subscribe(res => {
      answer = res.answer;
    });

    const firstAttempt = http.expectOne(`${API_URL}/api/v1/public/restaurants/cantina/assistant/interactions`);
    firstAttempt.flush({ message: 'Sessão expirada' }, { status: 401, statusText: 'Unauthorized' });

    const reboot = http.expectOne(`${API_URL}/api/v1/public/assistant/sessions`);
    expect(reboot.request.withCredentials).toBe(true);
    reboot.flush(null, { status: 204, statusText: 'No Content' });

    const retryAttempt = http.expectOne(`${API_URL}/api/v1/public/restaurants/cantina/assistant/interactions`);
    retryAttempt.flush({
      answer: 'Sessão renovada com sucesso.',
      recommendations: [],
      quota: null,
      fallback: false,
      fallbackCode: null
    });

    expect(answer).toBe('Sessão renovada com sucesso.');
  });

  it('registra clique de recomendação com withCredentials sem quebrar em caso de falha', () => {
    let completed = false;
    service.trackRecommendationClick('cantina', 'prod-1').subscribe({ complete: () => (completed = true) });

    const req = http.expectOne(`${API_URL}/api/v1/public/restaurants/cantina/assistant/recommendations/prod-1/click`);
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush(null, { status: 500, statusText: 'Error' });

    expect(completed).toBe(true);
  });
});
