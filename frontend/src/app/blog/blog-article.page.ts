import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FooterComponent } from '../landing/footer.component';
import { HeaderComponent } from '../landing/header.component';
import { IconComponent } from '../shared/icon.component';
import {
  getArticleBySlug,
  getCategoryBySlug,
  getRelatedArticles
} from './blog.data';
import { BlogArticle, BlogCategorySlug } from './blog.models';
import { BlogSeoService } from './blog-seo.service';

@Component({
  selector: 'app-blog-article-page',
  standalone: true,
  imports: [RouterLink, HeaderComponent, FooterComponent, IconComponent],
  template: `
    <div class="article-layout">
      <app-header />

      <main id="main-content" class="article-main">
        @if (article(); as post) {
          <article class="article-document" itemscope itemtype="https://schema.org/BlogPosting">
            <!-- Cabeçalho do Artigo -->
            <header class="article-header">
              <div class="container article-narrow">
                <!-- 1. Breadcrumb -->
                <nav class="breadcrumb" aria-label="Breadcrumb">
                  <ol>
                    <li><a routerLink="/">Início</a></li>
                    <li aria-hidden="true">/</li>
                    <li><a routerLink="/blog">Blog</a></li>
                    <li aria-hidden="true">/</li>
                    <li>
                      <a [routerLink]="['/blog']" [queryParams]="{ categoria: post.category }">
                        {{ category()?.shortName }}
                      </a>
                    </li>
                    <li aria-hidden="true">/</li>
                    <li aria-current="page" class="crumb-current">{{ post.title }}</li>
                  </ol>
                </nav>

                <!-- 2. Categoria e Tempo de Leitura -->
                <div class="header-badges">
                  <a
                    [routerLink]="['/blog']"
                    [queryParams]="{ categoria: post.category }"
                    class="category-pill"
                  >
                    {{ category()?.name }}
                  </a>
                  <span class="reading-badge">
                    <bo-icon name="clock" [size]="14" />
                    <span>{{ post.readingTime }} min de leitura</span>
                  </span>
                </div>

                <!-- 3. Título Principal (H1 único) -->
                <h1 class="article-h1" itemprop="headline">{{ post.title }}</h1>

                <!-- 4. Subtítulo -->
                <p class="article-subtitle" itemprop="description">{{ post.subtitle }}</p>

                <!-- 5, 6, 7, 17. Autor, Datas e Compartilhamento -->
                <div class="meta-bar">
                  <div class="author-meta">
                    <div class="author-avatar" aria-hidden="true">B</div>
                    <div>
                      <span class="author-name" itemprop="author">{{ post.author.name }}</span>
                      <div class="dates-row">
                        <span>
                          Publicado em
                          <time [attr.datetime]="post.publishedAt" itemprop="datePublished">
                            {{ formatDate(post.publishedAt) }}
                          </time>
                        </span>
                        @if (post.updatedAt) {
                          <span aria-hidden="true">•</span>
                          <span>
                            Atualizado em
                            <time [attr.datetime]="post.updatedAt" itemprop="dateModified">
                              {{ formatDate(post.updatedAt) }}
                            </time>
                          </span>
                        }
                      </div>
                    </div>
                  </div>

                  <button
                    type="button"
                    class="share-btn"
                    (click)="copyArticleLink(post.canonicalUrl)"
                    [attr.aria-label]="'Copiar link do artigo ' + post.title"
                  >
                    <bo-icon [name]="copied() ? 'check' : 'link'" [size]="16" />
                    <span>{{ copied() ? 'Link copiado!' : 'Compartilhar link' }}</span>
                  </button>
                </div>
              </div>
            </header>

            <!-- 9 & 12. Imagem de Capa 16:9 e Crédito -->
            <div class="container article-wide">
              <figure class="cover-figure">
                <img
                  [src]="post.coverImage"
                  [attr.srcset]="post.coverSrcset || null"
                  [attr.sizes]="post.coverSizes || null"
                  [alt]="post.coverAlt"
                  [width]="post.coverWidth"
                  [height]="post.coverHeight"
                  loading="eager"
                  decoding="async"
                  itemprop="image"
                />
                <figcaption class="figure-caption">
                  <span>{{ post.coverAlt }}.</span>
                  @if (post.imageCredit.sourceUrl) {
                    <a
                      [href]="post.imageCredit.sourceUrl"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="credit-link"
                    >
                      {{ post.imageCredit.label }}
                    </a>
                  } @else {
                    <span class="credit-text">{{ post.imageCredit.label }}</span>
                  }
                </figcaption>
              </figure>
            </div>

            <!-- 10 & 11. Corpo Estruturado do Artigo -->
            <div class="container article-narrow">
              <div class="article-body" itemprop="articleBody">
                @for (block of post.content; track $index) {
                  @switch (block.type) {
                    @case ('heading') {
                      @if (block.level === 2) {
                        <h2 [id]="block.id" class="body-h2">{{ block.text }}</h2>
                      } @else {
                        <h3 [id]="block.id" class="body-h3">{{ block.text }}</h3>
                      }
                    }

                    @case ('paragraph') {
                      <p class="body-p">
                        @for (seg of block.segments; track $index) {
                          @if (seg.routerLink) {
                            <a
                              [routerLink]="seg.routerLink"
                              [queryParams]="seg.queryParams || null"
                              [fragment]="seg.fragment || undefined"
                              class="inline-link"
                            >{{ seg.text }}</a>
                          } @else if (seg.externalHref) {
                            <a
                              [href]="seg.externalHref"
                              target="_blank"
                              rel="noopener noreferrer"
                              class="inline-link"
                            >{{ seg.text }}</a>
                          } @else if (seg.strong) {
                            <strong>{{ seg.text }}</strong>
                          } @else {
                            {{ seg.text }}
                          }
                        }
                      </p>
                    }

                    @case ('list') {
                      @if (block.ordered) {
                        <ol class="body-list">
                          @for (item of block.items; track $index) {
                            <li>
                              @if (item.title) {
                                <strong>{{ item.title }}</strong>{{ ' ' }}
                              }
                              @for (seg of item.segments; track $index) {
                                @if (seg.routerLink) {
                                  <a [routerLink]="seg.routerLink" class="inline-link">{{ seg.text }}</a>
                                } @else {
                                  {{ seg.text }}
                                }
                              }
                            </li>
                          }
                        </ol>
                      } @else {
                        <ul class="body-list">
                          @for (item of block.items; track $index) {
                            <li>
                              @if (item.title) {
                                <strong>{{ item.title }}</strong>{{ ' ' }}
                              }
                              @for (seg of item.segments; track $index) {
                                @if (seg.routerLink) {
                                  <a [routerLink]="seg.routerLink" class="inline-link">{{ seg.text }}</a>
                                } @else {
                                  {{ seg.text }}
                                }
                              }
                            </li>
                          }
                        </ul>
                      }
                    }

                    @case ('callout') {
                      <aside class="body-callout" aria-label="Nota editorial">
                        <span class="callout-eyebrow">{{ block.eyebrow }}</span>
                        <h3 class="callout-title">{{ block.title }}</h3>
                        <p class="callout-text">{{ block.text }}</p>
                      </aside>
                    }

                    @case ('image') {
                      <figure class="inline-figure">
                        <img
                          [src]="block.src"
                          [attr.srcset]="block.srcset || null"
                          [attr.sizes]="block.sizes || null"
                          [alt]="block.alt"
                          [width]="block.width"
                          [height]="block.height"
                          loading="lazy"
                          decoding="async"
                        />
                        <figcaption class="figure-caption">
                          <span>{{ block.caption }}</span>
                          @if (block.credit.sourceUrl) {
                            <a
                              [href]="block.credit.sourceUrl"
                              target="_blank"
                              rel="noopener noreferrer"
                              class="credit-link"
                            >
                              {{ block.credit.label }}
                            </a>
                          } @else {
                            <span class="credit-text">{{ block.credit.label }}</span>
                          }
                        </figcaption>
                      </figure>
                    }
                  }
                }
              </div>

              <!-- Tags do Artigo -->
              <div class="tags-row" aria-label="Temas abordados neste artigo">
                @for (tag of post.tags; track tag) {
                  <span class="tag-badge">{{ tag }}</span>
                }
              </div>

              <!-- 13. Bloco de Links Internos Estratégicos -->
              <section class="internal-links-box" aria-labelledby="internal-links-title">
                <h2 id="internal-links-title" class="box-title">
                  Continue aprofundando este tema no Boraê
                </h2>
                <div class="internal-links-grid">
                  @for (link of post.internalLinks; track link.routerLink) {
                    <a
                      [routerLink]="link.routerLink"
                      [queryParams]="link.queryParams || null"
                      [fragment]="link.fragment || undefined"
                      class="internal-link-card"
                    >
                      <strong>{{ link.label }}</strong>
                      <span>{{ link.description }}</span>
                    </a>
                  }
                </div>
              </section>

              <!-- 14. Fontes Externas Verificáveis -->
              <section class="sources-box" aria-labelledby="sources-title">
                <h2 id="sources-title" class="box-title">Fontes e referências consultadas</h2>
                <ul class="sources-list">
                  @for (source of post.externalSources; track source.url) {
                    <li>
                      <a [href]="source.url" target="_blank" rel="noopener noreferrer">
                        {{ source.title }} — {{ source.publisher }}
                      </a>
                      <p>{{ source.note }}</p>
                    </li>
                  }
                </ul>
              </section>

              <!-- 16. CTA Contextual do Artigo -->
              <section class="article-cta-card" aria-labelledby="article-cta-heading">
                <span class="eyebrow-tag">{{ post.cta.eyebrow }}</span>
                <h2 id="article-cta-heading" class="cta-card-title">{{ post.cta.title }}</h2>
                <p class="cta-card-desc">{{ post.cta.description }}</p>
                <div class="cta-actions">
                  <a
                    [routerLink]="post.cta.routerLink"
                    [fragment]="post.cta.fragment || undefined"
                    class="btn btn-primary"
                  >
                    <span>{{ post.cta.buttonLabel }}</span>
                    <bo-icon class="btn-arrow" name="arrow-right" [size]="17" />
                  </a>
                  @if (post.cta.secondaryLabel && post.cta.secondaryRouterLink) {
                    <a
                      [routerLink]="post.cta.secondaryRouterLink"
                      [fragment]="post.cta.secondaryFragment || undefined"
                      class="btn btn-secondary"
                    >
                      <span>{{ post.cta.secondaryLabel }}</span>
                    </a>
                  }
                </div>
              </section>
            </div>
          </article>

          <!-- 15. Artigos Relacionados -->
          <section class="related-section" aria-labelledby="related-heading">
            <div class="container">
              <div class="related-header">
                <div>
                  <span class="eyebrow-tag">LEITURA RELACIONADA</span>
                  <h2 id="related-heading" class="related-main-title">
                    Outros artigos para continuar explorando
                  </h2>
                </div>
                <a routerLink="/blog" class="btn btn-secondary">Ver todos os artigos</a>
              </div>

              <div class="related-grid">
                @for (rel of relatedArticles(); track rel.id) {
                  <article class="related-card">
                    <a [routerLink]="['/blog', rel.slug]" class="related-media">
                      <img
                        [src]="rel.coverImage"
                        [alt]="rel.coverAlt"
                        [width]="rel.coverWidth"
                        [height]="rel.coverHeight"
                        loading="lazy"
                        decoding="async"
                      />
                    </a>
                    <div class="related-body">
                      <span class="related-cat">{{ getCategoryName(rel.category) }}</span>
                      <h3>
                        <a [routerLink]="['/blog', rel.slug]">{{ rel.title }}</a>
                      </h3>
                      <p>{{ rel.excerpt }}</p>
                      <a [routerLink]="['/blog', rel.slug]" class="related-link">
                        <span>Ler artigo ({{ rel.readingTime }} min)</span>
                        <bo-icon name="arrow-right" [size]="15" />
                      </a>
                    </div>
                  </article>
                }
              </div>
            </div>
          </section>
        } @else {
          <!-- Estado de Artigo Não Encontrado -->
          <section class="not-found-section">
            <div class="container article-narrow">
              <span class="eyebrow-tag">BLOG BORAÊ</span>
              <h1>Artigo não encontrado</h1>
              <p>
                O endereço acessado não corresponde a um artigo publicado no momento. Explore os
                conteúdos disponíveis em nossa página principal do blog.
              </p>
              <a routerLink="/blog" class="btn btn-primary">Voltar para o Blog Boraê</a>
            </div>
          </section>
        }
      </main>

      <app-footer />
    </div>
  `,
  styles: [`
    .article-layout { display: flex; flex-direction: column; min-height: 100vh; background: var(--color-white); }
    .article-main { flex: 1; }
    .article-narrow { max-width: 780px; }
    .article-wide { max-width: 1040px; }
    .article-header { padding: clamp(32px, 5vw, 52px) 0 24px; background: linear-gradient(180deg, var(--color-cream), var(--color-white)); }
    .breadcrumb ol { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; list-style: none; padding: 0; margin: 0 0 18px; font-size: 13px; color: var(--color-text-muted); }
    .breadcrumb a:hover, .related-body h3 a:hover { color: var(--color-primary-dark); }
    .crumb-current { color: var(--color-graphite); font-weight: 600; }
    .header-badges { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 14px; }
    .category-pill { background: var(--color-primary-light); color: var(--color-primary-dark); font-size: 12px; font-weight: 700; text-transform: uppercase; padding: 5px 12px; border-radius: var(--radius-pill); }
    .reading-badge { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: var(--color-text-muted); }
    .article-h1 { font-size: clamp(28px, 4.2vw, 44px); font-weight: 800; line-height: 1.15; margin-bottom: 14px; }
    .article-subtitle { font-size: clamp(16.5px, 1.9vw, 19.5px); line-height: 1.6; color: var(--color-text-muted); margin-bottom: 22px; }
    .meta-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 14px; padding-top: 18px; border-top: 1px solid var(--color-border); }
    .author-meta { display: flex; align-items: center; gap: 12px; }
    .author-avatar { width: 40px; height: 40px; border-radius: 50%; background: var(--color-graphite); color: var(--color-white); font-weight: 800; display: grid; place-items: center; }
    .author-name { display: block; font-size: 14px; font-weight: 700; }
    .dates-row { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; font-size: 12.5px; color: var(--color-text-muted); }
    .share-btn { display: inline-flex; align-items: center; gap: 8px; padding: 8px 15px; border-radius: var(--radius-pill); border: 1px solid var(--color-border); background: var(--color-white); font-size: 13px; font-weight: 700; }
    .share-btn:hover, .internal-link-card:hover { border-color: var(--color-primary); }
    .cover-figure, .inline-figure { margin: 22px 0 32px; border-radius: var(--radius-lg); overflow: hidden; border: 1px solid var(--color-border); background: var(--color-cream); }
    .cover-figure img, .inline-figure img, .related-media img { width: 100%; aspect-ratio: 16 / 9; object-fit: cover; }
    .figure-caption { padding: 12px 16px; font-size: 12.5px; color: var(--color-text-muted); display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; background: var(--color-white); }
    .credit-link, .inline-link, .sources-list a { color: var(--color-primary-dark); font-weight: 600; text-decoration: underline; }
    .article-body { font-size: 17px; line-height: 1.76; color: #262626; }
    .body-h2 { font-size: clamp(21px, 2.6vw, 27px); font-weight: 800; margin: 36px 0 14px; }
    .body-h3 { font-size: 19px; font-weight: 700; margin: 26px 0 10px; }
    .body-p { margin-bottom: 18px; }
    .body-list { margin: 0 0 22px; padding-left: 20px; display: flex; flex-direction: column; gap: 10px; }
    .body-callout { margin: 28px 0; padding: 22px 24px; background: var(--color-cream); border-left: 4px solid var(--color-primary); border-radius: 0 var(--radius-md) var(--radius-md) 0; }
    .callout-eyebrow { display: block; font-size: 11px; font-weight: 800; color: var(--color-primary-dark); text-transform: uppercase; margin-bottom: 6px; }
    .callout-title, .box-title { font-size: 18px; font-weight: 800; margin-bottom: 10px; }
    .callout-text { font-size: 15px; line-height: 1.65; color: var(--color-text-muted); }
    .tags-row { display: flex; flex-wrap: wrap; gap: 8px; margin: 32px 0; padding-top: 20px; border-top: 1px solid var(--color-border); }
    .tag-badge { padding: 5px 12px; border-radius: var(--radius-pill); background: var(--color-surface); font-size: 12.5px; font-weight: 600; color: var(--color-text-muted); }
    .internal-links-box, .sources-box { margin: 28px 0; padding: 22px; border: 1px solid var(--color-border); border-radius: var(--radius-md); }
    .internal-links-grid { display: grid; gap: 10px; margin-top: 12px; }
    .internal-link-card { padding: 12px 16px; border-radius: var(--radius-sm); background: var(--color-cream); border: 1px solid var(--color-border); display: flex; flex-direction: column; gap: 3px; }
    .internal-link-card strong { font-size: 15px; color: var(--color-primary-dark); }
    .internal-link-card span, .sources-list p { font-size: 13px; color: var(--color-text-muted); }
    .sources-list { margin: 12px 0 0; padding-left: 18px; display: flex; flex-direction: column; gap: 10px; font-size: 14px; }
    .article-cta-card { margin: 36px 0 56px; padding: clamp(24px, 4vw, 36px); border-radius: var(--radius-lg); background: linear-gradient(180deg, #FFF9F2, #FFF0E3); border: 1px solid #FED4B2; display: flex; flex-direction: column; align-items: flex-start; gap: 12px; }
    .cta-card-title { font-size: clamp(21px, 2.6vw, 27px); font-weight: 800; }
    .cta-card-desc { font-size: 15px; line-height: 1.65; color: var(--color-text-muted); }
    .cta-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 4px; }
    .related-section { padding: clamp(52px, 7vw, 84px) 0; background: var(--color-surface); border-top: 1px solid var(--color-border); }
    .related-header { display: flex; align-items: flex-end; justify-content: space-between; flex-wrap: wrap; gap: 16px; margin-bottom: 28px; }
    .related-main-title { font-size: clamp(23px, 3vw, 32px); font-weight: 800; margin-top: 8px; }
    .related-grid { display: grid; grid-template-columns: 1fr; gap: 22px; }
    @media (min-width: 768px) { .related-grid { grid-template-columns: repeat(3, 1fr); } }
    .related-card { background: var(--color-white); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; display: flex; flex-direction: column; }
    .related-body { padding: 18px; display: flex; flex-direction: column; gap: 8px; flex: 1; }
    .related-cat { font-size: 11px; font-weight: 700; text-transform: uppercase; color: var(--color-primary-dark); }
    .related-body h3 { font-size: 17px; font-weight: 800; line-height: 1.3; }
    .related-body p { font-size: 13.5px; color: var(--color-text-muted); }
    .related-link { margin-top: auto; display: inline-flex; align-items: center; gap: 6px; font-size: 13.5px; font-weight: 700; color: var(--color-primary-dark); }
    .not-found-section { padding: 80px 0; text-align: center; }
    .not-found-section p { color: var(--color-text-muted); margin: 12px 0 22px; }
  `]
})
export class BlogArticlePage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly seoService = inject(BlogSeoService);

  readonly article = signal<BlogArticle | undefined>(undefined);
  readonly copied = signal<boolean>(false);

  readonly category = computed(() => {
    const post = this.article();
    return post ? getCategoryBySlug(post.category) : undefined;
  });

  readonly relatedArticles = computed(() => {
    const post = this.article();
    return post ? getRelatedArticles(post) : [];
  });

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const slug = params.get('slug') ?? '';
      const found = getArticleBySlug(slug);
      this.article.set(found);
      this.copied.set(false);
      if (found) {
        this.seoService.setArticleSeo(found);
      }
      if (typeof window !== 'undefined') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  copyArticleLink(url: string): void {
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      navigator.clipboard.writeText(url).then(() => {
        this.copied.set(true);
        setTimeout(() => this.copied.set(false), 2600);
      });
    }
  }

  getCategoryName(slug: BlogCategorySlug): string {
    return getCategoryBySlug(slug)?.shortName ?? 'Editorial';
  }

  formatDate(isoDate: string): string {
    const [year, month, day] = isoDate.split('-');
    return `${day}/${month}/${year}`;
  }
}
