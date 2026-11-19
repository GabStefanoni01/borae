import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy, Component, ElementRef, HostListener, computed, effect, inject, input, output, signal,
  untracked, viewChild
} from '@angular/core';
import { IconComponent } from '../shared/icon.component';
import { formatCents } from '../shared/money';
import {
  AssistantApiErrorBody, AssistantInteractionFilters, AssistantQuota, AssistantRecommendation, PublicAssistantService
} from './public-assistant.service';
import { PublicProduct, PublicRestaurantMenu } from './public-menu.service';

export interface AssistantChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
  recommendations?: AssistantRecommendation[];
  kind?: 'normal' | 'fallback' | 'error';
  retryPrompt?: string;
}

const STARTER_PROMPTS: readonly string[] = [
  'Qual prato você recomenda hoje?',
  'Quero uma opção mais leve',
  'Qual opção é mais econômica?',
  'Sugestão para compartilhar'
];

const BUDGET_PRESETS: readonly { label: string; cents: number }[] = [
  { label: 'Até R$ 35', cents: 3500 },
  { label: 'Até R$ 55', cents: 5500 },
  { label: 'Até R$ 80', cents: 8000 }
];

@Component({
  selector: 'bo-public-assistant-drawer',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent],
  styleUrls: ['./public-assistant-drawer.component.css', './public-assistant-drawer.extras.css'],
  template: `
    <button
      type="button"
      class="asst-fab"
      aria-haspopup="dialog"
      [attr.aria-expanded]="open()"
      (click)="openDrawer()"
    >
      <bo-icon name="sparkles" [size]="18" />
      <span>Assistente</span>
    </button>

    @if (open()) {
      <div class="asst-backdrop" (click)="onBackdropClick($event)">
        <section
          class="asst-panel"
          role="dialog"
          aria-modal="true"
          aria-labelledby="asst-title"
          (click)="$event.stopPropagation()"
        >
          <header class="asst-head">
            <div class="asst-head-id">
              <span class="asst-badge" aria-hidden="true"><bo-icon name="sparkles" [size]="18" /></span>
              <div class="asst-head-text">
                <h2 id="asst-title">Assistente do cardápio</h2>
                <span class="asst-head-sub">{{ restaurant().name }}</span>
              </div>
            </div>
            <button type="button" class="asst-close" aria-label="Fechar assistente" (click)="closeDrawer()">
              <bo-icon name="x" [size]="18" />
            </button>
          </header>

          <div class="asst-body" #scrollBody aria-live="polite">
            <p class="asst-notice">
              <bo-icon name="info" [size]="15" />
              <span>Sugestões feitas apenas com os itens disponíveis hoje. Suas perguntas não ficam salvas; para alergias, confirme diretamente com o restaurante.</span>
            </p>

            @for (msg of messages(); track msg.id) {
              <div class="asst-msg" [class.user]="msg.role === 'user'" [class.assistant]="msg.role === 'assistant'" [class.error]="msg.kind === 'error'" [class.fallback]="msg.kind === 'fallback'">
                <div class="asst-bubble">{{ msg.text }}</div>

                @if (msg.recommendations?.length) {
                  <ul class="asst-recs" aria-label="Produtos recomendados">
                    @for (rec of msg.recommendations; track rec.productId) {
                      <li>
                        <button type="button" class="asst-rec" (click)="onRecommendationClick(rec)">
                          @if (rec.imageUrl) {
                            <img class="asst-rec-img" [src]="rec.imageUrl" [alt]="rec.name" loading="lazy" />
                          }
                          <div class="asst-rec-body">
                            <div class="asst-rec-top">
                              <span class="asst-rec-name">{{ rec.name }}</span>
                              <span class="asst-rec-price">{{ price(rec.priceInCents) }}</span>
                            </div>
                            <p class="asst-rec-reason">{{ rec.reason }}</p>
                            @if (rec.ingredients.length) {
                              <p class="asst-rec-ing">{{ rec.ingredients.join(', ') }}</p>
                            }
                            <span class="asst-rec-cta">Ver detalhes <bo-icon name="chevron-right" [size]="14" /></span>
                          </div>
                        </button>
                      </li>
                    }
                  </ul>
                }

                @if (msg.retryPrompt && !sending() && !quotaExhausted()) {
                  <button type="button" class="asst-retry" (click)="sendPrompt(msg.retryPrompt!)">
                    <bo-icon name="refresh" [size]="14" /> Tentar novamente
                  </button>
                }
              </div>
            }

            @if (sending()) {
              <div class="asst-typing" role="status" aria-label="Consultando o cardápio">
                <bo-icon name="loader" [size]="16" [spin]="true" />
                <span>Consultando o cardápio...</span>
              </div>
            }

            @if (showStarters()) {
              <div class="asst-chips" aria-label="Sugestões rápidas">
                @for (prompt of starterPrompts; track prompt) {
                  <button type="button" class="asst-chip" [disabled]="sending() || quotaExhausted()" (click)="sendPrompt(prompt)">
                    {{ prompt }}
                  </button>
                }
              </div>
            }
          </div>

          @if (filtersOpen()) {
            <div class="asst-filters" aria-label="Filtros opcionais do assistente">
              <div class="asst-filter-row">
                <span class="asst-filter-label">Orçamento máximo</span>
                @for (preset of budgetPresets; track preset.cents) {
                  <button
                    type="button"
                    class="asst-pill"
                    [class.active]="maxPriceInCents() === preset.cents"
                    (click)="toggleBudget(preset.cents)"
                  >{{ preset.label }}</button>
                }
              </div>
              <div class="asst-filter-row">
                <label class="asst-filter-label" for="asst-exclude">Evitar ingrediente (opcional)</label>
                <input
                  id="asst-exclude"
                  class="asst-filter-input"
                  type="text"
                  maxlength="120"
                  placeholder="Ex.: amendoim, pimenta, queijo"
                  [value]="excludedInput()"
                  (input)="excludedInput.set($any($event.target).value)"
                />
              </div>
            </div>
          }

          <footer class="asst-foot">
            <div class="asst-foot-meta">
              <button
                type="button"
                class="asst-filter-toggle"
                [class.active]="filtersOpen() || activeFilterCount() > 0"
                (click)="filtersOpen.set(!filtersOpen())"
              >
                <bo-icon name="sliders" [size]="13" />
                <span>Filtros{{ activeFilterCount() ? ' (' + activeFilterCount() + ')' : '' }}</span>
              </button>

              @if (quotaLabel(); as q) {
                <span class="asst-quota">{{ q }}</span>
              } @else if (draft().length > 380) {
                <span>{{ draft().length }}/500</span>
              }
            </div>

            <form class="asst-form" (submit)="onSubmit($event)">
              <input
                #promptInput
                class="asst-input"
                type="text"
                name="assistantPrompt"
                maxlength="500"
                autocomplete="off"
                [disabled]="sending() || quotaExhausted()"
                [placeholder]="quotaExhausted() ? 'Limite diário atingido. Explore o cardápio normalmente.' : 'Pergunte o que comer, combinações ou filtros...'"
                aria-label="Mensagem para o assistente gastronômico"
                [value]="draft()"
                (input)="draft.set($any($event.target).value)"
              />
              <button
                type="submit"
                class="asst-send"
                aria-label="Enviar mensagem"
                [disabled]="!canSend()"
              >
                <bo-icon name="arrow-right" [size]="18" />
              </button>
            </form>
          </footer>
        </section>
      </div>
    }
  `
})
export class PublicAssistantDrawerComponent {
  readonly restaurant = input.required<PublicRestaurantMenu>();
  readonly selectedMenuId = input<string | null>(null);
  readonly selectProduct = output<PublicProduct>();

