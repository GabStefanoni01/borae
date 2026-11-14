import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import {
  BLOG_ARTICLES,
  BORAE_CANONICAL_ORIGIN,
  getCategoryBySlug
} from './blog.data';
import { BlogArticle } from './blog.models';

export interface PageSeoConfig {
  title: string;
  description: string;
  path: string;
  type?: 'website' | 'article';
  image?: string;
  publishedTime?: string;
  modifiedTime?: string;
  section?: string;
  robots?: string;
}

@Injectable({
  providedIn: 'root'
})
export class BlogSeoService {
  private readonly titleService = inject(Title);
  private readonly metaService = inject(Meta);
  private readonly doc = inject(DOCUMENT);

  private readonly jsonLdScriptId = 'borae-structured-data-jsonld';

  setLandingSeo(): void {
    const canonicalUrl = `${BORAE_CANONICAL_ORIGIN}/`;
    const imageUrl = `${BORAE_CANONICAL_ORIGIN}/assets/images/borae-logo.png`;

    this.applyMetaTags({
      title: 'Boraê | Experiências gastronômicas mais inteligentes e cardápios digitais',
      description:
        'Conheça o Boraê, plataforma de tecnologia gastronômica que conecta restaurantes e consumidores por meio de presença digital, cardápios por QR Code e inovação.',
      path: '/',
      type: 'website',
      image: imageUrl
    });

    const structuredData = {
      '@context': 'https://schema.org',
      '@graph': [
        {
          '@type': 'Organization',
          '@id': `${BORAE_CANONICAL_ORIGIN}/#organization`,
          name: 'Boraê',
          url: canonicalUrl,
          logo: {
            '@type': 'ImageObject',
            url: imageUrl
          },
          description:
            'Plataforma gastronômica que aproxima restaurantes e consumidores por meio de cardápios digitais, descoberta e tecnologia.'
        },
        {
          '@type': 'WebSite',
          '@id': `${BORAE_CANONICAL_ORIGIN}/#website`,
          url: canonicalUrl,
          name: 'Boraê',
          inLanguage: 'pt-BR',
          publisher: {
            '@id': `${BORAE_CANONICAL_ORIGIN}/#organization`
          }
        }
      ]
    };

    this.setJsonLd(structuredData);
  }

  setBlogIndexSeo(): void {
    const canonicalUrl = `${BORAE_CANONICAL_ORIGIN}/blog`;
    const coverImage = `${BORAE_CANONICAL_ORIGIN}/assets/images/blog/chef-preparando-prato-restaurante.webp`;

    this.applyMetaTags({
      title: 'Blog Boraê | Gastronomia, tecnologia e inovação',
      description:
        'Explore conteúdos sobre gestão de restaurantes, cardápios digitais, gastronomia brasileira, tecnologia e relacionamento com clientes.',
      path: '/blog',
      type: 'website',
      image: coverImage
    });

    const structuredData = {
      '@context': 'https://schema.org',
      '@graph': [
        {
          '@type': 'Blog',
          '@id': `${canonicalUrl}#blog`,
          url: canonicalUrl,
          name: 'Blog Boraê',
          description:
            'Explore conteúdos sobre gestão de restaurantes, cardápios digitais, gastronomia brasileira, tecnologia e relacionamento com clientes.',
          inLanguage: 'pt-BR',
          publisher: {
            '@type': 'Organization',
            name: 'Boraê',
            url: `${BORAE_CANONICAL_ORIGIN}/`,
            logo: {
              '@type': 'ImageObject',
              url: `${BORAE_CANONICAL_ORIGIN}/assets/images/borae-logo.png`
            }
          },
          blogPost: BLOG_ARTICLES.map(article => ({
            '@type': 'BlogPosting',
            headline: article.title,
            description: article.description,
            url: article.canonicalUrl,
            datePublished: article.publishedAt,
            dateModified: article.updatedAt ?? article.publishedAt,
            image: this.toAbsoluteUrl(article.coverImage),
            author: {
              '@type': 'Organization',
              name: article.author.name
            }
          }))
        },
        {
          '@type': 'BreadcrumbList',
          itemListElement: [
            {
              '@type': 'ListItem',
              position: 1,
              name: 'Início',
              item: `${BORAE_CANONICAL_ORIGIN}/`
            },
            {
              '@type': 'ListItem',
              position: 2,
              name: 'Blog',
              item: canonicalUrl
            }
          ]
        }
      ]
    };

    this.setJsonLd(structuredData);
  }

