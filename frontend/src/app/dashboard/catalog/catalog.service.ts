import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../auth.service';
import {
  Category, CategoryCreateRequest, CategoryUpdateRequest, Menu, MenuCreateRequest, MenuUpdateRequest,
  Model3dDimensions, Product, Product3dModel, ProductAvailability, ProductSaveRequest
} from './catalog.models';

/**
 * Chamadas HTTP do catálogo (cardápios, categorias, produtos e imagem do produto).
 * Endpoints da PR #22 — MenuController, MenuProductController e MenuProductImageController.
 * Todas as rotas são escopadas pelo restaurante selecionado no contexto do painel;
 * o backend confere o vínculo e o papel do usuário em cada requisição.
 */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  private menusUrl(restaurantId: string): string {
    return `${API_URL}/api/v1/restaurants/${restaurantId}/menus`;
  }
  private productsUrl(restaurantId: string, menuId: string): string {
    return `${this.menusUrl(restaurantId)}/${menuId}/products`;
  }

  // ---------- Cardápios ----------
  listMenus(restaurantId: string): Observable<Menu[]> {
    return this.http.get<Menu[]>(this.menusUrl(restaurantId));
  }

  getMenu(restaurantId: string, menuId: string): Observable<Menu> {
    return this.http.get<Menu>(`${this.menusUrl(restaurantId)}/${menuId}`);
  }

  createMenu(restaurantId: string, request: MenuCreateRequest): Observable<Menu> {
    return this.http.post<Menu>(this.menusUrl(restaurantId), request);
  }

  updateMenu(restaurantId: string, menuId: string, request: MenuUpdateRequest): Observable<Menu> {
    return this.http.put<Menu>(`${this.menusUrl(restaurantId)}/${menuId}`, request);
  }

  publishMenu(restaurantId: string, menuId: string): Observable<Menu> {
    return this.http.post<Menu>(`${this.menusUrl(restaurantId)}/${menuId}/publish`, null);
  }

  /** Despublicar devolve o cardápio para DRAFT. */
  unpublishMenu(restaurantId: string, menuId: string): Observable<Menu> {
    return this.http.post<Menu>(`${this.menusUrl(restaurantId)}/${menuId}/unpublish`, null);
  }

  /** Arquivar preserva os dados; não há endpoint para desarquivar nesta versão. */
  archiveMenu(restaurantId: string, menuId: string): Observable<Menu> {
    return this.http.post<Menu>(`${this.menusUrl(restaurantId)}/${menuId}/archive`, null);
  }

  // ---------- Categorias ----------
  listCategories(restaurantId: string, menuId: string): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.menusUrl(restaurantId)}/${menuId}/categories`);
  }

  createCategory(restaurantId: string, menuId: string, request: CategoryCreateRequest): Observable<Category> {
    return this.http.post<Category>(`${this.menusUrl(restaurantId)}/${menuId}/categories`, request);
  }

  updateCategory(restaurantId: string, menuId: string, categoryId: string, request: CategoryUpdateRequest): Observable<Category> {
    return this.http.put<Category>(`${this.menusUrl(restaurantId)}/${menuId}/categories/${categoryId}`, request);
  }

  /** O corpo é a lista COMPLETA de IDs das categorias do cardápio, na nova ordem. */
  reorderCategories(restaurantId: string, menuId: string, categoryIds: string[]): Observable<Category[]> {
    return this.http.put<Category[]>(`${this.menusUrl(restaurantId)}/${menuId}/categories/order`, categoryIds);
  }

  // ---------- Produtos ----------
  /** Lista produtos ativos e arquivados do cardápio (opcionalmente de uma categoria). */
  listProducts(restaurantId: string, menuId: string, categoryId?: string): Observable<Product[]> {
    const params: Record<string, string> = categoryId ? { categoryId } : {};
    return this.http.get<Product[]>(this.productsUrl(restaurantId, menuId), { params });
  }

  createProduct(restaurantId: string, menuId: string, request: ProductSaveRequest): Observable<Product> {
    return this.http.post<Product>(this.productsUrl(restaurantId, menuId), request);
  }

  updateProduct(restaurantId: string, menuId: string, productId: string, request: ProductSaveRequest): Observable<Product> {
    return this.http.put<Product>(`${this.productsUrl(restaurantId, menuId)}/${productId}`, request);
  }

  /** OWNER, MANAGER e STAFF podem alterar a disponibilidade. */
  changeAvailability(restaurantId: string, menuId: string, productId: string, availability: ProductAvailability): Observable<Product> {
    return this.http.patch<Product>(`${this.productsUrl(restaurantId, menuId)}/${productId}/availability`, { availability });
  }

  archiveProduct(restaurantId: string, menuId: string, productId: string): Observable<Product> {
    return this.http.post<Product>(`${this.productsUrl(restaurantId, menuId)}/${productId}/archive`, null);
  }

  restoreProduct(restaurantId: string, menuId: string, productId: string): Observable<Product> {
    return this.http.post<Product>(`${this.productsUrl(restaurantId, menuId)}/${productId}/restore`, null);
  }

  /** O contrato exige a lista COMPLETA de IDs dos produtos da categoria, na nova ordem. */
  reorderProducts(restaurantId: string, menuId: string, categoryId: string, productIds: string[]): Observable<Product[]> {
    return this.http.put<Product[]>(`${this.productsUrl(restaurantId, menuId)}/order`, { categoryId, productIds });
  }

  // ---------- Imagem do produto ----------
  /** Envia ou substitui a imagem (multipart "file"); o backend processa e guarda no ImageKit. */
  uploadProductImage(restaurantId: string, menuId: string, productId: string, file: File): Observable<HttpEvent<Product>> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<Product>(`${this.productsUrl(restaurantId, menuId)}/${productId}/image`, form, {
      reportProgress: true,
      observe: 'events'
    });
  }

  removeProductImage(restaurantId: string, menuId: string, productId: string): Observable<void> {
    return this.http.delete<void>(`${this.productsUrl(restaurantId, menuId)}/${productId}/image`);
  }

  // ---------- Modelo 3D do produto (PR #27) ----------
  private modelUrl(restaurantId: string, menuId: string, productId: string): string {
    return `${this.productsUrl(restaurantId, menuId)}/${productId}/model-3d`;
  }

  /** Somente OWNER/MANAGER. 404 = o produto ainda não tem modelo 3D. */
  getModel3d(restaurantId: string, menuId: string, productId: string): Observable<Product3dModel> {
    return this.http.get<Product3dModel>(this.modelUrl(restaurantId, menuId, productId));
  }

  /** Envia ou substitui o GLB (multipart: file, widthMm, heightMm, depthMm), com progresso. */
  uploadModel3d(restaurantId: string, menuId: string, productId: string, file: File, size: Model3dDimensions): Observable<HttpEvent<Product3dModel>> {
    const form = new FormData();
    form.append('file', file, file.name);
    form.append('widthMm', String(size.widthMm));
    form.append('heightMm', String(size.heightMm));
    form.append('depthMm', String(size.depthMm));
    return this.http.post<Product3dModel>(this.modelUrl(restaurantId, menuId, productId), form, { reportProgress: true, observe: 'events' });
  }

  setModel3dPublication(restaurantId: string, menuId: string, productId: string, published: boolean): Observable<Product3dModel> {
    return this.http.patch<Product3dModel>(`${this.modelUrl(restaurantId, menuId, productId)}/publication`, { published });
  }

  deleteModel3d(restaurantId: string, menuId: string, productId: string): Observable<void> {
    return this.http.delete<void>(this.modelUrl(restaurantId, menuId, productId));
  }
}
