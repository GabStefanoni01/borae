import { HttpEventType } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { IconComponent } from '../../shared/icon.component';
import { centsToInput, formatCents, formatDelta, parseBrlToCents } from '../../shared/money';
import { RestaurantContext } from '../restaurant-context.service';
import { ToastService } from '../ui/feedback';
import { ConfirmDialogComponent, StateComponent } from '../ui/ui.components';
import { catalogErrorMessage, httpStatus } from './catalog-errors';
import { CATALOG_LIMITS, Category, Menu, Product, ProductSaveRequest, moveItem, nextDisplayOrder, optionalText } from './catalog.models';
import { CatalogService } from './catalog.service';
import { ProductModel3dComponent } from './product-model-3d.component';

interface AddonRow { key: number; name: string; price: string; active: boolean; }
interface IngredientRow { key: number; name: string; }

/** Formulário de produto — POST/PUT /products e imagem (POST/DELETE /products/{id}/image). */
@Component({
  selector: 'bo-product-editor-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, IconComponent, StateComponent, ConfirmDialogComponent, ProductModel3dComponent],
  template: `
    <nav class="d-breadcrumb c-crumb" aria-label="Você está em">
      <a routerLink="/app/cardapios">Cardápios</a><span aria-hidden="true">/</span>
      <a [routerLink]="['/app/cardapios', menuId()]">{{ menu()?.name ?? 'Cardápio' }}</a><span aria-hidden="true">/</span>
      <span aria-current="page">{{ isNew() ? 'Novo produto' : (product()?.name ?? 'Produto') }}</span>
    </nav>

    @if (!ctx.canManageCatalog()) {
      <bo-state kind="forbidden" title="Edição disponível para proprietário e gerentes" text="Com acesso de Equipe, você pode consultar o cardápio e alterar somente a disponibilidade dos produtos.">
        <a [routerLink]="['/app/cardapios', menuId()]" class="d-btn d-btn-secondary"><bo-icon name="arrow-left" [size]="16" /> Voltar ao cardápio</a>
      </bo-state>
    } @else if (loading()) {
      <div class="c-skeleton c-skeleton-hero" aria-busy="true" aria-label="Carregando produto"></div>
    } @else if (loadError()) {
      <bo-state kind="error" title="Não foi possível carregar o produto." [text]="loadError()">
        <button type="button" class="d-btn d-btn-secondary" (click)="load()"><bo-icon name="refresh" [size]="16" /> Tentar novamente</button>
      </bo-state>
    } @else if (!isNew() && !product()) {
      <bo-state kind="not-found" title="Este conteúdo não está mais disponível." text="O produto pode ter sido removido deste cardápio.">
        <a [routerLink]="['/app/cardapios', menuId()]" class="d-btn d-btn-secondary"><bo-icon name="arrow-left" [size]="16" /> Voltar ao cardápio</a>
      </bo-state>
    } @else if (readOnlyReason(); as reason) {
      <bo-state kind="forbidden" icon="lock" title="Este produto não pode ser editado agora" [text]="reason">
        <a [routerLink]="['/app/cardapios', menuId()]" class="d-btn d-btn-secondary"><bo-icon name="arrow-left" [size]="16" /> Voltar ao cardápio</a>
      </bo-state>
    } @else if (!categories().length) {
      <bo-state kind="empty" icon="tag" title="Crie uma categoria primeiro" text="Todo produto pertence a uma categoria do cardápio.">
        <a [routerLink]="['/app/cardapios', menuId()]" class="d-btn d-btn-primary">Organizar categorias</a>
      </bo-state>
    } @else {
      <header class="c-form-header">
        <h1>{{ isNew() ? 'Adicionar produto' : 'Editar produto' }}</h1>
        <p>Apresente seu produto com informações claras, preço e detalhes que ajudem o cliente a conhecer melhor o prato.</p>
      </header>

      <form class="c-product-form" (ngSubmit)="save()" novalidate>
        <div class="c-product-main">
          <section class="d-card">
            <div class="d-card-head"><span class="d-card-icon"><bo-icon name="utensils" [size]="18" /></span><h2>Informações do produto</h2></div>
            <div class="d-form">
              <div class="d-field">
                <label for="pe-name">Nome</label>
                <input id="pe-name" name="name" class="d-input" [(ngModel)]="name" (ngModelChange)="touch()" required [maxlength]="limits.productName" autocomplete="off"
                       placeholder="Ex.: Hambúrguer da casa" [attr.aria-invalid]="(submitted() && !name.trim()) || null">
                @if (submitted() && !name.trim()) { <span class="c-field-error" role="alert"><bo-icon name="alert" [size]="14" /> Informe o nome do produto.</span> }
                <span class="d-hint"><span></span><span>{{ name.length }}/{{ limits.productName }}</span></span>
              </div>
              <div class="d-field">
                <label for="pe-desc">Descrição <span>(opcional)</span></label>
                <textarea id="pe-desc" name="description" class="d-input" rows="3" [(ngModel)]="description" (ngModelChange)="touch()" [maxlength]="limits.productDescription"
                          placeholder="Ex.: Blend artesanal, queijo cheddar, cebola caramelizada e molho especial da casa."></textarea>
                <span class="d-hint"><span></span><span>{{ description.length }}/{{ limits.productDescription }}</span></span>
              </div>
              <div class="d-form-row">
                <div class="d-field">
                  <label for="pe-price">Preço</label>
                  <div class="c-money">
                    <span aria-hidden="true">R$</span>
                    <input id="pe-price" name="price" class="d-input" inputmode="decimal" [(ngModel)]="price" (ngModelChange)="touch()" (blur)="formatPrice()" required
                           placeholder="0,00" autocomplete="off" [attr.aria-invalid]="priceInvalid() || null" aria-describedby="pe-price-hint">
                  </div>
                  @if (priceInvalid()) { <span class="c-field-error" role="alert"><bo-icon name="alert" [size]="14" /> Informe um preço válido, por exemplo 25,90.</span> }
                  <span class="d-hint" id="pe-price-hint"><span>Valor cobrado do cliente, em reais.</span></span>
                </div>
                <div class="d-field">
                  <label for="pe-cat">Categoria</label>
                  <select id="pe-cat" name="category" class="d-input" [(ngModel)]="categoryId" (ngModelChange)="touch()" required>
                    @for (c of categories(); track c.id) { <option [value]="c.id">{{ c.name }}{{ c.active ? '' : ' (inativa)' }}</option> }
                  </select>
                </div>
              </div>
            </div>
          </section>

          <section class="d-card">
            <div class="d-card-head"><span class="d-card-icon"><bo-icon name="leaf" [size]="18" /></span><h2>Ingredientes</h2></div>
            <p class="d-card-text">Informe os ingredientes que compõem este produto. Eles serão apresentados ao consumidor como informações descritivas do prato.</p>
            @if (ingredients().length) {
              <ol class="c-rows">
                @for (row of ingredients(); track row.key; let i = $index, first = $first, last = $last) {
                  <li class="c-row">
                    <label class="sr-only" [for]="'pe-ing-' + row.key">Ingrediente {{ i + 1 }}</label>
                    <input [id]="'pe-ing-' + row.key" class="d-input d-input-sm c-row-input" [name]="'ing-' + row.key" [(ngModel)]="row.name" (ngModelChange)="touch()" [maxlength]="limits.ingredientName">
                    <div class="c-row-tools">
                      <button type="button" class="d-icon-btn" (click)="moveIngredient(i, -1)" [disabled]="first" [attr.aria-label]="'Mover ' + (row.name || 'ingrediente') + ' para cima'"><bo-icon name="chevron-down" [size]="16" class="c-rot" /></button>
                      <button type="button" class="d-icon-btn" (click)="moveIngredient(i, 1)" [disabled]="last" [attr.aria-label]="'Mover ' + (row.name || 'ingrediente') + ' para baixo'"><bo-icon name="chevron-down" [size]="16" /></button>
                      <button type="button" class="d-icon-btn" (click)="removeIngredient(i)" [attr.aria-label]="'Remover ' + (row.name || 'ingrediente')"><bo-icon name="x" [size]="16" /></button>
                    </div>
                  </li>
                }
              </ol>
            } @else {
              <p class="c-empty-line">Nenhum ingrediente informado. Você pode salvar o produto sem ingredientes.</p>
            }
            <div class="c-add-line">
              <label class="sr-only" for="pe-new-ing">Novo ingrediente</label>
              <input id="pe-new-ing" name="newIngredient" class="d-input d-input-sm" [(ngModel)]="newIngredient" [maxlength]="limits.ingredientName"
                     placeholder="Ex.: Pão artesanal" (keydown.enter)="$event.preventDefault(); addIngredient()" autocomplete="off">
              <button type="button" class="d-btn d-btn-secondary d-btn-sm" (click)="addIngredient()" [disabled]="ingredients().length >= limits.maxIngredients">
                <bo-icon name="plus" [size]="15" /> Adicionar ingrediente
              </button>
            </div>
            <p class="d-hint"><span>Até {{ limits.maxIngredients }} ingredientes. Ingredientes são informativos e não representam declaração de alergênicos.</span></p>
          </section>

          <section class="d-card">
            <div class="d-card-head"><span class="d-card-icon"><bo-icon name="layers" [size]="18" /></span><h2>Adicionais</h2></div>
            <p class="d-card-text">Apresente opções complementares e informe quanto cada uma acrescenta ao preço do produto.</p>
            @if (addons().length) {
              <ol class="c-rows">
                @for (row of addons(); track row.key; let i = $index, first = $first, last = $last) {
                  <li class="c-row c-addon-row" [class.inactive]="!row.active">
                    <div class="c-addon-fields">
                      <label class="sr-only" [for]="'pe-ad-' + row.key">Nome do adicional {{ i + 1 }}</label>
                      <input [id]="'pe-ad-' + row.key" class="d-input d-input-sm" [name]="'ad-' + row.key" [(ngModel)]="row.name" (ngModelChange)="touch()" [maxlength]="limits.addonName" placeholder="Ex.: Cheddar">
                      <div class="c-money c-money-sm">
                        <span aria-hidden="true">+ R$</span>
                        <label class="sr-only" [for]="'pe-adp-' + row.key">Acréscimo de {{ row.name || 'adicional' }}</label>
                        <input [id]="'pe-adp-' + row.key" class="d-input d-input-sm" [name]="'adp-' + row.key" inputmode="decimal" [(ngModel)]="row.price" (ngModelChange)="touch()" (blur)="formatAddonPrice(row)" placeholder="0,00"
                               [attr.aria-invalid]="addonPriceInvalid(row) || null">
                      </div>
                      <label class="c-check c-check-sm"><input type="checkbox" [name]="'ada-' + row.key" [(ngModel)]="row.active" (ngModelChange)="touch()"> Ativo</label>
                    </div>
                    <div class="c-row-tools">
                      <button type="button" class="d-icon-btn" (click)="moveAddon(i, -1)" [disabled]="first" [attr.aria-label]="'Mover ' + (row.name || 'adicional') + ' para cima'"><bo-icon name="chevron-down" [size]="16" class="c-rot" /></button>
                      <button type="button" class="d-icon-btn" (click)="moveAddon(i, 1)" [disabled]="last" [attr.aria-label]="'Mover ' + (row.name || 'adicional') + ' para baixo'"><bo-icon name="chevron-down" [size]="16" /></button>
                      <button type="button" class="d-icon-btn" (click)="removeAddon(i)" [attr.aria-label]="'Remover ' + (row.name || 'adicional')"><bo-icon name="x" [size]="16" /></button>
                    </div>
                    @if (addonPreview(row); as text) { <span class="c-addon-preview">{{ text }}</span> }
                  </li>
                }
              </ol>
            } @else {
              <p class="c-empty-line">Nenhum adicional cadastrado.</p>
            }
            <button type="button" class="d-btn d-btn-secondary d-btn-sm" (click)="addAddon()" [disabled]="addons().length >= limits.maxAddons"><bo-icon name="plus" [size]="15" /> Adicionar adicional</button>
            <p class="d-hint"><span>Adicionais inativos ficam salvos, mas não aparecem no cardápio público. O cliente apenas visualiza as opções; não há seleção nem pedido.</span></p>
          </section>

          <bo-product-model-3d [menuId]="menuId()" [product]="product()" />
        </div>

        <aside class="c-product-side">
          <section class="d-card">
            <div class="d-card-head"><span class="d-card-icon"><bo-icon name="image" [size]="18" /></span><h2>Imagem do produto</h2></div>
            <p class="d-card-text">Uma boa imagem ajuda o cliente a conhecer melhor o que seu restaurante oferece.</p>
            <div class="c-image-box">
              @if (imagePreview()) {
                <img [src]="imagePreview()" alt="Prévia da imagem do produto" width="640" height="480">
              } @else {
                <span class="c-image-empty"><bo-icon name="image" [size]="28" /> Nenhuma imagem adicionada.</span>
              }
              @if (uploadProgress() !== null) {
                <div class="c-upload" role="status">
                  <span>Enviando imagem... {{ uploadProgress() }}%</span>
                  <span class="c-upload-bar"><span [style.width.%]="uploadProgress()"></span></span>
                </div>
              }
            </div>
            @if (imageError()) { <p class="c-field-error" role="alert"><bo-icon name="alert" [size]="14" /> {{ imageError() }}</p> }
            <div class="c-image-actions">
              <label class="d-btn d-btn-secondary d-btn-sm c-file" [class.disabled]="imageBusy()">
                <bo-icon name="camera" [size]="15" /> {{ imagePreview() ? 'Trocar imagem' : 'Adicionar imagem' }}
                <input type="file" accept="image/jpeg,image/png,image/webp" (change)="pickImage($event)" [disabled]="imageBusy()">
              </label>
              @if (imagePreview()) {
                <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="removeImage()" [disabled]="imageBusy()">Remover</button>
              }
            </div>
            <p class="d-hint"><span>JPEG, PNG ou WebP com até 10 MB.{{ isNew() ? ' A imagem é enviada depois que o produto for salvo.' : '' }}</span></p>
          </section>

          <section class="d-card c-summary">
            <h2>Resumo</h2>
            <dl>
              <div><dt>Preço</dt><dd>{{ pricePreview() }}</dd></div>
              <div><dt>Ingredientes</dt><dd>{{ ingredients().length }}</dd></div>
              <div><dt>Adicionais ativos</dt><dd>{{ activeAddonCount() }}</dd></div>
            </dl>
          </section>
        </aside>

        <div class="c-sticky-actions">
          @if (formError()) { <div class="d-alert d-alert-error" role="alert"><bo-icon name="alert" [size]="16" /><p>{{ formError() }}</p></div> }
          <div class="d-form-actions c-form-end">
            <a class="d-btn d-btn-secondary" [routerLink]="['/app/cardapios', menuId()]" [queryParams]="{ categoria: categoryId || null }">Cancelar</a>
            <button type="submit" class="d-btn d-btn-primary" [disabled]="saving()">
              @if (saving()) { <bo-icon name="loader" [size]="16" [spin]="true" /> } Salvar produto
            </button>
          </div>
        </div>
      </form>
    }

    <bo-confirm-dialog [open]="leaveOpen()" title="Descartar alterações?" message="As alterações deste produto ainda não foram salvas."
      confirmLabel="Descartar" [danger]="true" (confirmed)="resolveLeave(true)" (cancelled)="resolveLeave(false)" />
  `
})
export class ProductEditorPage {
  readonly menuId = input.required<string>();
  /** Ausente em /produtos/novo. */
  readonly productId = input<string | undefined>(undefined);
  /** Categoria pré-selecionada (?categoria=). */
  readonly categoria = input<string | undefined>(undefined);

