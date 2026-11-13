import { Directive, ElementRef, OnDestroy, OnInit, PLATFORM_ID, inject, input } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * Animação leve de entrada ao rolar a página.
 * Uso: <article boReveal>, ou com atraso em ms para efeito cascata: <article [boReveal]="120">.
 * - Só esconde o elemento quando o navegador suporta IntersectionObserver;
 *   sem JS o conteúdo continua visível.
 * - Respeita prefers-reduced-motion (o elemento aparece sem transição).
 * - Os estilos .reveal / .is-visible ficam em src/styles.css.
 */
@Directive({
  selector: '[boReveal]',
  standalone: true
})
export class RevealDirective implements OnInit, OnDestroy {
  readonly boReveal = input<number | string>(0);

  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly platformId = inject(PLATFORM_ID);
  private observer?: IntersectionObserver;
  private cleanupTimer?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId) || typeof IntersectionObserver === 'undefined') return;
    const node = this.el.nativeElement;
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion) return;

    const delay = Number(this.boReveal()) || 0;
    if (delay) node.style.setProperty('--reveal-delay', `${delay}ms`);
    node.classList.add('reveal');

    this.observer = new IntersectionObserver(entries => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          node.classList.add('is-visible');
          this.observer?.disconnect();
          // Ao fim da entrada, devolve o elemento aos estilos normais (hover, transições próprias).
          this.cleanupTimer = setTimeout(() => node.classList.remove('reveal', 'is-visible'), 750 + delay);
        }
      }
    }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });
    this.observer.observe(node);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    clearTimeout(this.cleanupTimer);
  }
}
