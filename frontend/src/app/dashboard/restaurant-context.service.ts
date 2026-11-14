import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AuthService, AuthUser, RestaurantAccess } from '../auth.service';
import { SLOW_REQUEST_MS } from '../shared/api-warmup';

export type RestaurantRole = RestaurantAccess['role'];

export const ROLE_LABEL: Record<RestaurantRole, string> = {
  OWNER: 'Proprietário',
  MANAGER: 'Gerente',
  STAFF: 'Equipe'
};

const SELECTED_KEY = 'borae.dashboard.restaurant';

/**
 * Contexto do painel: usuário autenticado, restaurantes vinculados e restaurante selecionado.
 *
 * - Todos os dados vêm da API (GET /users/me e GET /users/me/restaurants).
 * - A preferência de restaurante selecionado fica no navegador apenas como conveniência;
 *   ela nunca concede acesso — o backend valida o vínculo em cada requisição.
 * - As permissões abaixo apenas espelham o comportamento ATUAL da API de equipe para
 *   esconder ações indisponíveis. O backend continua sendo a autoridade.
 */
@Injectable()
export class RestaurantContext {
  private readonly auth = inject(AuthService);

  readonly user = signal<AuthUser | null>(null);
  readonly restaurants = signal<RestaurantAccess[]>([]);
  readonly selectedId = signal<string | null>(null);
  readonly status = signal<'loading' | 'ready' | 'error'>('loading');
  readonly errorMessage = signal('');
  /** Verdadeiro quando o carregamento passa de alguns segundos (API reiniciando no Render). */
  readonly slow = signal(false);
  /** Nome do restaurante recém-cadastrado, para a visão geral confirmar o cadastro. */
  readonly justCreated = signal<string | null>(null);
  private slowTimer?: ReturnType<typeof setTimeout>;

  readonly selected = computed(() => this.restaurants().find(r => r.restaurantId === this.selectedId()) ?? null);
  readonly role = computed<RestaurantRole | null>(() => this.selected()?.role ?? null);
  readonly hasRestaurants = computed(() => this.restaurants().length > 0);

  /** GET /restaurants/{id}/members — OWNER e MANAGER. */
  readonly canViewTeam = computed(() => this.role() === 'OWNER' || this.role() === 'MANAGER');
  /** Convidar, alterar papel e remover — somente OWNER na implementação atual. */
  readonly canManageTeam = computed(() => this.role() === 'OWNER');

  load(): void {
    this.status.set('loading');
    this.errorMessage.set('');
    this.slow.set(false);
    clearTimeout(this.slowTimer);
    this.slowTimer = setTimeout(() => this.slow.set(true), SLOW_REQUEST_MS);
    forkJoin({ user: this.auth.getCurrentUser(), restaurants: this.auth.getMyRestaurants() }).subscribe({
      next: ({ user, restaurants }) => {
        this.user.set(user);
        this.setRestaurants(restaurants, this.readStoredSelection());
        this.status.set('ready');
        clearTimeout(this.slowTimer);
      },
      error: (error: unknown) => {
        this.status.set('error');
        clearTimeout(this.slowTimer);
        this.errorMessage.set(describeHttpError(error, 'carregar seus dados'));
      }
    });
  }

  /** Recarrega a lista (após aceitar convite, por exemplo) e seleciona o restaurante indicado. */
  reloadRestaurants(selectId?: string): void {
    this.auth.getMyRestaurants().subscribe({
      next: restaurants => this.setRestaurants(restaurants, selectId ?? this.selectedId())
    });
  }

  select(restaurantId: string): void {
    if (!this.restaurants().some(r => r.restaurantId === restaurantId)) return;
    this.selectedId.set(restaurantId);
    try { localStorage.setItem(SELECTED_KEY, restaurantId); } catch { /* armazenamento indisponível */ }
  }

  /** Adiciona o restaurante devolvido por POST /restaurants e o seleciona. */
  addRestaurant(access: RestaurantAccess): void {
    this.restaurants.update(list => [...list.filter(r => r.restaurantId !== access.restaurantId), access]);
    this.select(access.restaurantId);
    this.justCreated.set(access.restaurantName);
  }

  clear(): void {
    this.justCreated.set(null);
    this.user.set(null);
    this.restaurants.set([]);
    this.selectedId.set(null);
    try { localStorage.removeItem(SELECTED_KEY); } catch { /* armazenamento indisponível */ }
  }

  private setRestaurants(restaurants: RestaurantAccess[], preferredId: string | null): void {
    this.restaurants.set(restaurants);
    const preferred = restaurants.find(r => r.restaurantId === preferredId);
    const next = preferred ?? restaurants[0] ?? null;
    if (next) this.select(next.restaurantId); else this.selectedId.set(null);
  }

  private readStoredSelection(): string | null {
    try { return localStorage.getItem(SELECTED_KEY); } catch { return null; }
  }
}

/** Mensagens padronizadas para erros HTTP do painel. */
export function describeHttpError(error: unknown, action: string): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return `Não foi possível conectar ao Boraê para ${action}. Verifique sua conexão e tente novamente.`;
    if (error.status === 401) return 'Sua sessão expirou. Entre novamente para continuar.';
    if (error.status === 403) return 'Seu papel neste restaurante não permite esta ação.';
    if (error.status === 404) return 'O recurso solicitado não foi encontrado ou não está mais ativo.';
    if (error.status === 409) return 'Esta operação entra em conflito com dados existentes.';
    if (error.status === 400) return 'Confira as informações enviadas e tente novamente.';
    if (error.status >= 500) return 'O servidor encontrou um problema. Tente novamente em instantes.';
  }
  return `Não foi possível ${action}. Tente novamente.`;
}