  protected readonly ctx = inject(RestaurantContext);
  private readonly catalog = inject(CatalogService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected get limits() { return CATALOG_LIMITS; }
  protected readonly menu = signal<Menu | null>(null);
  protected readonly categories = signal<Category[]>([]);
  private readonly allProducts = signal<Product[]>([]);
  protected readonly product = signal<Product | null>(null);
  protected readonly loading = signal(false);
  protected readonly loadError = signal('');

  protected name = '';
  protected description = '';
  protected price = '';
  protected categoryId = '';
  protected newIngredient = '';
  protected readonly ingredients = signal<IngredientRow[]>([]);
  protected readonly addons = signal<AddonRow[]>([]);
  private readonly version = signal(0);
  private baseline = '';
  private keySeq = 0;

  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly formError = signal('');

  private readonly pendingFile = signal<File | null>(null);
  private readonly localPreview = signal<string | null>(null);
  protected readonly uploadProgress = signal<number | null>(null);
  protected readonly imageBusy = signal(false);
  protected readonly imageError = signal('');

  protected readonly leaveOpen = signal(false);
  private leaveResolver?: (leave: boolean) => void;
  private saved = false;

  protected readonly isNew = computed(() => !this.productId());
  protected readonly readOnlyReason = computed(() => {
    if (this.menu()?.status === 'ARCHIVED') return 'O cardápio está arquivado e não pode mais ser alterado.';
    if (this.product()?.lifecycleStatus === 'ARCHIVED') return 'Produto arquivado não pode ser editado. Restaure-o na lista de produtos do cardápio para voltar a editá-lo.';
    return null;
  });
  protected readonly imagePreview = computed(() => this.localPreview() ?? this.product()?.imageUrl ?? null);
  protected readonly priceInvalid = computed(() => { this.version(); return this.submitted() && parseBrlToCents(this.price) === null; });
  protected readonly pricePreview = computed(() => { this.version(); const c = parseBrlToCents(this.price); return c === null ? '—' : formatCents(c); });
  protected readonly activeAddonCount = computed(() => { this.version(); return this.addons().filter(a => a.active).length; });

  constructor() {
    effect(() => {
      const restaurantId = this.ctx.selectedId();
      this.menuId(); this.productId();
      untracked(() => { if (restaurantId) this.load(); });
    });
    inject(DestroyRef).onDestroy(() => this.revokePreview());
  }

  load(): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId || !this.ctx.canManageCatalog()) return;
    this.loading.set(true);
    this.loadError.set('');
    forkJoin({
      menu: this.catalog.getMenu(restaurantId, this.menuId()),
      categories: this.catalog.listCategories(restaurantId, this.menuId()),
      products: this.catalog.listProducts(restaurantId, this.menuId())
    }).subscribe({
      next: ({ menu, categories, products }) => {
        this.menu.set(menu);
        this.categories.set([...categories].sort((a, b) => a.displayOrder - b.displayOrder));
        this.allProducts.set(products);
        const product = this.productId() ? products.find(p => p.id === this.productId()) ?? null : null;
        this.product.set(product);
        this.fillForm(product, categories);
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        if (httpStatus(error) === 404) { this.menu.set(null); this.product.set(null); return; }
        this.loadError.set(catalogErrorMessage(error, 'load-products'));
      }
    });
  }

  /** Usado pelo canDeactivate da rota: pede confirmação se houver alterações não salvas. */
  canLeave(): boolean | Promise<boolean> {
    if (this.saved || !this.isDirty()) return true;
    this.leaveOpen.set(true);
    return new Promise(resolve => (this.leaveResolver = resolve));
  }

  protected resolveLeave(leave: boolean): void {
    this.leaveOpen.set(false);
    this.leaveResolver?.(leave);
    this.leaveResolver = undefined;
  }

  protected touch(): void { this.version.update(v => v + 1); }

  protected formatPrice(): void {
    const cents = parseBrlToCents(this.price);
    if (cents !== null) { this.price = centsToInput(cents); this.touch(); }
  }

  protected formatAddonPrice(row: AddonRow): void {
    const cents = parseBrlToCents(row.price);
    if (cents !== null) { row.price = centsToInput(cents); this.touch(); }
  }

  protected addonPriceInvalid(row: AddonRow): boolean {
    this.version();
    return this.submitted() && parseBrlToCents(row.price) === null;
  }

  protected addonPreview(row: AddonRow): string {
    const cents = parseBrlToCents(row.price);
    return row.name.trim() && cents !== null ? `${row.name.trim()} — ${formatDelta(cents)}` : '';
  }

  // ---------- Ingredientes ----------
  protected addIngredient(): void {
    const name = this.newIngredient.trim();
    if (!name || this.ingredients().length >= CATALOG_LIMITS.maxIngredients) return;
    this.ingredients.update(list => [...list, { key: ++this.keySeq, name }]);
    this.newIngredient = '';
    this.touch();
  }
  protected removeIngredient(index: number): void {
    this.ingredients.update(list => list.filter((_, i) => i !== index));
    this.touch();
  }
  protected moveIngredient(index: number, direction: -1 | 1): void {
    this.ingredients.update(list => moveItem(list, index, direction));
    this.touch();
  }

  // ---------- Adicionais ----------
  protected addAddon(): void {
    if (this.addons().length >= CATALOG_LIMITS.maxAddons) return;
    this.addons.update(list => [...list, { key: ++this.keySeq, name: '', price: '', active: true }]);
    this.touch();
  }
  protected removeAddon(index: number): void {
    this.addons.update(list => list.filter((_, i) => i !== index));
    this.touch();
  }
  protected moveAddon(index: number, direction: -1 | 1): void {
    this.addons.update(list => moveItem(list, index, direction));
    this.touch();
  }

  // ---------- Imagem ----------
  protected pickImage(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    const file = inputEl.files?.[0];
    inputEl.value = '';
    if (!file) return;
    const problem = validateImage(file);
    if (problem) { this.imageError.set(problem); return; }
    this.imageError.set('');
    if (this.isNew()) {
      this.revokePreview();
      this.pendingFile.set(file);
      this.localPreview.set(URL.createObjectURL(file));
      this.touch();
    } else {
      this.upload(this.product()!.id, file, false);
    }
  }

  protected removeImage(): void {
    if (this.isNew()) {
      this.revokePreview();
      this.pendingFile.set(null);
      this.touch();
      return;
    }
    const restaurantId = this.ctx.selectedId();
    const product = this.product();
    if (!restaurantId || !product) return;
    this.imageBusy.set(true);
    this.imageError.set('');
    this.catalog.removeProductImage(restaurantId, this.menuId(), product.id).subscribe({
      next: () => {
        this.imageBusy.set(false);
        this.product.set({ ...product, imageUrl: null });
        this.toast.success('Imagem removida.');
      },
      error: error => {
        this.imageBusy.set(false);
        // 404: o produto já não tinha imagem — a tela passa a refletir isso.
        if (httpStatus(error) === 404) this.product.set({ ...product, imageUrl: null });
        this.imageError.set(catalogErrorMessage(error, 'remove-image'));
      }
    });
  }

  private upload(productId: string, file: File, afterCreate: boolean): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId) return;
    this.imageBusy.set(true);
    this.imageError.set('');
    this.uploadProgress.set(0);
    this.catalog.uploadProductImage(restaurantId, this.menuId(), productId, file).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress) {
          this.uploadProgress.set(event.total ? Math.round((event.loaded / event.total) * 100) : 0);
        } else if (event.type === HttpEventType.Response && event.body) {
          this.uploadProgress.set(null);
          this.imageBusy.set(false);
          this.product.set(event.body);
          this.revokePreview();
          this.pendingFile.set(null);
          this.toast.success('Imagem atualizada com sucesso.');
          if (afterCreate) this.finish();
        }
      },
      error: error => {
        this.uploadProgress.set(null);
        this.imageBusy.set(false);
        const message = catalogErrorMessage(error, 'upload-image');
        if (afterCreate) {
          // O produto já foi criado: segue para a edição dele, onde a imagem pode ser reenviada.
          this.toast.error(`Produto salvo, mas a imagem não foi enviada. ${message}`);
          this.saved = true;
          void this.router.navigate(['/app/cardapios', this.menuId(), 'produtos', productId]);
        } else {
          this.imageError.set(message);
        }
      }
    });
  }

  // ---------- Gravação ----------
  protected save(): void {
    this.submitted.set(true);
    this.formError.set('');
    const request = this.buildRequest();
    if (typeof request === 'string') { this.formError.set(request); return; }
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId || this.saving()) return;
    this.saving.set(true);
    const existing = this.product();
    const request$ = existing
      ? this.catalog.updateProduct(restaurantId, this.menuId(), existing.id, request)
      : this.catalog.createProduct(restaurantId, this.menuId(), request);
    request$.subscribe({
      next: saved => {
        this.saving.set(false);
        this.product.set(saved);
        this.toast.success('Produto salvo com sucesso.');
        const file = this.pendingFile();
        if (!existing && file) { this.upload(saved.id, file, true); return; }
        this.finish();
      },
      error: error => {
        this.saving.set(false);
        this.formError.set(catalogErrorMessage(error, 'save-product'));
      }
    });
  }

  /**
   * Monta o payload em centavos inteiros. As verificações abaixo só antecipam erros comuns;
   * o backend continua validando e normalizando tudo.
   */
  private buildRequest(): ProductSaveRequest | string {
    const name = this.name.trim();
    if (!name) return 'Informe o nome do produto.';
    const priceInCents = parseBrlToCents(this.price);
    if (priceInCents === null) return 'Informe um preço válido, por exemplo 25,90.';
    if (!this.categoryId) return 'Selecione a categoria do produto.';

    const ingredients = this.ingredients().map(i => i.name.trim());
    if (ingredients.some(i => !i)) return 'Preencha ou remova os ingredientes em branco.';
    if (hasDuplicates(ingredients)) return 'Não repita ingredientes no mesmo produto.';

    const addons = [];
    for (const row of this.addons()) {
      const addonName = row.name.trim();
      const cents = parseBrlToCents(row.price);
      if (!addonName) return 'Preencha o nome de todos os adicionais ou remova os que estiverem em branco.';
      if (cents === null) return `Informe um acréscimo válido para "${addonName}", por exemplo 3,00.`;
      addons.push({ name: addonName, priceDeltaInCents: cents, active: row.active });
    }
    if (hasDuplicates(addons.map(a => a.name))) return 'Não repita adicionais no mesmo produto.';

    const existing = this.product();
    const sameCategory = existing && existing.categoryId === this.categoryId;
    const siblings = this.allProducts().filter(p => p.categoryId === this.categoryId && p.id !== existing?.id);
    return {
      categoryId: this.categoryId,
      name,
      description: optionalText(this.description),
      priceInCents,
      displayOrder: sameCategory ? existing.displayOrder : nextDisplayOrder(siblings),
      ingredients,
      addons
    };
  }

  private finish(): void {
    this.saved = true;
    void this.router.navigate(['/app/cardapios', this.menuId()], { queryParams: { categoria: this.product()?.categoryId ?? this.categoryId } });
  }

  private fillForm(product: Product | null, categories: Category[]): void {
    this.name = product?.name ?? '';
    this.description = product?.description ?? '';
    this.price = product ? centsToInput(product.priceInCents) : '';
    const preferred = product?.categoryId ?? this.categoria();
    this.categoryId = categories.some(c => c.id === preferred) ? preferred! : (categories[0]?.id ?? '');
    this.ingredients.set((product?.ingredients ?? []).map(name => ({ key: ++this.keySeq, name })));
    this.addons.set((product?.addons ?? []).map(a => ({ key: ++this.keySeq, name: a.name, price: centsToInput(a.priceDeltaInCents), active: a.active })));
    this.submitted.set(false);
    this.saved = false;
    this.baseline = this.snapshot();
    this.touch();
  }

  private snapshot(): string {
    return JSON.stringify([this.name.trim(), this.description.trim(), this.price.trim(), this.categoryId,
      this.ingredients().map(i => i.name.trim()), this.addons().map(a => [a.name.trim(), a.price.trim(), a.active]), !!this.pendingFile()]);
  }

  private isDirty(): boolean {
    return !this.loading() && this.ctx.canManageCatalog() && !this.readOnlyReason() && this.snapshot() !== this.baseline;
  }

  private revokePreview(): void {
    const url = this.localPreview();
    if (url) URL.revokeObjectURL(url);
    this.localPreview.set(null);
  }
}

/** Antecipação amigável dos limites do backend (JPEG, PNG ou WebP, até 10 MB). */
export function validateImage(file: File): string | null {
  const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
  const typeOk = !file.type || (CATALOG_LIMITS.imageTypes as readonly string[]).includes(file.type);
  if (!typeOk || !(CATALOG_LIMITS.imageExtensions as readonly string[]).includes(extension)) {
    return 'Formato não aceito. Envie uma imagem JPEG, PNG ou WebP.';
  }
  if (file.size > CATALOG_LIMITS.imageBytes) return 'A imagem do produto pode ter no máximo 10 MB.';
  if (file.size === 0) return 'O arquivo selecionado está vazio.';
  return null;
}

function hasDuplicates(values: string[]): boolean {
  const normalized = values.map(v => v.normalize('NFD').replace(/\p{M}/gu, '').replace(/\s+/g, ' ').trim().toLowerCase());
  return new Set(normalized).size !== normalized.length;
}
