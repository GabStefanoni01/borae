import { BoraeIconName } from '../shared/icon.component';

export type BlogCategorySlug =
  | 'gestao-e-inovacao'
  | 'cardapios-e-experiencia'
  | 'cultura-e-gastronomia'
  | 'tecnologia-e-gastronomia'
  | 'marketing-e-relacionamento';

export interface BlogCategory {
  id: string;
  slug: BlogCategorySlug;
  name: string;
  shortName: string;
  description: string;
  audienceLabel: string;
  icon: BoraeIconName;
  accentColor: string;
}

export interface BlogImageCredit {
  label: string;
  photographer: string;
  sourceName: string;
  sourceUrl?: string;
}

export interface BlogInlineSegment {
  text: string;
  strong?: boolean;
  routerLink?: string;
  queryParams?: Record<string, string>;
  fragment?: string;
  externalHref?: string;
}

export type BlogContentBlock =
  | {
      type: 'paragraph';
      segments: BlogInlineSegment[];
    }
  | {
      type: 'heading';
      level: 2 | 3;
      id: string;
      text: string;
    }
  | {
      type: 'list';
      ordered?: boolean;
      items: {
        title?: string;
        segments: BlogInlineSegment[];
      }[];
    }
  | {
      type: 'callout';
      eyebrow: string;
      title: string;
      text: string;
      tone?: 'warm' | 'neutral';
    }
  | {
      type: 'image';
      src: string;
      srcset?: string;
      sizes?: string;
      alt: string;
      width: number;
      height: number;
      caption: string;
      credit: BlogImageCredit;
    };

export interface BlogExternalSource {
  title: string;
  publisher: string;
  url: string;
  note: string;
}

export interface BlogInternalLinkRef {
  label: string;
  description: string;
  routerLink: string;
  queryParams?: Record<string, string>;
  fragment?: string;
}

export interface BlogArticleCta {
  audience: 'restaurants' | 'consumers' | 'partners';
  eyebrow: string;
  title: string;
  description: string;
  buttonLabel: string;
  routerLink: string;
  fragment?: string;
  secondaryLabel?: string;
  secondaryRouterLink?: string;
  secondaryFragment?: string;
}

export interface BlogAuthor {
  name: string;
  role: string;
  bio: string;
}

export interface BlogArticle {
  id: string;
  slug: string;
  title: string;
  subtitle: string;
  seoTitle: string;
  description: string;
  excerpt: string;
  category: BlogCategorySlug;
  author: BlogAuthor;
  publishedAt: string;
  updatedAt?: string;
  readingTime: number;
  featured?: boolean;
  coverImage: string;
  coverSrcset?: string;
  coverSizes?: string;
  coverWidth: number;
  coverHeight: number;
  coverAlt: string;
  imageCredit: BlogImageCredit;
  content: BlogContentBlock[];
  tags: string[];
  relatedArticles: string[];
  internalLinks: BlogInternalLinkRef[];
  externalSources: BlogExternalSource[];
  cta: BlogArticleCta;
  canonicalUrl: string;
}