  private readonly assistantService = inject(PublicAssistantService);
  private readonly scrollBody = viewChild<ElementRef<HTMLElement>>('scrollBody');
  private readonly promptInput = viewChild<ElementRef<HTMLInputElement>>('promptInput');

  protected readonly open = signal(false);
  protected readonly sending = signal(false);
  protected readonly draft = signal('');
  protected readonly messages = signal<AssistantChatMessage[]>([]);
  protected readonly quota = signal<AssistantQuota | null>(null);
  protected readonly quotaExhausted = signal(false);
  protected readonly filtersOpen = signal(false);
  protected readonly maxPriceInCents = signal<number | null>(null);
  protected readonly excludedInput = signal('');

  protected get starterPrompts(): readonly string[] { return STARTER_PROMPTS; }
  protected get budgetPresets(): readonly { label: string; cents: number }[] { return BUDGET_PRESETS; }

  private lastRestaurantSlug = '';
  private messageCounter = 0;

  protected readonly canSend = computed(() =>
    !this.sending() && !this.quotaExhausted() && this.draft().trim().length > 0
  );

  protected readonly showStarters = computed(() =>
    this.messages().length <= 1 && !this.sending() && !this.quotaExhausted()
  );

  protected readonly activeFilterCount = computed(() => {
    let count = 0;
    if (this.maxPriceInCents() !== null) count++;
    if (this.parseExcludedIngredients().length > 0) count++;
    return count;
  });

