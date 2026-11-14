import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../shared/icon.component';
import { RevealDirective } from '../shared/reveal.directive';
import { BLOG_ARTICLES, BLOG_CATEGORIES, getCategoryBySlug } from './blog.data';
import { BlogArticle, BlogCategorySlug } from './blog.models';

@Component({
  selector: 'app-blog-section',
  standalone: true,
  imports: [RouterLink, IconComponent, RevealDirective],
  template: `
    <section id="blog" class="blog-home-section" aria-labelledby="blog-section-title">
      <div class="container">
        <!-- Cabeçalho Editorial -->
        <div class="section-header" boReveal>
          <span class="eyebrow-tag">CONHECIMENTO QUE CONECTA</span>
          <h2 id="blog-section-title" class="section-title">
            Novas ideias para quem vive a gastronomia.
          </h2>
          <p class="section-lead">
            Histórias, tendências e conhecimentos sobre restaurantes, tecnologia e experiências
            gastronômicas. Um espaço para descobrir, aprender e enxergar novas possibilidades.
          </p>
        </div>

        <!-- Filtros por Pilar Editorial (sem recarregar a página) -->
        <div class="category-pills" role="tablist" aria-label="Filtrar artigos por tema" boReveal>
          <button
            type="button"
            role="tab"
            class="cat-pill"
            [class.active]="selectedCategory() === 'all'"
            [attr.aria-selected]="selectedCategory() === 'all'"
            (click)="selectCategory('all')"
          >
            <span>Todos os temas</span>
          </button>
          @for (cat of categories; track cat.id) {
            <button
              type="button"
              role="tab"
              class="cat-pill"
              [class.active]="selectedCategory() === cat.slug"
              [attr.aria-selected]="selectedCategory() === cat.slug"
              (click)="selectCategory(cat.slug)"
            >
              <bo-icon [name]="cat.icon" [size]="15" />
              <span>{{ cat.shortName }}</span>
            </button>
          }
        </div>

        <!-- Destaque Editorial Principal -->
        @if (featuredArticle(); as featured) {
          <article class="featured-card" boReveal>
            <a [routerLink]="['/blog', featured.slug]" class="featured-media-link" [attr.aria-label]="featured.title">
              <figure class="featured-figure">
                <img
                  [src]="featured.coverImage"
                  [attr.srcset]="featured.coverSrcset || null"
                  [attr.sizes]="featured.coverSizes || null"
                  [alt]="featured.coverAlt"
                  [width]="featured.coverWidth"
                  [height]="featured.coverHeight"
                  loading="lazy"
                  decoding="async"
                />
              </figure>
            </a>

            <div class="featured-body">
              <div class="article-meta-top">
                <span class="cat-badge">{{ getCategoryName(featured.category) }}</span>
                <span class="meta-dot" aria-hidden="true">•</span>
                <span class="featured-tag">Destaque editorial</span>
              </div>

              <h3 class="featured-title">
                <a [routerLink]="['/blog', featured.slug]">{{ featured.title }}</a>
              </h3>

              <p class="featured-excerpt">{{ featured.excerpt }}</p>

              <div class="article-meta-footer">
                <div class="meta-info">
                  <time [attr.datetime]="featured.publishedAt">{{ formatDate(featured.publishedAt) }}</time>
                  <span class="meta-dot" aria-hidden="true">•</span>
                  <span>{{ featured.readingTime }} min de leitura</span>
                </div>

                <a [routerLink]="['/blog', featured.slug]" class="read-link">
                  <span>Ler artigo</span>
                  <bo-icon name="arrow-right" [size]="16" />
                </a>
              </div>
            </div>
          </article>
        }

        <!-- Grade Responsiva de Artigos -->
        <div class="articles-grid" aria-label="Mais artigos do Blog Boraê">
          @for (article of gridArticles(); track article.id; let idx = $index) {
            <article class="article-card" [class.highlighted]="selectedCategory() === article.category" [boReveal]="idx * 60">
              <a [routerLink]="['/blog', article.slug]" class="card-media-link" [attr.aria-label]="article.title">
                <figure class="card-figure">
                  <img
                    [src]="article.coverImage"
                    [attr.srcset]="article.coverSrcset || null"
                    [attr.sizes]="article.coverSizes || null"
                    [alt]="article.coverAlt"
                    [width]="article.coverWidth"
                    [height]="article.coverHeight"
                    loading="lazy"
                    decoding="async"
                  />
                </figure>
              </a>

              <div class="card-body">
                <div class="article-meta-top">
                  <a
                    [routerLink]="['/blog']"
                    [queryParams]="{ categoria: article.category }"
                    class="cat-badge-link"
                  >
                    {{ getCategoryName(article.category) }}
                  </a>
                </div>

                <h3 class="card-title">
                  <a [routerLink]="['/blog', article.slug]">{{ article.title }}</a>
                </h3>

                <p class="card-excerpt">{{ article.excerpt }}</p>

                <div class="card-footer">
                  <div class="meta-info">
                    <time [attr.datetime]="article.publishedAt">{{ formatDate(article.publishedAt) }}</time>
                    <span class="meta-dot" aria-hidden="true">•</span>
                    <span>{{ article.readingTime }} min de leitura</span>
                  </div>

                  <a [routerLink]="['/blog', article.slug]" class="read-link">
                    <span>Ler matéria</span>
                    <bo-icon name="arrow-right" [size]="15" />
                  </a>
                </div>
              </div>
            </article>
          }
        </div>

        <!-- CTA da Seção -->
        <div class="section-cta-box" boReveal>
          <div class="cta-copy">
            <h3 class="cta-title">Continue explorando o universo da gastronomia.</h3>
            <p class="cta-sub">
              Acesse todos os artigos divididos pelos cinco pilares editoriais do Boraê.
            </p>
          </div>
          <a routerLink="/blog" class="btn btn-primary">
            <span>Ver todos os artigos</span>
            <bo-icon class="btn-arrow" name="arrow-right" [size]="18" />
          </a>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .blog-home-section {
      padding: clamp(64px, 9vw, 112px) 0;
      background-color: var(--color-cream);
      border-top: 1px solid var(--color-border);
    }
    .section-header {
      text-align: center;
      max-width: 780px;
      margin: 0 auto 32px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 14px;
    }
    .section-title {
      font-size: clamp(30px, 4vw, 44px);
      font-weight: 800;
      color: var(--color-graphite);
    }
    .section-lead {
      font-size: clamp(16px, 1.8vw, 18px);
      line-height: 1.65;
      color: var(--color-text-muted);
    }
    .category-pills {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 10px;
      margin-bottom: 36px;
    }
    .cat-pill {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 9px 16px;
      border-radius: var(--radius-pill);
      border: 1px solid var(--color-border);
      background: var(--color-white);
      color: var(--color-text-muted);
      font-size: 13.5px;
      font-weight: 600;
      transition: all var(--transition-fast);
    }
    .cat-pill:hover {
      border-color: var(--color-primary);
      color: var(--color-graphite);
    }
    .cat-pill.active {
      background: var(--color-graphite);
      color: var(--color-white);
      border-color: var(--color-graphite);
    }
    .featured-card {
      display: grid;
      grid-template-columns: 1fr;
      background: var(--color-white);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
      overflow: hidden;
      box-shadow: var(--shadow-sm);
      margin-bottom: 28px;
    }
    @media (min-width: 960px) {
      .featured-card {
        grid-template-columns: 1.15fr 1fr;
      }
    }
    .featured-figure, .card-figure {
      margin: 0;
      aspect-ratio: 16 / 9;
      overflow: hidden;
      background: #EFEAE4;
    }
    .featured-figure img, .card-figure img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform var(--transition-normal);
    }
    .featured-card:hover .featured-figure img,
    .article-card:hover .card-figure img {
      transform: scale(1.03);
    }
    .featured-body {
      padding: clamp(24px, 3.5vw, 40px);
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 16px;
    }
    .article-meta-top {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      font-size: 12.5px;
      font-weight: 700;
    }
    .cat-badge, .cat-badge-link {
      color: var(--color-primary-dark);
      background: var(--color-primary-light);
      padding: 4px 11px;
      border-radius: var(--radius-pill);
      text-transform: uppercase;
      letter-spacing: 0.04em;
      font-size: 11.5px;
    }
    .featured-tag {
      color: var(--color-text-muted);
      font-weight: 600;
    }
    .featured-title {
      font-size: clamp(22px, 2.6vw, 30px);
      font-weight: 800;
      line-height: 1.22;
    }
    .featured-title a:hover, .card-title a:hover {
      color: var(--color-primary-dark);
    }
    .featured-excerpt {
      font-size: 15.5px;
      line-height: 1.65;
      color: var(--color-text-muted);
    }
    .article-meta-footer, .card-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding-top: 12px;
      border-top: 1px solid var(--color-border-subtle);
      margin-top: auto;
    }
    .meta-info {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: var(--color-text-muted);
    }
    .read-link {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 13.5px;
      font-weight: 700;
      color: var(--color-primary-dark);
    }
    .articles-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 24px;
    }
    @media (min-width: 680px) {
      .articles-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
    @media (min-width: 1040px) {
      .articles-grid {
        grid-template-columns: repeat(4, 1fr);
      }
    }
    .article-card {
      background: var(--color-white);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
      overflow: hidden;
      display: flex;
      flex-direction: column;
      transition: transform var(--transition-fast), box-shadow var(--transition-fast), border-color var(--transition-fast);
    }
    .article-card:hover, .article-card.highlighted {
      transform: translateY(-4px);
      box-shadow: var(--shadow-md);
      border-color: var(--color-primary);
    }
    .card-body {
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      flex: 1;
    }
    .card-title {
      font-size: 17.5px;
      font-weight: 800;
      line-height: 1.3;
    }
    .card-excerpt {
      font-size: 14px;
      line-height: 1.6;
      color: var(--color-text-muted);
    }
    .section-cta-box {
      margin-top: 40px;
      padding: 28px 32px;
      background: var(--color-white);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      justify-content: space-between;
      gap: 20px;
    }
    @media (min-width: 768px) {
      .section-cta-box {
        flex-direction: row;
        align-items: center;
      }
    }
    .cta-title {
      font-size: 20px;
      font-weight: 800;
      margin-bottom: 4px;
    }
    .cta-sub {
      font-size: 14.5px;
      color: var(--color-text-muted);
    }
  `]
})
export class BlogSectionComponent {
  readonly categories = BLOG_CATEGORIES;
  readonly allArticles = BLOG_ARTICLES;
  readonly selectedCategory = signal<'all' | BlogCategorySlug>('all');

  readonly featuredArticle = computed<BlogArticle>(() => {
    const current = this.selectedCategory();
    if (current !== 'all') {
      const match = this.allArticles.find(a => a.category === current);
      if (match) return match;
    }
    return this.allArticles.find(a => a.featured) ?? this.allArticles[0];
  });

  readonly gridArticles = computed<BlogArticle[]>(() => {
    const featured = this.featuredArticle();
    return this.allArticles.filter(a => a.id !== featured.id);
  });

  selectCategory(slug: 'all' | BlogCategorySlug): void {
    this.selectedCategory.set(slug);
  }

  getCategoryName(slug: BlogCategorySlug): string {
    return getCategoryBySlug(slug)?.shortName ?? 'Editorial';
  }

  formatDate(isoDate: string): string {
    const [year, month, day] = isoDate.split('-');
    return `${day}/${month}/${year}`;
  }
}
