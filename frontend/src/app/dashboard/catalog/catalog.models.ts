/*
 * Contratos do catálogo — espelham os DTOs do backend da PR #22
 * (branch docs/catalog-menu-module-plan, pacote com.borae.catalog):
 * MenuResponse, CreateMenuRequest, UpdateMenuRequest, MenuCategoryResponse,
 * Create/UpdateMenuCategoryRequest, MenuProductResponse, Create/UpdateMenuProductRequest,
 * ProductAddonRequest, ChangeProductAvailabilityRequest e ReorderMenuProductsRequest.
 * O backend é a autoridade das regras (permissões, estados, validação e isolamento).
 */

export type MenuStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type ProductAvailability = 'AVAILABLE' | 'UNAVAILABLE';
export type ProductLifecycleStatus = 'ACTIVE' | 'ARCHIVED';

export interface Menu {
  id: string;
  name: string;
  description: string | null;
  status: MenuStatus;
  displayOrder: number;
}

/** POST /menus — todo cardápio novo nasce como DRAFT (o payload não tem status). */
export interface MenuCreateRequest {
  name: string;
  description: string | null;
  displayOrder: number;
}

/** PUT /menus/{id} — displayOrder opcional (nulo mantém a ordem atual). */
export interface MenuUpdateRequest {
  name: string;
  description: string | null;
  displayOrder: number | null;
}

export interface Category {
  id: string;
  menuId: string;
  name: string;
  description: string | null;
  active: boolean;
  displayOrder: number;
}

export interface CategoryCreateRequest {
  name: string;
  description: string | null;
  displayOrder: number;
}

/** PUT /categories/{id} — atualização completa (nome, descrição, ordem e estado). */
export interface CategoryUpdateRequest {
  name: string;
  description: string | null;
  displayOrder: number;
  active: boolean;
}

export interface Addon {
  name: string;
  priceDeltaInCents: number;
  active: boolean;
}

export interface Product {
  id: string;
  menuId: string;
  categoryId: string;
  name: string;
  description: string | null;
  priceInCents: number;
  lifecycleStatus: ProductLifecycleStatus;
  availability: ProductAvailability;
  displayOrder: number;
  ingredients: string[];
  addons: Addon[];
  imageUrl: string | null;
}

/** POST e PUT /products — mesmo formato (ingredientes e adicionais são substituídos por inteiro). */
export interface ProductSaveRequest {
  categoryId: string;
  name: string;
  description: string | null;
  priceInCents: number;
  displayOrder: number;
  ingredients: string[];
  addons: Addon[];
}
export type ProductCreateRequest = ProductSaveRequest;
export type ProductUpdateRequest = ProductSaveRequest;

export interface ReorderProductsRequest {
  categoryId: string;
  productIds: string[];
}

/** Limites dos DTOs (@Size), usados apenas para orientar o preenchimento. */
export const CATALOG_LIMITS = {
  menuName: 120,
  menuDescription: 500,
  categoryName: 120,
  categoryDescription: 500,
  productName: 160,
  productDescription: 1000,
  ingredientName: 120,
  maxIngredients: 40,
  addonName: 120,
  maxAddons: 30,
  imageBytes: 10 * 1024 * 1024,
  imageTypes: ['image/jpeg', 'image/png', 'image/webp'],
  imageExtensions: ['jpg', 'jpeg', 'png', 'webp']
} as const;

export const MENU_STATUS_LABEL: Record<MenuStatus, string> = {
  DRAFT: 'Rascunho',
  PUBLISHED: 'Publicado',
  ARCHIVED: 'Arquivado'
};

export const AVAILABILITY_LABEL: Record<ProductAvailability, string> = {
  AVAILABLE: 'Disponível',
  UNAVAILABLE: 'Indisponível'
};

/** Move um item da lista uma posição (para cima: -1, para baixo: +1). Devolve uma nova lista. */
export function moveItem<T>(list: readonly T[], index: number, direction: -1 | 1): T[] {
  const target = index + direction;
  if (index < 0 || index >= list.length || target < 0 || target >= list.length) return [...list];
  const copy = [...list];
  [copy[index], copy[target]] = [copy[target], copy[index]];
  return copy;
}

/** Próxima posição de exibição (maior ordem existente + 1). */
export function nextDisplayOrder(items: readonly { displayOrder: number }[]): number {
  return items.reduce((max, item) => Math.max(max, item.displayOrder + 1), 0);
}

/** Texto opcional: vazio vira null (o backend também normaliza). */
export function optionalText(value: string | null | undefined): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed ? trimmed : null;
}