  protected readonly quotaLabel = computed(() => {
    const q = this.quota();
    if (!q) return null;
    if (this.quotaExhausted() || q.remainingConsumer <= 0 || q.remainingRestaurant <= 0) {
      return 'Limite diário atingido';
    }
    const remaining = Math.min(q.remainingConsumer, q.remainingRestaurant);
    return `${remaining} ${remaining === 1 ? 'pergunta restante' : 'perguntas restantes'} hoje`;
  });

  /** Índice rápido de produtos carregados no cardápio para abrir o detalhe completo com 3D e adicionais. */
  private readonly productsById = computed(() => {
    const map = new Map<string, PublicProduct>();
    for (const menu of this.restaurant().menus) {
      for (const category of menu.categories) {
        for (const product of category.products) {
          map.set(product.id, product);
        }
      }
    }
    return map;
  });

  constructor() {
    effect(() => {
      const current = this.restaurant();
      if (current.slug !== this.lastRestaurantSlug) {
        this.lastRestaurantSlug = current.slug;
        untracked(() => this.resetForRestaurant(current));
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open()) {
      this.closeDrawer();
    }
  }

  openDrawer(): void {
    this.open.set(true);
    this.assistantService.ensureSession().subscribe({ error: () => void 0 });
    this.scheduleScrollAndFocus();
  }

  closeDrawer(): void {
    this.open.set(false);
  }

  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeDrawer();
    }
  }

  protected toggleBudget(cents: number): void {
    this.maxPriceInCents.set(this.maxPriceInCents() === cents ? null : cents);
  }

  protected onSubmit(event?: Event): void {
    event?.preventDefault();
    const text = this.draft().trim();
    if (!text || this.sending() || this.quotaExhausted()) return;
    this.draft.set('');
    this.sendPrompt(text);
  }

  protected sendPrompt(rawPrompt: string): void {
    const prompt = rawPrompt.trim().slice(0, 500);
    if (!prompt || this.sending() || this.quotaExhausted()) return;

    this.appendMessage({ role: 'user', text: prompt });
    this.sending.set(true);
    this.scrollToBottom();

    const filters = this.buildFilters();
    const slug = this.restaurant().slug;

    this.assistantService
      .interact(slug, {
        message: prompt,
        menuId: this.selectedMenuId(),
        filters
      })
      .subscribe({
        next: response => {
          this.sending.set(false);
          if (response.quota) {
            this.quota.set(response.quota);
            if (response.quota.remainingConsumer <= 0 || response.quota.remainingRestaurant <= 0) {
              this.quotaExhausted.set(true);
            }
          }
          this.appendMessage({
            role: 'assistant',
            text: response.answer,
            recommendations: response.recommendations,
            kind: response.fallback ? 'fallback' : 'normal'
          });
          this.scrollToBottom();
        },
        error: (error: unknown) => {
          this.sending.set(false);
          this.handleInteractionError(error, prompt);
          this.scrollToBottom();
        }
      });
  }

  protected onRecommendationClick(rec: AssistantRecommendation): void {
    this.assistantService.trackRecommendationClick(this.restaurant().slug, rec.productId).subscribe();
    const fullProduct = this.productsById().get(rec.productId) ?? {
      id: rec.productId,
      name: rec.name,
      description: rec.reason,
      priceInCents: rec.priceInCents,
      availability: rec.availability,
      displayOrder: 0,
      ingredients: rec.ingredients,
      addons: [],
      imageUrl: rec.imageUrl
    };
    this.selectProduct.emit(fullProduct);
  }

