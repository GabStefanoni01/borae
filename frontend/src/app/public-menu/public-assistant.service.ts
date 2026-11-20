import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { API_URL } from '../auth.service';
import { PublicAvailability } from './public-menu.service';

export interface AssistantInteractionFilters {
  maxPriceInCents?: number | null;
  categoryIds?: string[] | null;
  excludedIngredients?: string[] | null;
}

export interface AssistantInteractionRequest {
  message: string;
  menuId?: string | null;
  filters?: AssistantInteractionFilters | null;
}

export interface AssistantRecommendation {
  productId: string;
  name: string;
  priceInCents: number;
  availability: PublicAvailability;
  imageUrl: string | null;
  ingredients: string[];
  reason: string;
}

export interface AssistantQuota {
  remainingConsumer: number;
  remainingRestaurant: number;
  resetsAt: string;
}

export interface AssistantInteractionResponse {
  answer: string;
  recommendations: AssistantRecommendation[];
  quota: AssistantQuota | null;
  fallback: boolean;
  fallbackCode: string | null;
}

export interface AssistantApiErrorBody {
  code?: string;
  scope?: 'CONSUMER' | 'RESTAURANT';
  message?: string;
  resetsAt?: string;
  retryAfterSeconds?: number;
}

/**
 * Cliente HTTP público do Assistente Gastronômico.
 *
 * Usa exclusivamente sessão anônima por cookie HttpOnly emitido pelo backend
 * (withCredentials: true), nunca envia JWT de restaurante e nunca manipula chaves de IA.
 */
@Injectable({ providedIn: 'root' })
export class PublicAssistantService {
  private readonly http = inject(HttpClient);
  private sessionReady = false;
  private inFlightBootstrap$: Observable<void> | null = null;

  ensureSession(force = false): Observable<void> {
    if (this.sessionReady && !force) {
      return of(void 0);
    }
    if (!force && this.inFlightBootstrap$) {
      return this.inFlightBootstrap$;
    }
    const request$ = this.http
      .post<void>(`${API_URL}/api/v1/public/assistant/sessions`, null, { withCredentials: true })
      .pipe(
        tap(() => {
          this.sessionReady = true;
          this.inFlightBootstrap$ = null;
        }),
        catchError((error: unknown) => {
          this.sessionReady = false;
          this.inFlightBootstrap$ = null;
          return throwError(() => error);
        }),
        shareReplay(1)
      );
    this.inFlightBootstrap$ = request$;
    return request$;
  }

  interact(slug: string, payload: AssistantInteractionRequest): Observable<AssistantInteractionResponse> {
    const url = `${API_URL}/api/v1/public/restaurants/${encodeURIComponent(slug)}/assistant/interactions`;
    const send = () => this.http.post<AssistantInteractionResponse>(url, payload, { withCredentials: true });

    return this.ensureSession().pipe(
      switchMap(() => send()),
      catchError((error: unknown) => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.sessionReady = false;
          return this.ensureSession(true).pipe(switchMap(() => send()));
        }
        return throwError(() => error);
      })
    );
  }

  trackRecommendationClick(slug: string, productId: string): Observable<void> {
    const url = `${API_URL}/api/v1/public/restaurants/${encodeURIComponent(slug)}/assistant/recommendations/${encodeURIComponent(productId)}/click`;
    return this.http.post<void>(url, null, { withCredentials: true }).pipe(
      catchError(() => of(void 0))
    );
  }

  resetLocalSessionState(): void {
    this.sessionReady = false;
    this.inFlightBootstrap$ = null;
  }
}
