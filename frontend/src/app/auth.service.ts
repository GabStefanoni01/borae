import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';

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

export const API_URL = 'https://borae.onrender.com';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storageKey = 'borae.auth';
  private session: AuthResponse | null = this.readSession();

  get currentUser(): AuthUser | null {
    return this.session?.user ?? null;
  }

  get accessToken(): string | null {
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
    if (typeof localStorage !== 'undefined') localStorage.removeItem(this.storageKey);
  }

  getErrorMessage(error: unknown, mode: 'login' | 'register'): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) return 'Não foi possível conectar à API. Verifique a disponibilidade do serviço e a configuração de CORS.';
      if (error.status === 401) return 'E-mail ou senha inválidos. Confira os dados e tente novamente.';
      if (error.status === 409) return 'Este e-mail já possui uma conta. Faça login ou use outro e-mail.';
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
      if (!parsed.accessToken || !parsed.user?.email) {
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
