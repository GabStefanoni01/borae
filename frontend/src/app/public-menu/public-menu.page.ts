import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy, Component, ElementRef, computed, effect, inject, input, signal, untracked, viewChild
} from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { BrandLogoComponent } from '../shared/brand-logo.component';
import { IconComponent } from '../shared/icon.component';
import { MenuProductCardComponent } from '../shared/menu-product-card.component';
import { formatCents } from '../shared/money';
import { PublicAssistantDrawerComponent } from './public-assistant-drawer.component';
import { PublicMenu, PublicMenuService, PublicProduct, PublicRestaurantMenu } from './public-menu.service';

type LoadState = 'loading' | 'ready' | 'not-found' | 'error';

/**
 * Cardápio digital público do restaurante (/r/:slug), aberto também pelo QR Code principal.
 * Sem login, sem pedido: apresenta o que GET /api/v1/public/restaurants/{slug}/menu devolve.
 */
@Component({
  selector: 'bo-public-menu-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, BrandLogoComponent, IconComponent, MenuProductCardComponent, PublicAssistantDrawerComponent],
  styleUrls: ['./public-menu.page.css', './public-menu.extras.css'],
  template: `
    <div class="pm">
      <header class="pm-bar">
        <a routerLink="/" class="pm-brand" aria-label="Boraê — Início"><bo-brand-logo tone="light" [size]="26" /></a>
        <span class="pm-bar-tag">Cardápio digital</span>
      </header>

      @switch (state()) {
        @case ('loading') {
          <main class="pm-main" aria-busy="true" aria-label="Carregando cardápio">
            <div class="pm-skel pm-skel-hero"></div>
            <div class="pm-skel pm-skel-chips"></div>
            @for (i of [1, 2, 3, 4]; track i) { <div class="pm-skel pm-skel-row"></div> }
          </main>
        }
        @case ('not-found') {
          <main class="pm-main pm-center">
            <span class="pm-state-icon"><bo-icon name="search" [size]="28" /></span>
            <h1>Não foi possível encontrar este restaurante.</h1>
            <p>A página pode não estar disponível no momento ou o endereço pode estar incorreto.</p>
            <a routerLink="/" class="pm-btn pm-btn-ghost">Conhecer o Boraê</a>
          </main>
        }
        @case ('error') {
          <main class="pm-main pm-center" role="alert">
            <span class="pm-state-icon"><bo-icon name="alert" [size]="28" /></span>
            <h1>Não foi possível carregar o cardápio.</h1>
            <p>{{ errorText() }}</p>
            <button type="button" class="pm-btn" (click)="load()"><bo-icon name="refresh" [size]="16" /> Tentar novamente</button>
          </main>
        }
        @default {
          @if (data(); as r) {
            <main class="pm-main">
              <section class="pm-hero" aria-labelledby="pm-name">
                @if (r.logoUrl && !logoFailed()) {
                  <img class="pm-logo" [src]="r.logoUrl" [alt]="'Logo de ' + r.name" loading="eager" decoding="async" (error)="logoFailed.set(true)">
                } @else {
                  <span class="pm-avatar" aria-hidden="true">{{ initials() }}</span>
                }
                <div class="pm-hero-text">
                  @if (r.category) { <span class="pm-kicker">{{ r.category }}</span> }
                  <h1 id="pm-name">{{ r.name }}</h1>
                  @if (r.description) { <p>{{ r.description }}</p> }
                  @if (contacts().length) {
                    <ul class="pm-contacts">
                      @for (c of contacts(); track c.label) {
                        <li><a [href]="c.href" target="_blank" rel="noopener"><bo-icon [name]="c.icon" [size]="14" /> {{ c.label }}</a></li>
                      }
                    </ul>
                  }
                </div>
              </section>

              @if (!r.menus.length) {
                <section class="pm-center pm-empty">
                  <span class="pm-state-icon"><bo-icon name="book-open" [size]="26" /></span>
                  <h2>Este restaurante ainda não possui cardápios disponíveis.</h2>
                  <p>Volte em outro momento para conferir as novidades.</p>
                </section>
              } @else {
                @if (r.menus.length > 1) {
                  <nav class="pm-menus" aria-label="Cardápios disponíveis">
                    @for (m of r.menus; track m.id) {
                      <button type="button" class="pm-menu-tab" [class.active]="m.id === selectedMenu()?.id" [attr.aria-pressed]="m.id === selectedMenu()?.id" (click)="selectMenu(m.id)">{{ m.name }}</button>
                    }
                  </nav>
                }

                @if (selectedMenu(); as menu) {
                  <div class="pm-menu-head">
                    <h2>{{ menu.name }}</h2>
                    @if (menu.description) { <p>{{ menu.description }}</p> }
                  </div>

                  @if (!visibleCategories().length) {
                    <section class="pm-center pm-empty">
                      <span class="pm-state-icon"><bo-icon name="utensils" [size]="24" /></span>
                      <h2>Este cardápio ainda não tem produtos disponíveis para consulta.</h2>
                      <p>Volte em outro momento para conferir as novidades.</p>
                    </section>
                  } @else {
                    @if (visibleCategories().length > 1) {
                      <nav class="pm-cats" aria-label="Categorias do cardápio">
                        @for (c of visibleCategories(); track c.id) {
                          <a class="pm-cat" [class.active]="c.id === activeCategory()" [href]="'#cat-' + c.id" (click)="goTo($event, c.id)">{{ c.name }}</a>
                        }
                      </nav>
                    }

                    @for (c of visibleCategories(); track c.id) {
                      <section class="pm-section" [id]="'cat-' + c.id" [attr.aria-labelledby]="'cat-title-' + c.id">
                        <h3 class="pm-section-title" [id]="'cat-title-' + c.id">{{ c.name }}</h3>
                        @if (c.description) { <p class="pm-section-desc">{{ c.description }}</p> }
                        <ul class="pm-list">
                          @for (p of c.products; track p.id) {
                            <li>
                              <button type="button" class="pm-item" [class.off]="p.availability === 'UNAVAILABLE'" (click)="open(p)" [attr.aria-label]="itemLabel(p)">
                                <span class="pm-item-text">
                                  <span class="pm-item-name">{{ p.name }}</span>
                                  @if (p.description) { <span class="pm-item-desc">{{ p.description }}</span> }
                                  <span class="pm-item-foot">
                                    <span class="pm-item-price">{{ price(p.priceInCents) }}</span>
                                    @if (p.availability === 'UNAVAILABLE') { <span class="pm-off-tag">Indisponível no momento</span> }
                                    @else if (p.addons.length) { <span class="pm-item-extra">{{ p.addons.length }} {{ p.addons.length === 1 ? 'adicional' : 'adicionais' }}</span> }
                                    @if (p.imageUrl) { <span class="pm-item-3d" title="Fotografia disponível em realidade aumentada">AR</span> }
                                  </span>
                                </span>
                                @if (p.imageUrl) {
                                  <img class="pm-item-img" [src]="p.imageUrl" alt="" loading="lazy" decoding="async" width="104" height="104">
                                }
                              </button>
                            </li>
                          }
                        </ul>
                      </section>
                    }
                  }
                }
              }
            </main>
          }
        }
      }

      <footer class="pm-foot">
        <p>Preços e disponibilidade informados pelo restaurante. Ingredientes são descritivos e não substituem a orientação da equipe em caso de alergias.</p>
        <a routerLink="/">Cardápio digital por <strong>Boraê</strong></a>
      </footer>

      @if (state() === 'ready' && data(); as readyRestaurant) {
        @if (hasAvailableProducts()) {
          <bo-public-assistant-drawer
            [restaurant]="readyRestaurant"
            [selectedMenuId]="selectedMenu()?.id ?? null"
            (selectProduct)="open($event)"
          />
        }
      }

      <dialog #sheet class="pm-sheet" (cancel)="close()" (click)="onBackdrop($event)" aria-label="Detalhes do produto">
        @if (detail(); as p) {
          <div class="pm-sheet-inner">
            <button type="button" class="pm-sheet-close" (click)="close()" aria-label="Fechar detalhes"><bo-icon name="x" [size]="18" /></button>
            <bo-menu-product-card [product]="p" [photoAr]="true" />
          </div>
        }
      </dialog>
    </div>
  `
})
export class PublicMenuPage {
  /** Parâmetro de rota :slug. */
  readonly slug = input.required<string>();

