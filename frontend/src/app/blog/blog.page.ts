import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FooterComponent } from '../landing/footer.component';
import { HeaderComponent } from '../landing/header.component';
import { IconComponent } from '../shared/icon.component';
import { BLOG_ARTICLES, BLOG_CATEGORIES, getCategoryBySlug } from './blog.data';
import { BlogArticle, BlogCategorySlug } from './blog.models';
import { BlogSeoService } from './blog-seo.service';

@Component({
  selector: 'app-blog-page',
  standalone: true,
  imports: [RouterLink, FormsModule, HeaderComponent, FooterComponent, IconComponent],
  template: `
    <div class="blog-layout">
      <app-header />

      <main id="main-content" class="blog-main">
        <!-- Hero Editorial do Blog -->
        <section class="blog-hero" aria-labelledby="blog-main-title">
          <div class="container">
            <nav class="breadcrumb" aria-label="Breadcrumb">
              <ol>
                <li><a routerLink="/">Início</a></li>
                <li aria-hidden="true">/</li>
                <li aria-current="page">Blog Boraê</li>
              </ol>
            </nav>

            <div class="hero-content">
              <span class="eyebrow-tag">BLOG BORAÊ</span>
              <h1 id="blog-main-title" class="hero-title">
                Um universo de ideias para quem acredita na gastronomia.
              </h1>
              <p class="hero-lead">
                A gastronomia está em constante transformação. Aqui reunimos conhecimento,
                histórias e novas perspectivas sobre o universo dos restaurantes, da tecnologia e
                das experiências à mesa.
              </p>

              <!-- Busca Editorial -->
              <div class="search-bar" role="search" aria-label="Buscar artigos no Blog Boraê">
                <bo-icon name="search" [size]="18" class="search-icon" />
                <label for="blog-search-input" class="sr-only">
                  Buscar artigos por título, tema ou conteúdo no blog
                </label>
                <input
                  id="blog-search-input"
                  type="search"
                  class="search-input"
                  placeholder="Buscar artigos por título, palavra-chave ou assunto do blog..."
                  [ngModel]="searchQuery()"
                  (ngModelChange)="onSearchChange($event)"
                />
                @if (searchQuery()) {
                  <button
                    type="button"
                    class="clear-btn"
                    (click)="onSearchChange('')"
                    aria-label="Limpar busca"
                  >
                    <bo-icon name="x" [size]="16" />
                  </button>
                }
              </div>
              <p class="search-caption">
                Pesquisa editorial sobre os artigos publicados no Blog Boraê.
              </p>
            </div>
          </div>
        </section>

        <!-- Cinco Pilares Editoriais -->
        <section class="pillars-section" aria-labelledby="pillars-heading">
          <div class="container">
            <div class="pillars-header">
              <h2 id="pillars-heading" class="section-subtitle">Cinco pilares editoriais</h2>
              @if (activeCategory() !== 'all') {
                <button type="button" class="reset-filter-btn" (click)="setCategory('all')">
                  Ver todas as categorias
                </button>
              }
            </div>

            <div class="pillars-grid">
              @for (cat of categories; track cat.id) {
                <button
                  type="button"
                  class="pillar-card-item"
                  [class.active]="activeCategory() === cat.slug"
                  [attr.aria-pressed]="activeCategory() === cat.slug"
                  (click)="setCategory(cat.slug)"
                >
                  <div class="pillar-top">
                    <span class="pillar-icon-box">
                      <bo-icon [name]="cat.icon" [size]="20" />
                    </span>
                    <span class="pillar-audience">{{ cat.audienceLabel }}</span>
                  </div>
                  <h3 class="pillar-name">{{ cat.name }}</h3>
                  <p class="pillar-desc">{{ cat.description }}</p>
                </button>
              }
            </div>
          </div>
        </section>

        <!-- Listagem de Artigos -->
        <section class="catalog-section" aria-labelledby="articles-heading">
          <div class="container">
            <div class="catalog-toolbar">
              <div>
                <h2 id="articles-heading" class="catalog-title">
                  @if (activeCategoryObj(); as catObj) {
                    {{ catObj.name }}
                  } @else {
                    Artigos publicados
                  }
                </h2>
                <p class="catalog-count">
                  {{ filteredArticles().length }}
                  {{ filteredArticles().length === 1 ? 'artigo encontrado' : 'artigos encontrados' }}
                  — ordenados do mais recente para o mais antigo.
                </p>
              </div>
            </div>

            @if (filteredArticles().length === 0) {
              <div class="empty-state" role="status">
                <h3>Nenhum artigo corresponde aos filtros selecionados.</h3>
                <p>Experimente buscar por outros termos ou visualizar todas as categorias editoriais.</p>
                <button type="button" class="btn btn-secondary" (click)="resetAllFilters()">
                  Limpar filtros
                </button>
              </div>
            } @else {
              <div class="articles-list-grid">
                @for (article of filteredArticles(); track article.id; let first = $first) {
                  <article class="blog-card" [class.blog-card-featured]="first && activeCategory() === 'all' && !searchQuery()">
                    <a
                      [routerLink]="['/blog', article.slug]"
                      class="blog-card-media"
                      [attr.aria-label]="article.title"
                    >
                      <figure class="blog-figure">
                        <img
                          [src]="article.coverImage"
                          [attr.srcset]="article.coverSrcset || null"
                          [attr.sizes]="article.coverSizes || null"
                          [alt]="article.coverAlt"
                          [width]="article.coverWidth"
                          [height]="article.coverHeight"
                          [attr.loading]="first ? 'eager' : 'lazy'"
                          decoding="async"
                        />
                      </figure>
                    </a>

                    <div class="blog-card-content">
                      <div class="blog-card-meta">
                        <button
                          type="button"
                          class="cat-chip"
                          (click)="setCategory(article.category)"
                        >
                          {{ getCategoryName(article.category) }}
                        </button>
                        <span aria-hidden="true">•</span>
                        <time [attr.datetime]="article.publishedAt">
                          {{ formatDate(article.publishedAt) }}
                        </time>
                        <span aria-hidden="true">•</span>
                        <span>{{ article.readingTime }} min de leitura</span>
                      </div>

                      <h3 class="blog-card-title">
                        <a [routerLink]="['/blog', article.slug]">{{ article.title }}</a>
                      </h3>

                      <p class="blog-card-excerpt">{{ article.excerpt }}</p>

                      <div class="blog-card-footer">
                        <span class="credit-note">{{ article.imageCredit.label }}</span>
                        <a [routerLink]="['/blog', article.slug]" class="read-more-link">
                          <span>Ler artigo completo</span>
                          <bo-icon name="arrow-right" [size]="16" />
                        </a>
                      </div>
                    </div>
                  </article>
                }
              </div>
            }
          </div>
        </section>

        <!-- Caminhos Naturais de Conversão (Restaurantes, Consumidores e Parceiros) -->
        <section class="conversion-section" aria-labelledby="conversion-heading">
          <div class="container">
            <div class="conversion-header">
              <span class="eyebrow-tag">CONECTE-SE AO BORAÊ</span>
              <h2 id="conversion-heading" class="conversion-main-title">
                Uma plataforma construída para quem faz e quem vive a gastronomia.
              </h2>
            </div>

            <div class="conversion-grid">
              <!-- CTA Restaurantes -->
              <article class="conv-card conv-card-primary">
                <span class="conv-tag">Para restaurantes</span>
                <h3>
                  Seu restaurante tem uma história, uma identidade e pratos que merecem ser
                  apresentados com cuidado.
                </h3>
                <p>
                  O Boraê está desenvolvendo uma plataforma para aproximar restaurantes e
                  consumidores através de experiências digitais mais completas.
                </p>
                <a routerLink="/auth" class="btn btn-primary">
                  <span>Conheça o Boraê para restaurantes</span>
                  <bo-icon class="btn-arrow" name="arrow-right" [size]="17" />
                </a>
              </article>

              <!-- CTA Consumidores -->
              <article class="conv-card">
                <span class="conv-tag">Para consumidores</span>
                <h3>Uma boa experiência gastronômica começa com a descoberta.</h3>
                <p>
                  Conheça a proposta do Boraê e acompanhe a evolução de uma nova forma de conectar
                  pessoas e restaurantes.
                </p>
                <a routerLink="/" class="btn btn-secondary">
                  <span>Conheça o Boraê</span>
                  <bo-icon class="btn-arrow" name="arrow-right" [size]="17" />
                </a>
              </article>

              <!-- CTA Colaboradores e Parceiros -->
              <article class="conv-card">
                <span class="conv-tag">Colaboradores e parceiros</span>
                <h3>Quer construir novas possibilidades para a gastronomia?</h3>
                <p>
                  O Boraê é um projeto em evolução que conecta tecnologia, gastronomia e
                  experiências digitais. Estamos abertos a conversas com pessoas e organizações que
                  compartilham dessa visão.
                </p>
                <a routerLink="/" fragment="sobre" class="btn btn-secondary">
                  <span>Conheça nosso projeto</span>
                  <bo-icon class="btn-arrow" name="arrow-right" [size]="17" />
                </a>
              </article>
            </div>
          </div>
        </section>
      </main>

      <app-footer />
    </div>
  `,
  styles: [`
    .blog-layout { display: flex; flex-direction: column; min-height: 100vh; background: var(--color-white); }
    .blog-main { flex: 1; }
    .blog-hero { padding: clamp(36px, 5vw, 68px) 0; background: linear-gradient(180deg, var(--color-cream), var(--color-white)); border-bottom: 1px solid var(--color-border); }
    .breadcrumb ol { display: flex; align-items: center; gap: 8px; list-style: none; padding: 0; margin: 0 0 20px; font-size: 13.5px; color: var(--color-text-muted); }
    .breadcrumb a:hover, .blog-card-title a:hover { color: var(--color-primary-dark); }
    .hero-content { max-width: 780px; display: flex; flex-direction: column; align-items: flex-start; gap: 14px; }
    .hero-title { font-size: clamp(30px, 4.4vw, 50px); font-weight: 800; line-height: 1.12; }
    .hero-lead { font-size: clamp(16px, 1.8vw, 18.5px); line-height: 1.65; color: var(--color-text-muted); }
    .search-bar { width: 100%; max-width: 620px; display: flex; align-items: center; gap: 10px; background: var(--color-white); border: 1.5px solid var(--color-border); border-radius: var(--radius-pill); padding: 10px 18px; margin-top: 6px; box-shadow: var(--shadow-sm); }
    .search-bar:focus-within { border-color: var(--color-primary); }
    .search-icon { color: var(--color-primary); }
    .search-input { flex: 1; border: 0; background: transparent; font: inherit; font-size: 15px; outline: none; }
    .clear-btn { border: 0; background: var(--color-surface); border-radius: 50%; width: 28px; height: 28px; display: grid; place-items: center; color: var(--color-text-muted); }
    .search-caption { font-size: 12.5px; color: var(--color-text-subtle); }
    .pillars-section { padding: clamp(36px, 5vw, 56px) 0; border-bottom: 1px solid var(--color-border); }
    .pillars-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
    .section-subtitle { font-size: 20px; font-weight: 800; }
    .reset-filter-btn { background: none; border: 0; color: var(--color-primary-dark); font-weight: 700; font-size: 14px; text-decoration: underline; }
    .pillars-grid, .articles-list-grid, .conversion-grid { display: grid; grid-template-columns: 1fr; gap: 20px; }
    @media (min-width: 680px) { .pillars-grid, .articles-list-grid { grid-template-columns: repeat(2, 1fr); } }
    @media (min-width: 1060px) {
      .pillars-grid { grid-template-columns: repeat(5, 1fr); gap: 16px; }
      .articles-list-grid, .conversion-grid { grid-template-columns: repeat(3, 1fr); gap: 24px; }
      .blog-card-featured { grid-column: 1 / -1; display: grid; grid-template-columns: 1.15fr 1fr; }
    }
    .pillar-card-item { text-align: left; background: var(--color-cream); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 18px; display: flex; flex-direction: column; gap: 10px; transition: all var(--transition-fast); }
    .pillar-card-item:hover, .pillar-card-item.active { background: var(--color-white); border-color: var(--color-primary); box-shadow: var(--shadow-sm); transform: translateY(-2px); }
    .pillar-top { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
    .pillar-icon-box { width: 36px; height: 36px; border-radius: var(--radius-sm); background: var(--color-primary-light); color: var(--color-primary-dark); display: grid; place-items: center; }
    .pillar-audience { font-size: 11px; font-weight: 700; color: var(--color-text-muted); }
    .pillar-name { font-size: 15px; font-weight: 800; line-height: 1.3; }
    .pillar-desc { font-size: 13px; line-height: 1.5; color: var(--color-text-muted); }
    .catalog-section { padding: clamp(44px, 6vw, 80px) 0; }
    .catalog-toolbar { margin-bottom: 28px; }
    .catalog-title { font-size: clamp(24px, 3vw, 32px); font-weight: 800; margin-bottom: 4px; }
    .catalog-count { font-size: 14px; color: var(--color-text-muted); }
    .empty-state { padding: 44px 24px; text-align: center; background: var(--color-cream); border: 1px solid var(--color-border); border-radius: var(--radius-lg); display: flex; flex-direction: column; align-items: center; gap: 12px; }
    .blog-card { background: var(--color-white); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; display: flex; flex-direction: column; transition: transform var(--transition-fast), box-shadow var(--transition-fast); }
    .blog-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-md); }
    .blog-figure { margin: 0; aspect-ratio: 16 / 9; overflow: hidden; background: #EFEAE4; height: 100%; }
    .blog-figure img { width: 100%; height: 100%; object-fit: cover; }
    .blog-card-content { padding: 22px; display: flex; flex-direction: column; gap: 12px; flex: 1; }
    .blog-card-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 12.5px; color: var(--color-text-muted); }
    .cat-chip, .conv-tag { border: 0; background: var(--color-primary-light); color: var(--color-primary-dark); font-weight: 700; font-size: 11px; padding: 4px 10px; border-radius: var(--radius-pill); text-transform: uppercase; }
    .blog-card-title { font-size: clamp(18.5px, 2vw, 23px); font-weight: 800; line-height: 1.28; }
    .blog-card-excerpt { font-size: 14.5px; line-height: 1.6; color: var(--color-text-muted); }
    .blog-card-footer { margin-top: auto; padding-top: 12px; border-top: 1px solid var(--color-border-subtle); display: flex; align-items: center; justify-content: space-between; gap: 10px; }
    .credit-note { font-size: 11.5px; color: var(--color-text-subtle); }
    .read-more-link { display: inline-flex; align-items: center; gap: 6px; font-size: 13.5px; font-weight: 700; color: var(--color-primary-dark); }
    .conversion-section { padding: clamp(52px, 7vw, 88px) 0; background: var(--color-cream); border-top: 1px solid var(--color-border); }
    .conversion-header { max-width: 700px; margin-bottom: 32px; display: flex; flex-direction: column; align-items: flex-start; gap: 10px; }
    .conversion-main-title { font-size: clamp(24px, 3.4vw, 36px); font-weight: 800; }
    .conv-card { background: var(--color-white); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 26px; display: flex; flex-direction: column; align-items: flex-start; gap: 12px; }
    .conv-card-primary { border-color: #FED4B2; background: linear-gradient(180deg, #FFFFFF, #FFF8F0); }
    .conv-card h3 { font-size: 18.5px; font-weight: 800; line-height: 1.3; }
    .conv-card p { font-size: 14px; line-height: 1.6; color: var(--color-text-muted); flex: 1; }
  `]
})
export class BlogPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly seoService = inject(BlogSeoService);

  readonly categories = BLOG_CATEGORIES;
  readonly activeCategory = signal<'all' | BlogCategorySlug>('all');
  readonly searchQuery = signal<string>('');

  readonly activeCategoryObj = computed(() => {
    const slug = this.activeCategory();
    return slug === 'all' ? undefined : getCategoryBySlug(slug);
  });

  readonly filteredArticles = computed<BlogArticle[]>(() => {
    const cat = this.activeCategory();
    const query = this.searchQuery().trim().toLowerCase();

    return [...BLOG_ARTICLES]
      .filter(article => {
        if (cat !== 'all' && article.category !== cat) return false;
        if (!query) return true;
        const searchable = [
          article.title,
          article.subtitle,
          article.excerpt,
          article.tags.join(' '),
          article.content
            .map(block => {
              if (block.type === 'paragraph') return block.segments.map(s => s.text).join(' ');
              if (block.type === 'heading') return block.text;
              if (block.type === 'callout') return `${block.title} ${block.text}`;
              if (block.type === 'list') {
                return block.items
                  .map(i => `${i.title ?? ''} ${i.segments.map(s => s.text).join(' ')}`)
                  .join(' ');
              }
              return '';
            })
            .join(' ')
        ]
          .join(' ')
          .toLowerCase();
        return searchable.includes(query);
      })
      .sort((a, b) => b.publishedAt.localeCompare(a.publishedAt));
  });

  ngOnInit(): void {
    this.seoService.setBlogIndexSeo();
    this.route.queryParamMap.subscribe(params => {
      const catParam = params.get('categoria') as BlogCategorySlug | null;
      if (catParam && getCategoryBySlug(catParam)) {
        this.activeCategory.set(catParam);
      } else {
        this.activeCategory.set('all');
      }
    });
  }

  setCategory(slug: 'all' | BlogCategorySlug): void {
    this.activeCategory.set(slug);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { categoria: slug === 'all' ? null : slug },
      queryParamsHandling: 'merge'
    });
  }

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
  }

  resetAllFilters(): void {
    this.searchQuery.set('');
    this.setCategory('all');
  }

  getCategoryName(slug: BlogCategorySlug): string {
    return getCategoryBySlug(slug)?.shortName ?? 'Editorial';
  }

  formatDate(isoDate: string): string {
    const [year, month, day] = isoDate.split('-');
    return `${day}/${month}/${year}`;
  }
}
