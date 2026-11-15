import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { IconComponent } from '../../shared/icon.component';
import { MenuProductCardComponent, MenuProductView } from '../../shared/menu-product-card.component';
import { formatCents } from '../../shared/money';
import { RestaurantContext } from '../restaurant-context.service';
import { PanelDialogComponent, ToastService } from '../ui/feedback';
import { ConfirmDialogComponent, StateComponent } from '../ui/ui.components';
import { catalogErrorMessage, httpStatus } from './catalog-errors';
import {
  AVAILABILITY_LABEL, CATALOG_LIMITS, Category, MENU_STATUS_LABEL, Menu, Product, moveItem, nextDisplayOrder, optionalText
} from './catalog.models';
import { CatalogService } from './catalog.service';
import { MenuFormComponent, MenuFormValue } from './menu-form.component';

type MenuAction = 'publish' | 'unpublish' | 'archive';

/**
 * Editor de um cardápio: informações e estado, categorias e produtos por categoria.
 * OWNER/MANAGER administram tudo; STAFF consulta e altera somente a disponibilidade.
 * Toda mudança é aplicada a partir da resposta do servidor (sem sucesso otimista).
 */
@Component({
  selector: 'bo-menu-editor-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule, RouterLink, IconComponent, StateComponent, ConfirmDialogComponent, PanelDialogComponent,
    MenuFormComponent, MenuProductCardComponent
  ],
  template: `
    <nav class="d-breadcrumb c-crumb" aria-label="Você está em">
      <a routerLink="/app/cardapios"><bo-icon name="arrow-left" [size]="14" /> Cardápios</a>
      @if (menu()) { <span aria-hidden="true">/</span><span aria-current="page">{{ menu()!.name }}</span> }
    </nav>

    @if (loading() && !menu()) {
      <div class="c-skeleton c-skeleton-hero" aria-busy="true" aria-label="Carregando cardápio"></div>
      <div class="c-editor"><div class="c-skeleton c-skeleton-panel"></div><div class="c-skeleton c-skeleton-panel"></div></div>
    } @else if (loadError()) {
      <bo-state [kind]="notFound() ? 'not-found' : 'error'" [title]="notFound() ? 'Este conteúdo não está mais disponível.' : 'Não foi possível carregar o cardápio.'" [text]="notFound() ? 'O cardápio pode ter sido removido deste restaurante.' : loadError()">
        @if (notFound()) {
          <a routerLink="/app/cardapios" class="d-btn d-btn-secondary"><bo-icon name="arrow-left" [size]="16" /> Voltar aos cardápios</a>
        } @else {
          <button type="button" class="d-btn d-btn-secondary" (click)="load()"><bo-icon name="refresh" [size]="16" /> Tentar novamente</button>
        }
      </bo-state>
    } @else if (menu(); as m) {
      <!-- Informações e estado do cardápio -->
      <section class="c-hero" [attr.data-status]="m.status">
        <div class="c-hero-main">
          <div class="c-hero-title">
            <h1>{{ m.name }}</h1>
            <span class="c-status" [attr.data-status]="m.status"><span class="c-status-dot" aria-hidden="true"></span>{{ statusLabel[m.status] }}</span>
          </div>
          <p>{{ m.description || 'Sem descrição.' }}</p>
          <p class="c-hero-meta">{{ categories().length }} {{ categories().length === 1 ? 'categoria' : 'categorias' }} · {{ activeProductCount() }} {{ activeProductCount() === 1 ? 'produto ativo' : 'produtos ativos' }}</p>
        </div>
        @if (editable()) {
          <div class="c-hero-actions">
            <button type="button" class="d-btn d-btn-secondary" (click)="menuFormOpen.set(true)" [disabled]="!!menuBusy()"><bo-icon name="sliders" [size]="16" /> Editar cardápio</button>
            @if (m.status === 'DRAFT') {
              <button type="button" class="d-btn d-btn-primary" (click)="runMenu('publish')" [disabled]="!!menuBusy()">
                <bo-icon [name]="menuBusy() === 'publish' ? 'loader' : 'globe'" [size]="16" [spin]="menuBusy() === 'publish'" /> Publicar cardápio
              </button>
            } @else {
              <button type="button" class="d-btn d-btn-secondary" (click)="runMenu('unpublish')" [disabled]="!!menuBusy()">
                <bo-icon [name]="menuBusy() === 'unpublish' ? 'loader' : 'eye-off'" [size]="16" [spin]="menuBusy() === 'unpublish'" /> Despublicar cardápio
              </button>
            }
            <button type="button" class="d-btn d-btn-ghost" (click)="archiveMenuOpen.set(true)" [disabled]="!!menuBusy()">Arquivar</button>
          </div>
        }
      </section>

      @if (archived()) {
        <div class="d-alert d-alert-warning"><bo-icon name="lock" [size]="16" />
          <p>Este cardápio está arquivado: não aparece na página pública e não pode ser alterado. Os dados continuam salvos para consulta.</p>
        </div>
      } @else if (m.status === 'PUBLISHED' && canManage()) {
        <div class="d-alert d-alert-info"><bo-icon name="info" [size]="16" />
          <p>Este cardápio está publicado: as alterações salvas aparecem imediatamente para os clientes, desde que o perfil do restaurante esteja publicado.</p>
        </div>
      } @else if (!canManage()) {
        <div class="d-alert d-alert-info"><bo-icon name="info" [size]="16" />
          <p>Com acesso de <strong>Equipe</strong>, você pode consultar o cardápio e marcar produtos como disponíveis ou indisponíveis.</p>
        </div>
      }

      <div class="c-editor-wrap"><div class="c-editor">
        <!-- Categorias -->
        <section class="d-card c-cats" aria-labelledby="c-cats-title">
          <div class="c-panel-head">
            <div>
              <h2 id="c-cats-title">Organize suas categorias</h2>
              <p>Separe seus pratos por grupos para tornar o cardápio mais fácil de explorar.</p>
            </div>
            @if (editable()) {
              <button type="button" class="d-btn d-btn-secondary d-btn-sm" (click)="openCategory(null)"><bo-icon name="plus" [size]="15" /> Nova categoria</button>
            }
          </div>

          @if (!categories().length) {
            <bo-state kind="empty" icon="tag" [compact]="true" title="Nenhuma categoria ainda"
              [text]="editable() ? 'Crie categorias como Entradas, Pratos principais, Bebidas ou Sobremesas para organizar seus produtos.' : 'Este cardápio ainda não possui categorias.'">
              @if (editable()) { <button type="button" class="d-btn d-btn-primary d-btn-sm" (click)="openCategory(null)"><bo-icon name="plus" [size]="15" /> Criar categoria</button> }
            </bo-state>
          } @else {
            <ol class="c-cat-list">
              @for (cat of categories(); track cat.id; let i = $index, first = $first, last = $last) {
                <li class="c-cat" [class.selected]="cat.id === selectedCategoryId()" [class.inactive]="!cat.active">
                  <button type="button" class="c-cat-select" (click)="selectedCategoryId.set(cat.id)" [attr.aria-pressed]="cat.id === selectedCategoryId()">
                    <span class="c-cat-pos" aria-hidden="true">{{ i + 1 }}</span>
                    <span class="c-cat-text">
                      <strong>{{ cat.name }}</strong>
                      <small>{{ productCountLabel(cat.id) }}@if (!cat.active) { · <em>Inativa</em> }</small>
                    </span>
                  </button>
                  @if (editable()) {
                    <div class="c-cat-tools">
                      <button type="button" class="d-icon-btn" (click)="moveCategory(i, -1)" [disabled]="first || reordering()" [attr.aria-label]="'Mover ' + cat.name + ' para cima'"><bo-icon name="chevron-down" [size]="16" class="c-rot" /></button>
                      <button type="button" class="d-icon-btn" (click)="moveCategory(i, 1)" [disabled]="last || reordering()" [attr.aria-label]="'Mover ' + cat.name + ' para baixo'"><bo-icon name="chevron-down" [size]="16" /></button>
                      <button type="button" class="d-icon-btn" (click)="openCategory(cat)" [attr.aria-label]="'Editar categoria ' + cat.name"><bo-icon name="sliders" [size]="15" /></button>
                    </div>
                  }
                </li>
              }
            </ol>
          }
        </section>

        <!-- Produtos da categoria selecionada -->
        <section class="d-card c-products" aria-labelledby="c-prod-title">
          @if (selectedCategory(); as cat) {
            <div class="c-panel-head">
              <div>
                <h2 id="c-prod-title">{{ cat.name }}</h2>
                <p>{{ cat.description || 'Produtos desta categoria.' }}</p>
              </div>
              <div class="c-panel-actions">
                @if (editable()) {
                  <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="toggleCategoryActive(cat)" [disabled]="categoryBusy() === cat.id">
                    @if (categoryBusy() === cat.id) { <bo-icon name="loader" [size]="14" [spin]="true" /> }
                    {{ cat.active ? 'Desativar categoria' : 'Ativar categoria' }}
                  </button>
                  <a class="d-btn d-btn-primary d-btn-sm" [routerLink]="['produtos', 'novo']" [queryParams]="{ categoria: cat.id }"><bo-icon name="plus" [size]="15" /> Adicionar produto</a>
                }
              </div>
            </div>

            @if (!cat.active) {
              <p class="c-inline-note"><bo-icon name="eye-off" [size]="14" /> Categoria inativa: ela e seus produtos não aparecem na página pública.</p>
            }

            <div class="c-toolbar">
              <label class="c-search">
                <span class="sr-only">Buscar produto</span>
                <bo-icon name="search" [size]="15" />
                <input type="search" class="d-input d-input-sm" placeholder="Buscar nesta categoria" [ngModel]="search()" (ngModelChange)="search.set($event)">
              </label>
              @if (archivedInCategory() > 0) {
                <label class="c-check"><input type="checkbox" [ngModel]="showArchived()" (ngModelChange)="showArchived.set($event)"> Mostrar arquivados ({{ archivedInCategory() }})</label>
              }
            </div>

            @if (productsError()) {
              <bo-state kind="error" [compact]="true" title="Não foi possível carregar os produtos." [text]="productsError()">
                <button type="button" class="d-btn d-btn-secondary d-btn-sm" (click)="loadProducts()">Tentar novamente</button>
              </bo-state>
            } @else if (!visibleProducts().length) {
              <bo-state kind="empty" icon="utensils" [compact]="true"
                [title]="search() ? 'Nenhum produto encontrado' : 'Nenhum produto nesta categoria'"
                [text]="search() ? 'Tente outro termo de busca.' : (editable() ? 'Adicione o primeiro produto desta categoria.' : 'Os produtos cadastrados aparecerão aqui.')" />
            } @else {
              <ul class="c-prod-list">
                @for (p of visibleProducts(); track p.id; let i = $index, first = $first, last = $last) {
                  <li class="c-prod" [class.archived]="p.lifecycleStatus === 'ARCHIVED'">
                    <span class="c-prod-thumb" aria-hidden="true">
                      @if (p.imageUrl) { <img [src]="p.imageUrl" alt="" loading="lazy" width="56" height="56"> } @else { <bo-icon name="image" [size]="20" /> }
                    </span>
                    <div class="c-prod-info">
                      <div class="c-prod-name">
                        <strong>{{ p.name }}</strong>
                        @if (p.lifecycleStatus === 'ARCHIVED') { <span class="c-tag c-tag-archived">Arquivado</span> }
                        @else if (p.availability === 'UNAVAILABLE') { <span class="c-tag c-tag-off">Indisponível</span> }
                      </div>
                      @if (p.description) { <p class="c-prod-desc">{{ p.description }}</p> }
                      <span class="c-prod-price">{{ price(p.priceInCents) }}</span>
                    </div>
                    <div class="c-prod-actions">
                      @if (p.lifecycleStatus === 'ACTIVE' && !archived()) {
                        <label class="c-toggle" [class.busy]="availabilityBusy() === p.id">
                          <input type="checkbox" role="switch" [checked]="p.availability === 'AVAILABLE'" (change)="toggleAvailability(p, $event)" [disabled]="availabilityBusy() === p.id"
                                 [attr.aria-label]="'Disponibilidade de ' + p.name">
                          <span class="c-toggle-track" aria-hidden="true"><span class="c-toggle-thumb"></span></span>
                          <span class="c-toggle-label">{{ availabilityLabel[p.availability] }}</span>
                        </label>
                      }
                      <div class="c-prod-buttons">
                        <button type="button" class="d-icon-btn" (click)="preview.set(p)" [attr.aria-label]="'Prévia de ' + p.name"><bo-icon name="eye" [size]="16" /></button>
                        @if (editable()) {
                          @if (p.lifecycleStatus === 'ACTIVE') {
                            <a class="d-icon-btn" [routerLink]="['produtos', p.id]" [attr.aria-label]="'Editar ' + p.name"><bo-icon name="sliders" [size]="15" /></a>
                          }
                          @if (!search()) {
                            <button type="button" class="d-icon-btn" (click)="moveProduct(p, -1)" [disabled]="first || reordering()" [attr.aria-label]="'Mover ' + p.name + ' para cima'"><bo-icon name="chevron-down" [size]="16" class="c-rot" /></button>
                            <button type="button" class="d-icon-btn" (click)="moveProduct(p, 1)" [disabled]="last || reordering()" [attr.aria-label]="'Mover ' + p.name + ' para baixo'"><bo-icon name="chevron-down" [size]="16" /></button>
                          }
                          @if (p.lifecycleStatus === 'ACTIVE') {
                            <button type="button" class="d-icon-btn" (click)="archiveProductTarget.set(p)" [attr.aria-label]="'Arquivar ' + p.name"><bo-icon name="trash" [size]="15" /></button>
                          } @else {
                            <button type="button" class="d-btn d-btn-secondary d-btn-sm" (click)="restoreProduct(p)" [disabled]="productBusy() === p.id">
                              @if (productBusy() === p.id) { <bo-icon name="loader" [size]="14" [spin]="true" /> } Restaurar
                            </button>
                          }
                        }
                      </div>
                    </div>
                  </li>
                }
              </ul>
            }
          } @else {
            <bo-state kind="empty" icon="utensils" [compact]="true" title="Selecione uma categoria" text="Os produtos são organizados dentro das categorias do cardápio." />
          }
        </section>
      </div></div>
    }

    <!-- Diálogos -->
    <bo-panel-dialog [open]="menuFormOpen()" title="Editar cardápio" subtitle="Atualize as informações do seu cardápio." (closed)="requestMenuClose(menuForm()?.dirty() ?? false)">
      @if (menuFormOpen()) {
        <bo-menu-form [menu]="menu()" [busy]="menuSaving()" [error]="menuFormError()" (saved)="saveMenu($event)" (cancelled)="requestMenuClose($event)" />
      }
    </bo-panel-dialog>

    <bo-panel-dialog [open]="categoryFormOpen()" [title]="editingCategory() ? 'Editar categoria' : 'Nova categoria'"
      subtitle="Dê um nome à categoria para agrupar produtos semelhantes." (closed)="requestCategoryClose()">
      @if (categoryFormOpen()) {
        <form class="d-form" (ngSubmit)="saveCategory()" novalidate>
          <div class="d-field">
            <label for="cf-name">Nome da categoria</label>
            <input id="cf-name" name="name" class="d-input" [(ngModel)]="categoryName" required [maxlength]="limits.categoryName" placeholder="Ex.: Entradas, Massas, Bebidas" autocomplete="off"
                   [attr.aria-invalid]="!!categoryError() || null">
            <span class="d-hint"><span></span><span>{{ categoryName.length }}/{{ limits.categoryName }}</span></span>
          </div>
          <div class="d-field">
            <label for="cf-desc">Descrição <span>(opcional)</span></label>
            <textarea id="cf-desc" name="description" class="d-input" rows="3" [(ngModel)]="categoryDescription" [maxlength]="limits.categoryDescription"></textarea>
            <span class="d-hint"><span></span><span>{{ categoryDescription.length }}/{{ limits.categoryDescription }}</span></span>
          </div>
          @if (editingCategory()) {
            <label class="c-check"><input type="checkbox" name="active" [(ngModel)]="categoryActive"> Categoria ativa (aparece na página pública)</label>
          }
          @if (categoryError()) { <div class="d-alert d-alert-error" role="alert"><bo-icon name="alert" [size]="16" /><p>{{ categoryError() }}</p></div> }
          <div class="d-form-actions c-form-end">
            <button type="button" class="d-btn d-btn-secondary" (click)="requestCategoryClose()" [disabled]="categorySaving()">Cancelar</button>
            <button type="submit" class="d-btn d-btn-primary" [disabled]="categorySaving()">
              @if (categorySaving()) { <bo-icon name="loader" [size]="16" [spin]="true" /> } Salvar categoria
            </button>
          </div>
        </form>
      }
    </bo-panel-dialog>

    <bo-panel-dialog [open]="!!preview()" title="Prévia do produto" subtitle="Como o cliente verá este produto no cardápio digital." (closed)="preview.set(null)">
      @if (previewView(); as view) {
        <div class="c-preview">
          @if (preview()?.lifecycleStatus === 'ARCHIVED') {
            <p class="c-inline-note"><bo-icon name="eye-off" [size]="14" /> Produto arquivado: não aparece no cardápio público.</p>
          }
          <bo-menu-product-card [product]="view" />
        </div>
      }
    </bo-panel-dialog>

    <bo-confirm-dialog [open]="!!discardOpen()" title="Descartar alterações?" message="As alterações ainda não foram salvas."
      confirmLabel="Descartar" [danger]="true" (confirmed)="confirmDiscard()" (cancelled)="discardOpen.set(null)" />

    <bo-confirm-dialog [open]="archiveMenuOpen()" title="Arquivar este cardápio?"
      message="Ele deixará de aparecer na página pública, mas seus dados serão preservados." confirmLabel="Arquivar"
      [danger]="true" [busy]="menuBusy() === 'archive'" (confirmed)="runMenu('archive')" (cancelled)="archiveMenuOpen.set(false)" />

    <bo-confirm-dialog [open]="!!archiveProductTarget()" title="Arquivar este produto?"
      message="Ele deixará de aparecer no cardápio público, mas continuará salvo no painel." confirmLabel="Arquivar"
      [danger]="true" [busy]="!!productBusy()" (confirmed)="archiveProduct()" (cancelled)="archiveProductTarget.set(null)" />
  `
})
export class MenuEditorPage {
  /** Parâmetro de rota :menuId (withComponentInputBinding). */
  readonly menuId = input.required<string>();
  /** Categoria a abrir (?categoria=), usada ao voltar do formulário de produto. */
  readonly categoria = input<string | undefined>(undefined);

