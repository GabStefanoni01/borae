import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext } from '../restaurant-context.service';
import { PanelDialogComponent, ToastService } from '../ui/feedback';
import { ConfirmDialogComponent, PageHeaderComponent, StateComponent } from '../ui/ui.components';
import { CatalogAction, catalogErrorMessage } from './catalog-errors';
import { MENU_STATUS_LABEL, Menu, nextDisplayOrder, optionalText } from './catalog.models';
import { CatalogService } from './catalog.service';
import { MenuFormComponent, MenuFormValue } from './menu-form.component';

type MenuAction = 'publish' | 'unpublish' | 'archive';

/** Cardápios do restaurante selecionado — GET/POST/PUT /menus e publish/unpublish/archive. */
@Component({
  selector: 'bo-menus-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent, PageHeaderComponent, StateComponent, ConfirmDialogComponent, PanelDialogComponent, MenuFormComponent],
  template: `
    <bo-page-header title="Seus cardápios" subtitle="Organize os cardápios do seu restaurante, atualize seus produtos e mantenha sua apresentação sempre alinhada com o que você oferece.">
      @if (ctx.canManageCatalog() && menus().length) {
        <button type="button" class="d-btn d-btn-primary" (click)="openCreate()"><bo-icon name="plus" [size]="16" /> Novo cardápio</button>
      }
    </bo-page-header>

    @if (!ctx.canManageCatalog() && ctx.role()) {
      <div class="d-alert d-alert-info c-role-note"><bo-icon name="info" [size]="16" />
        <p>Você está com acesso de <strong>Equipe</strong>: pode consultar os cardápios e marcar produtos como disponíveis ou indisponíveis.</p>
      </div>
    }

    @if (loading() && !loaded()) {
      <div class="c-menu-grid" aria-busy="true" aria-label="Carregando cardápios">
        @for (i of [1, 2, 3]; track i) { <div class="c-skeleton c-skeleton-card"></div> }
      </div>
    } @else if (loadError()) {
      <bo-state kind="error" title="Não foi possível carregar seus cardápios." [text]="loadError()">
        <button type="button" class="d-btn d-btn-secondary" (click)="load()"><bo-icon name="refresh" [size]="16" /> Tentar novamente</button>
      </bo-state>
    } @else if (!menus().length) {
      <bo-state kind="empty" icon="book-open" title="Seu primeiro cardápio começa aqui."
        [text]="ctx.canManageCatalog()
          ? 'Crie um cardápio para organizar categorias, cadastrar seus pratos e preparar a apresentação do seu restaurante.'
          : 'Este restaurante ainda não possui cardápios. Quando o proprietário ou um gerente criar um, ele aparecerá aqui.'">
        @if (ctx.canManageCatalog()) {
          <button type="button" class="d-btn d-btn-primary" (click)="openCreate()"><bo-icon name="plus" [size]="16" /> Criar primeiro cardápio</button>
        }
      </bo-state>
    } @else {
      <div class="c-menu-grid">
        @for (menu of activeMenus(); track menu.id) {
          <article class="c-menu-card" [attr.data-status]="menu.status">
            <header class="c-menu-card-head">
              <span class="c-menu-icon" aria-hidden="true"><bo-icon name="book-open" [size]="20" /></span>
              <span class="c-status" [attr.data-status]="menu.status"><span class="c-status-dot" aria-hidden="true"></span>{{ statusLabel[menu.status] }}</span>
            </header>
            <h2 class="c-menu-title"><a [routerLink]="[menu.id]" class="c-menu-link">{{ menu.name }}</a></h2>
            <p class="c-menu-desc">{{ menu.description || 'Sem descrição.' }}</p>
            <p class="c-menu-meta">
              @switch (categoryCount()[menu.id]) {
                @case (undefined) { <span class="c-skeleton c-skeleton-text"></span> }
                @case (-1) { <span>Categorias indisponíveis</span> }
                @case (0) { <span>Nenhuma categoria</span> }
                @case (1) { <span>1 categoria</span> }
                @default { <span>{{ categoryCount()[menu.id] }} categorias</span> }
              }
            </p>
            <footer class="c-menu-actions">
              <a [routerLink]="[menu.id]" class="d-btn d-btn-secondary d-btn-sm">
                <bo-icon [name]="ctx.canManageCatalog() ? 'sliders' : 'eye'" [size]="15" /> {{ ctx.canManageCatalog() ? 'Organizar' : 'Consultar' }}
              </a>
              @if (ctx.canManageCatalog()) {
                <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="openEdit(menu)" [disabled]="isBusy(menu.id)">Editar</button>
                @if (menu.status === 'DRAFT') {
                  <button type="button" class="d-btn d-btn-ghost d-btn-sm c-accent" (click)="run(menu, 'publish')" [disabled]="isBusy(menu.id)">
                    @if (busyAction()?.id === menu.id && busyAction()?.action === 'publish') { <bo-icon name="loader" [size]="14" [spin]="true" /> } Publicar
                  </button>
                } @else {
                  <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="run(menu, 'unpublish')" [disabled]="isBusy(menu.id)">
                    @if (busyAction()?.id === menu.id && busyAction()?.action === 'unpublish') { <bo-icon name="loader" [size]="14" [spin]="true" /> } Despublicar
                  </button>
                }
                <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="archiveTarget.set(menu)" [disabled]="isBusy(menu.id)">Arquivar</button>
              }
            </footer>
          </article>
        }
      </div>

      @if (archivedMenus().length) {
        <details class="c-archived">
          <summary>Arquivados ({{ archivedMenus().length }})</summary>
          <p class="c-archived-note">Cardápios arquivados não aparecem na página pública e não podem ser alterados. Os dados continuam preservados.</p>
          <ul class="c-archived-list">
            @for (menu of archivedMenus(); track menu.id) {
              <li>
                <span class="c-status" data-status="ARCHIVED"><span class="c-status-dot" aria-hidden="true"></span>Arquivado</span>
                <a [routerLink]="[menu.id]">{{ menu.name }}</a>
              </li>
            }
          </ul>
        </details>
      }
    }

    <bo-panel-dialog [open]="formOpen()" [title]="editing() ? 'Editar cardápio' : 'Novo cardápio'"
      [subtitle]="editing() ? 'Atualize as informações do seu cardápio.' : 'Crie um cardápio para organizar os produtos que seu restaurante oferece.'"
      (closed)="requestClose(form()?.dirty() ?? false)">
      @if (formOpen()) {
        <bo-menu-form [menu]="editing()" [busy]="saving()" [error]="formError()" (saved)="save($event)" (cancelled)="requestClose($event)" />
      }
    </bo-panel-dialog>

    <bo-confirm-dialog [open]="discardOpen()" title="Descartar alterações?" message="As alterações que você fez neste cardápio ainda não foram salvas."
      confirmLabel="Descartar" [danger]="true" (confirmed)="closeForm()" (cancelled)="discardOpen.set(false)" />

    <bo-confirm-dialog [open]="!!archiveTarget()" title="Arquivar este cardápio?"
      message="Ele deixará de aparecer na página pública, mas seus dados serão preservados." confirmLabel="Arquivar"
      [danger]="true" [busy]="!!busyAction()" (confirmed)="confirmArchive()" (cancelled)="archiveTarget.set(null)" />
  `
})
export class MenusPage {
  protected readonly ctx = inject(RestaurantContext);
  private readonly catalog = inject(CatalogService);
  private readonly toast = inject(ToastService);

