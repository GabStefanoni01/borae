import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../auth.service';

/*
 * Contrato público — PublicRestaurantMenuResponse (PR #22, GET /api/v1/public/restaurants/{slug}/menu).
 * O backend já entrega somente: restaurante publicado e ativo, cardápios PUBLISHED, categorias ativas,
 * produtos não arquivados e adicionais ativos. O frontend apenas apresenta.
 */

export type PublicAvailability = 'AVAILABLE' | 'UNAVAILABLE';

export interface PublicAddon { name: string; priceDeltaInCents: number; }

export interface PublicProduct {
  id: string;
  name: string;
  description: string | null;
  priceInCents: number;
  availability: PublicAvailability;
  displayOrder: number;
  ingredients: string[];
  addons: PublicAddon[];
  imageUrl: string | null;
}

export interface PublicCategory {
  id: string;
  name: string;
  description: string | null;
  displayOrder: number;
  products: PublicProduct[];
}

export interface PublicMenu {
  id: string;
  name: string;
  description: string | null;
  displayOrder: number;
  categories: PublicCategory[];
}

export interface PublicRestaurantMenu {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  phone: string | null;
  whatsapp: string | null;
  instagram: string | null;
  website: string | null;
  category: string | null;
  menus: PublicMenu[];
}

@Injectable({ providedIn: 'root' })
export class PublicMenuService {
  private readonly http = inject(HttpClient);

  /** Consulta pública: o interceptor não anexa JWT às rotas /api/v1/public/. */
  getRestaurantMenu(slug: string): Observable<PublicRestaurantMenu> {
    return this.http.get<PublicRestaurantMenu>(`${API_URL}/api/v1/public/restaurants/${encodeURIComponent(slug)}/menu`);
  }
}
