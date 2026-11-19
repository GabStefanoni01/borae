import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../environments/environment';

export interface AuthUser {
  id: string;
  fullName: string;
  email: string;
}
export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}
export interface RestaurantAccess {
  membershipId: string;
  restaurantId: string;
  restaurantName: string;
  restaurantSlug: string;
  role: 'OWNER' | 'MANAGER' | 'STAFF';
}
export interface CreateRestaurantPayload {
  name: string;
  description?: string;
}
export interface RestaurantMember {
  membershipId: string;
  restaurantId: string;
  userId: string;
  fullName: string;
  email: string;
  role: 'OWNER' | 'MANAGER' | 'STAFF';
}
export interface RestaurantInvitation {
  invitationId: string;
  email: string;
  role: 'MANAGER' | 'STAFF';
  expiresAt: string;
  token: string;
}
export interface LoginPayload { email: string; password: string; }
export interface RegisterPayload extends LoginPayload { fullName: string; }

export const API_URL = environment.apiUrl;

const SELECTED_RESTAURANT_STORAGE_KEY = 'borae.dashboard.restaurant';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storageKey = 'borae.auth';
  private session: AuthResponse | null = this.readSession();

  get currentUser(): AuthUser | null {
    if (!this.hasSession) return null;
    return this.session?.user ?? null;
  }

  get accessToken(): string | null {
    if (this.session?.accessToken && isJwtExpired(this.session.accessToken)) {
      this.logout();
      return null;
    }
    return this.session?.accessToken ?? null;
  }

  get hasSession(): boolean {
    return Boolean(this.accessToken);
  }

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/api/v1/auth/login`, payload)
      .pipe(tap(response => this.saveSession(response)));
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/api/v1/auth/register`, payload)
      .pipe(tap(response => this.saveSession(response)));
  }

  getCurrentUser(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${API_URL}/api/v1/users/me`).pipe(
      tap(user => {
        if (this.session) this.saveSession({ ...this.session, user });
      })
    );
  }

  getMyRestaurants(): Observable<RestaurantAccess[]> {
    return this.http.get<RestaurantAccess[]>(`${API_URL}/api/v1/users/me/restaurants`);
  }

  createRestaurant(payload: CreateRestaurantPayload): Observable<RestaurantAccess> {
    return this.http.post<RestaurantAccess>(`${API_URL}/api/v1/restaurants`, payload);
  }

  getRestaurantMembers(restaurantId: string): Observable<RestaurantMember[]> {
    return this.http.get<RestaurantMember[]>(`${API_URL}/api/v1/restaurants/${restaurantId}/members`);
  }

  inviteRestaurantMember(restaurantId: string, email: string, role: 'MANAGER' | 'STAFF'): Observable<RestaurantInvitation> {
    return this.http.post<RestaurantInvitation>(
      `${API_URL}/api/v1/restaurants/${restaurantId}/invitations`, { email, role }
    );
  }

  acceptRestaurantInvitation(token: string): Observable<RestaurantMember> {
    return this.http.post<RestaurantMember>(`${API_URL}/api/v1/restaurant-invitations/accept`, { token });
  }

  updateRestaurantMemberRole(restaurantId: string, membershipId: string, role: 'MANAGER' | 'STAFF'): Observable<RestaurantMember> {
    return this.http.patch<RestaurantMember>(
      `${API_URL}/api/v1/restaurants/${restaurantId}/members/${membershipId}`, { role }
    );
  }

  removeRestaurantMember(restaurantId: string, membershipId: string): Observable<void> {
    return this.http.delete<void>(
      `${API_URL}/api/v1/restaurants/${restaurantId}/members/${membershipId}`
    );
  }

  logout(): void {
    this.session = null;
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(this.storageKey);
      localStorage.removeItem(SELECTED_RESTAURANT_STORAGE_KEY);
    }
  }

  getErrorMessage(error: unknown, mode: 'login' | 'register'): string {
    if (error instanceof HttpErrorResponse) {
      const apiMessage = extractApiMessage(error);
      if (error.status === 0) return 'Não foi possível conectar à API. Verifique a disponibilidade do serviço e a configuração de CORS.';
      if (error.status === 401) return 'E-mail ou senha inválidos. Confira os dados e tente novamente.';
      if (error.status === 403) return apiMessage ?? 'Acesso negado para esta operação.';
      if (error.status === 409) return 'Este e-mail já possui uma conta. Faça login ou use outro e-mail.';
      if (error.status === 429) return apiMessage ?? 'Muitas tentativas em sequência. Aguarde alguns minutos para tentar novamente.';
      if (error.status === 400) return 'Confira os campos. O nome deve ter pelo menos 2 caracteres e a senha, no mínimo 8.';
      if (error.status >= 500) return 'O servidor encontrou um problema. Tente novamente em instantes.';
    }
    return mode === 'login' ? 'Não foi possível entrar. Tente novamente.' : 'Não foi possível criar sua conta. Tente novamente.';
  }

  private saveSession(response: AuthResponse): void {
    this.session = response;
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(this.storageKey, JSON.stringify(response));
    }
  }

  private readSession(): AuthResponse | null {
    if (typeof localStorage === 'undefined') return null;
    try {
      const raw = localStorage.getItem(this.storageKey);
      if (!raw) return null;
      const parsed = JSON.parse(raw) as AuthResponse;
      if (!parsed.accessToken || !parsed.user?.email || isJwtExpired(parsed.accessToken)) {
        localStorage.removeItem(this.storageKey);
        return null;
      }
      return parsed;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }
}

function extractApiMessage(error: HttpErrorResponse): string | null {
  const body = error.error;
  if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
    const trimmed = body.message.trim();
    if (trimmed) return trimmed;
  }
  return null;
}

/** Verificação apenas de UX (o backend continua sendo a autoridade de expiração/assinatura do JWT). */
function isJwtExpired(token: string): boolean {
  const parts = token.split('.');
  if (parts.length !== 3) return false;
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const payload = JSON.parse(atob(padded)) as { exp?: unknown };
    return typeof payload.exp === 'number' && payload.exp * 1000 <= Date.now();
  } catch {
    return false;
  }
}