  protected readonly ctx = inject(RestaurantContext);
  private readonly catalog = inject(CatalogService);
  private readonly toast = inject(ToastService);

  protected get limits() { return CATALOG_LIMITS; }
  protected get statusLabel() { return MENU_STATUS_LABEL; }
  protected get availabilityLabel() { return AVAILABILITY_LABEL; }

  protected readonly menu = signal<Menu | null>(null);
  protected readonly categories = signal<Category[]>([]);
  protected readonly products = signal<Product[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal('');
  protected readonly notFound = signal(false);
  protected readonly productsError = signal('');

  protected readonly selectedCategoryId = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly showArchived = signal(false);
  protected readonly reordering = signal(false);
  protected readonly availabilityBusy = signal<string | null>(null);
  protected readonly productBusy = signal<string | null>(null);
  protected readonly categoryBusy = signal<string | null>(null);
  protected readonly preview = signal<Product | null>(null);
  protected readonly archiveProductTarget = signal<Product | null>(null);

  protected readonly menuFormOpen = signal(false);
  protected readonly menuSaving = signal(false);
  protected readonly menuFormError = signal('');
  protected readonly menuBusy = signal<MenuAction | null>(null);
  protected readonly archiveMenuOpen = signal(false);
  protected readonly menuForm = viewChild(MenuFormComponent);

  protected readonly categoryFormOpen = signal(false);
  protected readonly editingCategory = signal<Category | null>(null);
  protected readonly categorySaving = signal(false);
  protected readonly categoryError = signal('');
  protected categoryName = '';
  protected categoryDescription = '';
  protected categoryActive = true;
  protected readonly discardOpen = signal<'menu' | 'category' | null>(null);

  protected readonly canManage = this.ctx.canManageCatalog;
  protected readonly archived = computed(() => this.menu()?.status === 'ARCHIVED');
  /** Ações de conteúdo: somente OWNER/MANAGER e cardápio não arquivado (o backend responde 409). */
  protected readonly editable = computed(() => this.canManage() && !this.archived());

  protected readonly selectedCategory = computed(() => this.categories().find(c => c.id === this.selectedCategoryId()) ?? null);
  /** Produtos da categoria selecionada na ordem do servidor (inclui arquivados). */
  private readonly categoryProducts = computed(() => this.products()
    .filter(p => p.categoryId === this.selectedCategoryId())
    .sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name, 'pt-BR')));
  protected readonly archivedInCategory = computed(() => this.categoryProducts().filter(p => p.lifecycleStatus === 'ARCHIVED').length);
  protected readonly visibleProducts = computed(() => {
    const term = normalize(this.search());
    return this.categoryProducts()
      .filter(p => this.showArchived() || p.lifecycleStatus === 'ACTIVE')
      .filter(p => !term || normalize(`${p.name} ${p.description ?? ''} ${p.ingredients.join(' ')}`).includes(term));
  });
  protected readonly activeProductCount = computed(() => this.products().filter(p => p.lifecycleStatus === 'ACTIVE').length);
  protected readonly previewView = computed<MenuProductView | null>(() => {
    const p = this.preview();
    if (!p) return null;
    return {
      name: p.name, description: p.description, priceInCents: p.priceInCents, availability: p.availability,
      ingredients: p.ingredients, imageUrl: p.imageUrl,
      addons: p.addons.filter(a => a.active).map(a => ({ name: a.name, priceDeltaInCents: a.priceDeltaInCents }))
    };
  });

  constructor() {
    effect(() => {
      const restaurantId = this.ctx.selectedId();
      const menuId = this.menuId();
      untracked(() => { if (restaurantId && menuId) this.load(); });
    });
  }

  load(): void {
    const restaurantId = this.ctx.selectedId();
    const menuId = this.menuId();
    if (!restaurantId) return;
    this.loading.set(true);
    this.loadError.set('');
    this.notFound.set(false);
    forkJoin({
      menu: this.catalog.getMenu(restaurantId, menuId),
      categories: this.catalog.listCategories(restaurantId, menuId)
    }).subscribe({
      next: ({ menu, categories }) => {
        this.menu.set(menu);
        this.setCategories(categories);
        this.loading.set(false);
        this.loadProducts();
      },
      error: error => {
        this.loading.set(false);
        this.menu.set(null);
        this.notFound.set(httpStatus(error) === 404);
        this.loadError.set(catalogErrorMessage(error, 'load-menu'));
      }
    });
  }

  loadProducts(): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId) return;
    this.productsError.set('');
    this.catalog.listProducts(restaurantId, this.menuId()).subscribe({
      next: products => this.products.set(products),
      error: error => this.productsError.set(catalogErrorMessage(error, 'load-products'))
    });
  }

  protected price(cents: number): string { return formatCents(cents); }

  protected productCountLabel(categoryId: string): string {
    const count = this.products().filter(p => p.categoryId === categoryId && p.lifecycleStatus === 'ACTIVE').length;
    return count === 1 ? '1 produto' : `${count} produtos`;
  }

  // ---------- Cardápio ----------
  protected requestMenuClose(dirty: boolean): void {
    if (this.menuSaving()) return;
    if (dirty) this.discardOpen.set('menu'); else this.menuFormOpen.set(false);
  }

  protected saveMenu(value: MenuFormValue): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId) return;
    this.menuSaving.set(true);
    this.menuFormError.set('');
    this.catalog.updateMenu(restaurantId, this.menuId(), { name: value.name, description: optionalText(value.description), displayOrder: null }).subscribe({
      next: menu => {
        this.menuSaving.set(false);
        this.menu.set(menu);
        this.menuFormOpen.set(false);
        this.toast.success('Alterações salvas com sucesso.');
      },
      error: error => {
        this.menuSaving.set(false);
        this.menuFormError.set(catalogErrorMessage(error, 'save-menu'));
      }
    });
  }

  protected runMenu(action: MenuAction): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId || this.menuBusy()) return;
    this.menuBusy.set(action);
    const id = this.menuId();
    const request$ = action === 'publish' ? this.catalog.publishMenu(restaurantId, id)
      : action === 'unpublish' ? this.catalog.unpublishMenu(restaurantId, id) : this.catalog.archiveMenu(restaurantId, id);
    request$.subscribe({
      next: menu => {
        this.menuBusy.set(null);
        this.archiveMenuOpen.set(false);
        this.menu.set(menu);
        this.toast.success(action === 'publish' ? 'Cardápio publicado.' : action === 'unpublish' ? 'Cardápio despublicado. Ele voltou para rascunho.' : 'Cardápio arquivado.');
      },
      error: error => {
        this.menuBusy.set(null);
        this.archiveMenuOpen.set(false);
        this.toast.error(catalogErrorMessage(error, action === 'publish' ? 'publish-menu' : action === 'unpublish' ? 'unpublish-menu' : 'archive-menu'));
      }
    });
  }

  // ---------- Categorias ----------
  protected openCategory(category: Category | null): void {
    this.editingCategory.set(category);
    this.categoryName = category?.name ?? '';
    this.categoryDescription = category?.description ?? '';
    this.categoryActive = category?.active ?? true;
    this.categoryError.set('');
    this.categoryFormOpen.set(true);
  }

  protected requestCategoryClose(): void {
    if (this.categorySaving()) return;
    const c = this.editingCategory();
    const dirty = this.categoryName.trim() !== (c?.name ?? '') || this.categoryDescription.trim() !== (c?.description ?? '') || this.categoryActive !== (c?.active ?? true);
    if (dirty) this.discardOpen.set('category'); else this.categoryFormOpen.set(false);
  }

  protected confirmDiscard(): void {
    if (this.discardOpen() === 'menu') this.menuFormOpen.set(false); else this.categoryFormOpen.set(false);
    this.discardOpen.set(null);
  }

  protected saveCategory(): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId || this.categorySaving()) return;
    const name = this.categoryName.trim();
    if (!name) { this.categoryError.set('Informe o nome da categoria.'); return; }
    const editing = this.editingCategory();
    this.categorySaving.set(true);
    this.categoryError.set('');
    const request$ = editing
      ? this.catalog.updateCategory(restaurantId, this.menuId(), editing.id, { name, description: optionalText(this.categoryDescription), displayOrder: editing.displayOrder, active: this.categoryActive })
      : this.catalog.createCategory(restaurantId, this.menuId(), { name, description: optionalText(this.categoryDescription), displayOrder: nextDisplayOrder(this.categories()) });
    request$.subscribe({
      next: category => {
        this.categorySaving.set(false);
        this.categoryFormOpen.set(false);
        this.upsertCategory(category);
        if (!editing) this.selectedCategoryId.set(category.id);
        this.toast.success('Categoria salva com sucesso.');
      },
      error: error => {
        this.categorySaving.set(false);
        this.categoryError.set(catalogErrorMessage(error, 'save-category'));
      }
    });
  }

  protected toggleCategoryActive(category: Category): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId || this.categoryBusy()) return;
    this.categoryBusy.set(category.id);
    this.catalog.updateCategory(restaurantId, this.menuId(), category.id, {
      name: category.name, description: category.description, displayOrder: category.displayOrder, active: !category.active
    }).subscribe({
      next: updated => {
        this.categoryBusy.set(null);
        this.upsertCategory(updated);
        this.toast.success(updated.active ? 'Categoria ativada.' : 'Categoria desativada. Ela não aparece mais na página pública.');
      },
      error: error => {
        this.categoryBusy.set(null);
        this.toast.error(catalogErrorMessage(error, 'save-category'));
      }
    });
  }

  /** Envia a lista completa na nova ordem; aplica a resposta do servidor ou recarrega em caso de falha. */
  protected moveCategory(index: number, direction: -1 | 1): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId || this.reordering()) return;
    const ids = moveItem(this.categories(), index, direction).map(c => c.id);
    this.reordering.set(true);
    this.catalog.reorderCategories(restaurantId, this.menuId(), ids).subscribe({
      next: categories => {
        this.reordering.set(false);
        this.setCategories(categories);
      },
      error: error => {
        this.reordering.set(false);
        this.toast.error(catalogErrorMessage(error, 'reorder-categories'));
        this.catalog.listCategories(restaurantId, this.menuId()).subscribe({ next: list => this.setCategories(list) });
      }
    });
  }

  // ---------- Produtos ----------
  protected toggleAvailability(product: Product, event?: Event): void {
    // O switch só muda de estado quando o servidor confirmar (sem sucesso otimista).
    if (event?.target instanceof HTMLInputElement) event.target.checked = product.availability === 'AVAILABLE';
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId || this.availabilityBusy()) return;
    const next = product.availability === 'AVAILABLE' ? 'UNAVAILABLE' : 'AVAILABLE';
    this.availabilityBusy.set(product.id);
    this.catalog.changeAvailability(restaurantId, this.menuId(), product.id, next).subscribe({
      next: updated => {
        this.availabilityBusy.set(null);
        this.upsertProduct(updated);
        this.toast.success(updated.availability === 'AVAILABLE' ? `${updated.name} está disponível.` : `${updated.name} foi marcado como indisponível.`);
      },
      error: error => {
        this.availabilityBusy.set(null);
        this.toast.error(catalogErrorMessage(error, 'availability'));
      }
    });
  }

  /**
   * Move o produto entre os visíveis, aplicado sobre a lista COMPLETA da categoria
   * (incluindo arquivados), como exige PUT /products/order.
   */
  protected moveProduct(product: Product, direction: -1 | 1): void {
    const restaurantId = this.ctx.selectedId();
    const categoryId = this.selectedCategoryId();
    if (!restaurantId || !categoryId || this.reordering()) return;
    const visible = this.visibleProducts();
    const neighbor = visible[visible.findIndex(p => p.id === product.id) + direction];
    if (!neighbor) return;
    const full = this.categoryProducts().map(p => p.id);
    const a = full.indexOf(product.id);
    const b = full.indexOf(neighbor.id);
    [full[a], full[b]] = [full[b], full[a]];
    this.reordering.set(true);
    this.catalog.reorderProducts(restaurantId, this.menuId(), categoryId, full).subscribe({
      next: updated => {
        this.reordering.set(false);
        updated.forEach(p => this.upsertProduct(p));
      },
      error: error => {
        this.reordering.set(false);
        this.toast.error(catalogErrorMessage(error, 'reorder-products'));
        this.loadProducts();
      }
    });
  }

  protected archiveProduct(): void {
    const product = this.archiveProductTarget();
    const restaurantId = this.ctx.selectedId();
    if (!product || !restaurantId) return;
    this.productBusy.set(product.id);
    this.catalog.archiveProduct(restaurantId, this.menuId(), product.id).subscribe({
      next: updated => {
        this.productBusy.set(null);
        this.archiveProductTarget.set(null);
        this.upsertProduct(updated);
        this.toast.success('Produto arquivado. Ele continua salvo no painel.');
      },
      error: error => {
        this.productBusy.set(null);
        this.archiveProductTarget.set(null);
        this.toast.error(catalogErrorMessage(error, 'archive-product'));
      }
    });
  }

  protected restoreProduct(product: Product): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId || this.productBusy()) return;
    this.productBusy.set(product.id);
    this.catalog.restoreProduct(restaurantId, this.menuId(), product.id).subscribe({
      next: updated => {
        this.productBusy.set(null);
        this.upsertProduct(updated);
        this.toast.success('Produto restaurado.');
      },
      error: error => {
        this.productBusy.set(null);
        this.toast.error(catalogErrorMessage(error, 'restore-product'));
      }
    });
  }

  private setCategories(categories: Category[]): void {
    const sorted = [...categories].sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name, 'pt-BR'));
    this.categories.set(sorted);
    if (!sorted.some(c => c.id === this.selectedCategoryId())) {
      const requested = sorted.find(c => c.id === this.categoria());
      this.selectedCategoryId.set(requested?.id ?? sorted[0]?.id ?? null);
    }
  }

  private upsertCategory(category: Category): void {
    const list = this.categories();
    this.setCategories(list.some(c => c.id === category.id) ? list.map(c => (c.id === category.id ? category : c)) : [...list, category]);
  }

  private upsertProduct(product: Product): void {
    this.products.update(list => list.some(p => p.id === product.id) ? list.map(p => (p.id === product.id ? product : p)) : [...list, product]);
  }
}

function normalize(value: string): string {
  return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
}