  protected get statusLabel() { return MENU_STATUS_LABEL; }
  protected readonly menus = signal<Menu[]>([]);
  protected readonly loading = signal(false);
  protected readonly loaded = signal(false);
  protected readonly loadError = signal('');
  /** Quantidade de categorias por cardápio (-1 = não foi possível consultar). */
  protected readonly categoryCount = signal<Record<string, number>>({});

  protected readonly formOpen = signal(false);
  protected readonly editing = signal<Menu | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal('');
  protected readonly discardOpen = signal(false);
  protected readonly archiveTarget = signal<Menu | null>(null);
  protected readonly busyAction = signal<{ id: string; action: MenuAction } | null>(null);
  protected readonly form = viewChild(MenuFormComponent);

  protected readonly activeMenus = computed(() => this.menus().filter(m => m.status !== 'ARCHIVED'));
  protected readonly archivedMenus = computed(() => this.menus().filter(m => m.status === 'ARCHIVED'));

  constructor() {
    effect(() => {
      const id = this.ctx.selectedId();
      untracked(() => {
        this.menus.set([]);
        this.loaded.set(false);
        if (id) this.load();
      });
    });
  }

  load(): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId) return;
    this.loading.set(true);
    this.loadError.set('');
    this.catalog.listMenus(restaurantId).subscribe({
      next: menus => {
        if (restaurantId !== this.ctx.selectedId()) return;
        this.menus.set(menus);
        this.loaded.set(true);
        this.loading.set(false);
        this.loadCategoryCounts(restaurantId, menus);
      },
      error: error => {
        this.loading.set(false);
        // Erro não é tratado como lista vazia.
        this.loadError.set(catalogErrorMessage(error, 'load-menus'));
      }
    });
  }

  protected isBusy(menuId: string): boolean {
    return this.busyAction()?.id === menuId;
  }

  protected openCreate(): void {
    this.editing.set(null);
    this.formError.set('');
    this.formOpen.set(true);
  }

  protected openEdit(menu: Menu): void {
    this.editing.set(menu);
    this.formError.set('');
    this.formOpen.set(true);
  }

  protected requestClose(dirty: boolean): void {
    if (this.saving()) return;
    if (dirty) this.discardOpen.set(true); else this.closeForm();
  }

  protected closeForm(): void {
    this.discardOpen.set(false);
    this.formOpen.set(false);
    this.editing.set(null);
  }

  protected save(value: MenuFormValue): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId) return;
    const editing = this.editing();
    this.saving.set(true);
    this.formError.set('');
    const request$ = editing
      ? this.catalog.updateMenu(restaurantId, editing.id, { name: value.name, description: optionalText(value.description), displayOrder: null })
      : this.catalog.createMenu(restaurantId, { name: value.name, description: optionalText(value.description), displayOrder: nextDisplayOrder(this.menus()) });
    request$.subscribe({
      next: menu => {
        this.saving.set(false);
        this.replace(menu);
        if (!editing) this.categoryCount.update(c => ({ ...c, [menu.id]: 0 }));
        this.closeForm();
        this.toast.success(editing ? 'Alterações salvas com sucesso.' : 'Cardápio criado com sucesso.');
      },
      error: error => {
        this.saving.set(false);
        this.formError.set(catalogErrorMessage(error, 'save-menu'));
      }
    });
  }

  protected run(menu: Menu, action: MenuAction): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId || this.busyAction()) return;
    this.busyAction.set({ id: menu.id, action });
    const request$ = action === 'publish'
      ? this.catalog.publishMenu(restaurantId, menu.id)
      : action === 'unpublish' ? this.catalog.unpublishMenu(restaurantId, menu.id) : this.catalog.archiveMenu(restaurantId, menu.id);
    const errorAction: CatalogAction = action === 'publish' ? 'publish-menu' : action === 'unpublish' ? 'unpublish-menu' : 'archive-menu';
    request$.subscribe({
      next: updated => {
        this.busyAction.set(null);
        this.archiveTarget.set(null);
        this.replace(updated);
        this.toast.success(action === 'publish' ? 'Cardápio publicado.' : action === 'unpublish' ? 'Cardápio despublicado. Ele voltou para rascunho.' : 'Cardápio arquivado.');
      },
      error: error => {
        this.busyAction.set(null);
        this.archiveTarget.set(null);
        this.toast.error(catalogErrorMessage(error, errorAction));
      }
    });
  }

  protected confirmArchive(): void {
    const menu = this.archiveTarget();
    if (menu) this.run(menu, 'archive');
  }

  private replace(menu: Menu): void {
    this.menus.update(list => {
      const exists = list.some(m => m.id === menu.id);
      const next = exists ? list.map(m => (m.id === menu.id ? menu : m)) : [...list, menu];
      return [...next].sort((a, b) => a.displayOrder - b.displayOrder || a.name.localeCompare(b.name, 'pt-BR'));
    });
  }

  /** A API de cardápios não traz a contagem; consulta as categorias de cada cardápio ativo. */
  private loadCategoryCounts(restaurantId: string, menus: Menu[]): void {
    const active = menus.filter(m => m.status !== 'ARCHIVED');
    if (!active.length) return;
    forkJoin(active.map(menu => this.catalog.listCategories(restaurantId, menu.id).pipe(
      map(categories => [menu.id, categories.length] as const),
      catchError(() => of([menu.id, -1] as const))
    ))).subscribe(entries => {
      if (restaurantId !== this.ctx.selectedId()) return;
      this.categoryCount.set(Object.fromEntries(entries));
    });
  }
}
