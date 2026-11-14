import { HttpClient, HttpErrorResponse, HttpEvent } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../auth.service';

/*
 * Contratos do perfil do restaurante — espelham os DTOs do backend
 * (branch feat/restaurant-profile-api: RestaurantProfileResponse, UpdateRestaurantProfileRequest,
 * BusinessHours*, RestaurantMediaResponse). O backend é a autoridade das regras.
 */

export type PublicationStatus = 'DRAFT' | 'PUBLISHED' | 'SUSPENDED';

export interface RestaurantProfile {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  /** Campo interno do backend (restaurante ativo na plataforma). Não é o status de publicação. */
  active: boolean;
  phone: string | null;
  whatsapp: string | null;
  instagram: string | null;
  website: string | null;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  category: string | null;
  publicationStatus: PublicationStatus;
}

/**
 * PATCH /restaurants/{id}: campo omitido = mantém o valor; string vazia = limpa o campo.
 * Latitude/longitude omitidas mantêm os valores atuais (a API ainda não permite removê-las).
 */
export interface UpdateRestaurantProfile {
  name?: string;
  description?: string;
  slug?: string;
  phone?: string;
  whatsapp?: string;
  instagram?: string;
  website?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  category?: string;
}

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface BusinessInterval { opensAt: string; closesAt: string; }
export interface BusinessDay { dayOfWeek: DayOfWeek; intervals: BusinessInterval[]; }

export type MediaType = 'LOGO' | 'PHOTO';

export interface RestaurantMedia {
  id: string;
  type: MediaType;
  /** URL pública da imagem no provedor (ImageKit). */
  url: string;
  contentType: string;
  sizeBytes: number;
  width: number;
  height: number;
  createdAt: string;
}

/** Limites documentados pela API de mídia (validados de novo no servidor). */
export const MEDIA_LIMITS = {
  LOGO: 5 * 1024 * 1024,
  PHOTO: 10 * 1024 * 1024,
  maxPhotos: 100,
  types: ['image/jpeg', 'image/png', 'image/webp'],
  extensions: ['jpg', 'jpeg', 'png', 'webp']
};

export const PUBLICATION_LABEL: Record<PublicationStatus, string> = {
  DRAFT: 'Rascunho',
  PUBLISHED: 'Publicado',
  SUSPENDED: 'Suspenso'
};

@Injectable({ providedIn: 'root' })
export class RestaurantProfileService {
  private readonly http = inject(HttpClient);
  private base(restaurantId: string): string {
    return `${API_URL}/api/v1/restaurants/${restaurantId}`;
  }

  getProfile(restaurantId: string): Observable<RestaurantProfile> {
    return this.http.get<RestaurantProfile>(this.base(restaurantId));
  }

  updateProfile(restaurantId: string, changes: UpdateRestaurantProfile): Observable<RestaurantProfile> {
    return this.http.patch<RestaurantProfile>(this.base(restaurantId), changes);
  }

  getBusinessHours(restaurantId: string): Observable<BusinessDay[]> {
    return this.http.get<BusinessDay[]>(`${this.base(restaurantId)}/business-hours`);
  }

  /** Substitui a grade semanal inteira; dias omitidos ficam fechados. */
  replaceBusinessHours(restaurantId: string, days: BusinessDay[]): Observable<BusinessDay[]> {
    return this.http.put<BusinessDay[]>(`${this.base(restaurantId)}/business-hours`, { days });
  }

  listMedia(restaurantId: string): Observable<RestaurantMedia[]> {
    return this.http.get<RestaurantMedia[]>(`${this.base(restaurantId)}/media`);
  }

  /** Upload multipart (type + file), com eventos de progresso. */
  uploadMedia(restaurantId: string, type: MediaType, file: File): Observable<HttpEvent<RestaurantMedia>> {
    const form = new FormData();
    form.append('type', type);
    form.append('file', file, file.name);
    return this.http.post<RestaurantMedia>(`${this.base(restaurantId)}/media`, form, { reportProgress: true, observe: 'events' });
  }

  deleteMedia(restaurantId: string, mediaId: string): Observable<void> {
    return this.http.delete<void>(`${this.base(restaurantId)}/media/${mediaId}`);
  }
}

/** A API não devolve o texto do erro (server.error.include-message=never); mensagens por status e contexto. */
export function profileErrorMessage(error: unknown, context: 'profile' | 'hours' | 'media-upload' | 'media-delete' | 'load'): string {
  const status = error instanceof HttpErrorResponse ? error.status : -1;
  if (status === 0) return 'Não foi possível conectar ao Boraê. Verifique sua conexão e tente novamente.';
  if (status === 401) return 'Sua sessão expirou. Entre novamente para continuar.';
  if (status === 403) return 'Seu papel neste restaurante não permite administrar o perfil. Somente proprietário e gerentes podem editar.';
  if (status === 404) return context === 'media-delete' ? 'Esta imagem já não existe.' : 'Restaurante não encontrado ou inativo.';
  if (status >= 500) return context === 'media-upload'
    ? 'O servidor não conseguiu armazenar a imagem agora. Tente novamente em instantes.'
    : 'O servidor encontrou um problema. Tente novamente em instantes.';
  switch (context) {
    case 'profile':
      if (status === 409) return 'Este endereço público já está em uso por outro restaurante.';
      if (status === 400) return 'Confira os campos: nome obrigatório, endereço público só com letras, números e hífens, e latitude/longitude informadas juntas e dentro dos limites.';
      break;
    case 'hours':
      if (status === 400) return 'A grade não foi aceita. Confira se cada dia aberto tem horários completos, se o fechamento é depois da abertura e se os intervalos não se sobrepõem.';
      break;
    case 'media-upload':
      if (status === 413) return 'Arquivo grande demais. A logo pode ter até 5 MB e cada foto até 10 MB.';
      if (status === 409) return 'O restaurante já atingiu o limite de 100 fotos.';
      if (status === 400) return 'Não foi possível processar a imagem. Envie um arquivo JPEG, PNG ou WebP válido.';
      break;
  }
  return 'Não foi possível concluir a operação. Tente novamente.';
}