  private readonly api = inject(PublicMenuService);
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);

  protected readonly state = signal<LoadState>('loading');
  protected readonly data = signal<PublicRestaurantMenu | null>(null);
  protected readonly errorText = signal('');
  protected readonly logoFailed = signal(false);
  private readonly selectedMenuId = signal<string | null>(null);
  protected readonly activeCategory = signal<string | null>(null);
  protected readonly detail = signal<PublicProduct | null>(null);
  private readonly sheet = viewChild<ElementRef<HTMLDialogElement>>('sheet');

  protected readonly selectedMenu = computed<PublicMenu | null>(() => {
    const menus = this.data()?.menus ?? [];
    return menus.find(m => m.id === this.selectedMenuId()) ?? menus[0] ?? null;
  });
  /** Categorias com ao menos um produto (o backend já remove inativas e arquivados). */
  protected readonly visibleCategories = computed(() => (this.selectedMenu()?.categories ?? []).filter(c => c.products.length > 0));
  /** Exibe o assistente somente quando o restaurante possui ao menos um produto disponível no cardápio publicado. */
  protected readonly hasAvailableProducts = computed(() =>
    (this.data()?.menus ?? []).some(menu =>
      menu.categories.some(category => category.products.some(product => product.availability === 'AVAILABLE'))
    )
  );
  protected readonly initials = computed(() => (this.data()?.name ?? '')
    .split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0]!.toUpperCase()).join(''));
  protected readonly contacts = computed(() => {
    const r = this.data();
    if (!r) return [];
    const list: { label: string; href: string; icon: 'smartphone' | 'camera' | 'globe' | 'map-pin' }[] = [];
    const digits = (value: string) => value.replace(/\D/g, '');
    if (r.whatsapp && digits(r.whatsapp).length >= 10) {
      const number = digits(r.whatsapp);
      list.push({ label: 'WhatsApp', href: `https://wa.me/${number.length <= 11 ? '55' + number : number}`, icon: 'smartphone' });
    }
    if (r.phone && digits(r.phone).length >= 8) list.push({ label: r.phone, href: `tel:${digits(r.phone)}`, icon: 'smartphone' });
    if (r.instagram) {
      const handle = r.instagram.replace(/^https?:\/\/(www\.)?instagram\.com\//i, '').replace(/^@/, '').replace(/\/.*$/, '');
      if (/^[A-Za-z0-9._]{1,30}$/.test(handle)) list.push({ label: `@${handle}`, href: `https://instagram.com/${handle}`, icon: 'camera' });
    }
    if (r.website && /^https?:\/\//i.test(r.website)) list.push({ label: 'Site', href: r.website, icon: 'globe' });
    if (r.address?.trim()) list.push({ label: r.address.trim(), href: `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(r.address.trim())}`, icon: 'map-pin' });
    return list;
  });

  constructor() {
    effect(() => {
      this.slug();
      untracked(() => this.load());
    });
    effect(() => {
      const el = this.sheet()?.nativeElement;
      if (!el) return;
      if (this.detail() && !el.open) el.showModal?.();
      if (!this.detail() && el.open) el.close();
    });
  }

  load(): void {
    this.state.set('loading');
    this.api.getRestaurantMenu(this.slug()).subscribe({
      next: data => {
        this.data.set(data);
        this.logoFailed.set(false);
        this.selectedMenuId.set(data.menus[0]?.id ?? null);
        this.activeCategory.set(null);
        this.state.set('ready');
        this.title.setTitle(`${data.name} · Cardápio digital | Boraê`);
        this.meta.updateTag({ name: 'description', content: data.description?.slice(0, 160) || `Consulte os cardápios de ${data.name} no Boraê.` });
      },
      error: (error: unknown) => {
        const status = error instanceof HttpErrorResponse ? error.status : -1;
        this.data.set(null);
        if (status === 404) {
          this.state.set('not-found');
          this.title.setTitle('Restaurante não encontrado | Boraê');
          return;
        }
        if (status === 429) {
          this.errorText.set('Muitas solicitações em sequência. Aguarde alguns instantes e tente novamente.');
          this.state.set('error');
          return;
        }
        this.errorText.set(status === 0
          ? 'Verifique sua conexão com a internet e tente novamente.'
          : 'O serviço pode estar iniciando. Tente novamente em alguns segundos.');
        this.state.set('error');
      }
    });
  }

  protected selectMenu(id: string): void {
    this.selectedMenuId.set(id);
    this.activeCategory.set(null);
  }

  protected goTo(event: Event, categoryId: string): void {
    event.preventDefault();
    this.activeCategory.set(categoryId);
    const target = document.getElementById(`cat-${categoryId}`);
    const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    target?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' });
  }

  protected open(product: PublicProduct): void { this.detail.set(product); }
  protected close(): void { this.detail.set(null); }

  protected onBackdrop(event: MouseEvent): void {
    if (event.target === this.sheet()?.nativeElement) this.close();
  }

  protected price(cents: number): string { return formatCents(cents); }

  protected itemLabel(p: PublicProduct): string {
    return `${p.name}, ${formatCents(p.priceInCents)}${p.availability === 'UNAVAILABLE' ? ', indisponível no momento' : ''}${p.imageUrl ? ', fotografia disponível em realidade aumentada' : ''}. Ver detalhes`;
  }
}