  setArticleSeo(article: BlogArticle): void {
    const category = getCategoryBySlug(article.category);
    const imageUrl = this.toAbsoluteUrl(article.coverImage);

    this.applyMetaTags({
      title: article.seoTitle,
      description: article.description,
      path: `/blog/${article.slug}`,
      type: 'article',
      image: imageUrl,
      publishedTime: article.publishedAt,
      modifiedTime: article.updatedAt ?? article.publishedAt,
      section: category?.name ?? 'Blog Boraê'
    });

    const structuredData = {
      '@context': 'https://schema.org',
      '@graph': [
        {
          '@type': 'BlogPosting',
          '@id': `${article.canonicalUrl}#article`,
          mainEntityOfPage: {
            '@type': 'WebPage',
            '@id': article.canonicalUrl
          },
          headline: article.title,
          description: article.description,
          image: [imageUrl],
          datePublished: article.publishedAt,
          dateModified: article.updatedAt ?? article.publishedAt,
          inLanguage: 'pt-BR',
          articleSection: category?.name ?? 'Gastronomia e Tecnologia',
          keywords: article.tags.join(', '),
          author: {
            '@type': 'Organization',
            name: article.author.name,
            url: `${BORAE_CANONICAL_ORIGIN}/`
          },
          publisher: {
            '@type': 'Organization',
            name: 'Boraê',
            url: `${BORAE_CANONICAL_ORIGIN}/`,
            logo: {
              '@type': 'ImageObject',
              url: `${BORAE_CANONICAL_ORIGIN}/assets/images/borae-logo.png`
            }
          }
        },
        {
          '@type': 'BreadcrumbList',
          itemListElement: [
            {
              '@type': 'ListItem',
              position: 1,
              name: 'Início',
              item: `${BORAE_CANONICAL_ORIGIN}/`
            },
            {
              '@type': 'ListItem',
              position: 2,
              name: 'Blog',
              item: `${BORAE_CANONICAL_ORIGIN}/blog`
            },
            {
              '@type': 'ListItem',
              position: 3,
              name: article.title,
              item: article.canonicalUrl
            }
          ]
        }
      ]
    };

    this.setJsonLd(structuredData);
  }

  private applyMetaTags(config: PageSeoConfig): void {
    const cleanPath = config.path.startsWith('/') ? config.path : `/${config.path}`;
    const canonicalUrl =
      cleanPath === '/'
        ? `${BORAE_CANONICAL_ORIGIN}/`
        : `${BORAE_CANONICAL_ORIGIN}${cleanPath}`;
    const ogType = config.type ?? 'website';
    const imageUrl = config.image
      ? this.toAbsoluteUrl(config.image)
      : `${BORAE_CANONICAL_ORIGIN}/assets/images/borae-logo.png`;

    this.titleService.setTitle(config.title);

    this.metaService.updateTag({ name: 'description', content: config.description });
    this.metaService.updateTag({ name: 'robots', content: config.robots ?? 'index, follow' });

    // Open Graph
    this.metaService.updateTag({ property: 'og:type', content: ogType });
    this.metaService.updateTag({ property: 'og:title', content: config.title });
    this.metaService.updateTag({ property: 'og:description', content: config.description });
    this.metaService.updateTag({ property: 'og:url', content: canonicalUrl });
    this.metaService.updateTag({ property: 'og:image', content: imageUrl });
    this.metaService.updateTag({ property: 'og:locale', content: 'pt_BR' });
    this.metaService.updateTag({ property: 'og:site_name', content: 'Boraê' });

    if (ogType === 'article') {
      if (config.publishedTime) {
        this.metaService.updateTag({
          property: 'article:published_time',
          content: config.publishedTime
        });
      }
      if (config.modifiedTime) {
        this.metaService.updateTag({
          property: 'article:modified_time',
          content: config.modifiedTime
        });
      }
      if (config.section) {
        this.metaService.updateTag({
          property: 'article:section',
          content: config.section
        });
      }
    } else {
      this.metaService.removeTag("property='article:published_time'");
      this.metaService.removeTag("property='article:modified_time'");
      this.metaService.removeTag("property='article:section'");
    }

    // Twitter / X Cards
    this.metaService.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.metaService.updateTag({ name: 'twitter:title', content: config.title });
    this.metaService.updateTag({ name: 'twitter:description', content: config.description });
    this.metaService.updateTag({ name: 'twitter:image', content: imageUrl });

    this.updateCanonicalLink(canonicalUrl);
  }

  private updateCanonicalLink(url: string): void {
    if (!this.doc?.head) return;
    let linkEl = this.doc.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!linkEl) {
      linkEl = this.doc.createElement('link');
      linkEl.setAttribute('rel', 'canonical');
      this.doc.head.appendChild(linkEl);
    }
    linkEl.setAttribute('href', url);
  }

  private setJsonLd(payload: Record<string, unknown>): void {
    if (!this.doc?.head) return;
    let scriptEl = this.doc.getElementById(this.jsonLdScriptId) as HTMLScriptElement | null;
    if (!scriptEl) {
      scriptEl = this.doc.createElement('script');
      scriptEl.id = this.jsonLdScriptId;
      scriptEl.type = 'application/ld+json';
      this.doc.head.appendChild(scriptEl);
    }
    scriptEl.textContent = JSON.stringify(payload);
  }

  private toAbsoluteUrl(pathOrUrl: string): string {
    if (pathOrUrl.startsWith('http://') || pathOrUrl.startsWith('https://')) {
      return pathOrUrl;
    }
    const clean = pathOrUrl.startsWith('/') ? pathOrUrl.slice(1) : pathOrUrl;
    return `${BORAE_CANONICAL_ORIGIN}/${clean}`;
  }
}