  protected price(cents: number): string {
    return formatCents(cents);
  }

  private resetForRestaurant(restaurant: PublicRestaurantMenu): void {
    this.open.set(false);
    this.sending.set(false);
    this.draft.set('');
    this.quota.set(null);
    this.quotaExhausted.set(false);
    this.filtersOpen.set(false);
    this.maxPriceInCents.set(null);
    this.excludedInput.set('');
    this.messages.set([
      {
        id: this.nextId(),
        role: 'assistant',
        text: `Olá! Sou o assistente gastronômico de ${restaurant.name}. Me diga o que você tem vontade de comer hoje e eu sugiro opções disponíveis no cardápio.`,
        kind: 'normal'
      }
    ]);
  }

  private buildFilters(): AssistantInteractionFilters | null {
    const maxPrice = this.maxPriceInCents();
    const excluded = this.parseExcludedIngredients();
    if (maxPrice === null && excluded.length === 0) return null;
    return {
      maxPriceInCents: maxPrice,
      excludedIngredients: excluded.length ? excluded : null
    };
  }

  private parseExcludedIngredients(): string[] {
    return this.excludedInput()
      .split(',')
      .map(item => item.trim())
      .filter(item => item.length > 0)
      .slice(0, 20);
  }

  private handleInteractionError(error: unknown, originalPrompt: string): void {
    if (error instanceof HttpErrorResponse) {
      const body = (error.error ?? {}) as AssistantApiErrorBody;
      if (error.status === 429) {
        if (body.code === 'DAILY_QUOTA_EXCEEDED') {
          this.quotaExhausted.set(true);
          const resetInfo = this.formatResetHint(body.resetsAt);
          const msg = body.scope === 'RESTAURANT'
            ? `O assistente deste restaurante atingiu o limite diário de uso${resetInfo}. Você ainda pode explorar o cardápio normalmente.`
            : `Você atingiu o limite diário de perguntas ao assistente${resetInfo}. Você ainda pode explorar o cardápio normalmente.`;
          this.appendMessage({ role: 'assistant', text: msg, kind: 'fallback' });
          return;
        }
        const waitSeconds = body.retryAfterSeconds;
        const waitHint = waitSeconds ? ` Aguarde cerca de ${waitSeconds} segundos e tente novamente.` : ' Aguarde alguns instantes e tente novamente.';
        this.appendMessage({
          role: 'assistant',
          text: `Muitas solicitações em pouco tempo.${waitHint}`,
          kind: 'error',
          retryPrompt: originalPrompt
        });
        return;
      }

      if (error.status === 0) {
        this.appendMessage({
          role: 'assistant',
          text: 'Não foi possível conectar ao assistente agora. Verifique sua conexão e tente novamente.',
          kind: 'error',
          retryPrompt: originalPrompt
        });
        return;
      }

      if (error.status === 400) {
        this.appendMessage({
          role: 'assistant',
          text: 'Não consegui processar sua mensagem. Tente escrevê-la de forma mais curta e objetiva.',
          kind: 'error'
        });
        return;
      }
    }

    this.appendMessage({
      role: 'assistant',
      text: 'O assistente está temporariamente indisponível. Você ainda pode explorar o cardápio normalmente.',
      kind: 'fallback',
      retryPrompt: originalPrompt
    });
  }

  private formatResetHint(resetsAt?: string): string {
    if (!resetsAt) return ' (renova à meia-noite)';
    const date = new Date(resetsAt);
    if (Number.isNaN(date.getTime())) return ' (renova à meia-noite)';
    const formatted = date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    return ` (renova às ${formatted})`;
  }

  private appendMessage(entry: Omit<AssistantChatMessage, 'id'>): void {
    this.messages.update(list => [...list, { ...entry, id: this.nextId() }]);
  }

  private nextId(): string {
    this.messageCounter += 1;
    return `m-${this.messageCounter}`;
  }

  private scrollToBottom(): void {
    queueMicrotask(() => {
      const el = this.scrollBody()?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }

  private scheduleScrollAndFocus(): void {
    queueMicrotask(() => {
      this.scrollToBottom();
      this.promptInput()?.nativeElement.focus();
    });
  }
}
