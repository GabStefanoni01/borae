import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext } from '../restaurant-context.service';
import { PUBLICATION_LABEL, RestaurantProfile, RestaurantProfileService } from '../profile/restaurant-profile.service';
import { ToastService } from '../ui/feedback';
import { ConfirmDialogComponent, PageHeaderComponent, StateComponent } from '../ui/ui.components';
import { catalogErrorMessage } from './catalog-errors';

/**
 * Página pública e QR Code — publicação do perfil do restaurante (POST publish/unpublish)
 * e QR Code principal (GET /qrcode, PNG gerado pelo backend). Somente OWNER e MANAGER.
 */
@Component({
  selector: 'bo-publication-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, IconComponent, PageHeaderComponent, StateComponent, ConfirmDialogComponent],
  template: `
    <bo-page-header title="Página pública e QR Code" subtitle="Publique o perfil do seu restaurante e compartilhe o acesso aos cardápios publicados." />

    @if (!ctx.canManageCatalog()) {
      <bo-state kind="forbidden" title="Disponível para proprietário e gerentes" text="A publicação do restaurante e o QR Code são gerenciados pelo proprietário e pelos gerentes." />
    } @else if (loading() && !profile()) {
      <div class="c-pub-grid" aria-busy="true" aria-label="Carregando publicação"><div class="c-skeleton c-skeleton-panel"></div><div class="c-skeleton c-skeleton-panel"></div></div>
    } @else if (loadError()) {
      <bo-state kind="error" title="Não foi possível carregar a publicação do restaurante." [text]="loadError()">
        <button type="button" class="d-btn d-btn-secondary" (click)="load()"><bo-icon name="refresh" [size]="16" /> Tentar novamente</button>
      </bo-state>
    } @else if (profile(); as p) {
      <div class="c-pub-grid">
        <section class="d-card c-pub-card" [attr.data-status]="p.publicationStatus" aria-labelledby="c-pub-title">
          <span class="c-pub-badge" [attr.data-status]="p.publicationStatus"><span class="c-status-dot" aria-hidden="true"></span>{{ statusLabel[p.publicationStatus] }}</span>
          @switch (p.publicationStatus) {
            @case ('PUBLISHED') {
              <h2 id="c-pub-title">Seu restaurante está publicado.</h2>
              <p>Sua página pública está disponível para consulta.</p>
              <div class="d-form-actions">
                <a class="d-btn d-btn-secondary" [routerLink]="['/r', p.slug]" target="_blank" rel="noopener"><bo-icon name="arrow-up-right" [size]="16" /> Ver página pública</a>
                <button type="button" class="d-btn d-btn-ghost" (click)="confirmUnpublish.set(true)" [disabled]="busy()">Despublicar restaurante</button>
              </div>
            }
            @case ('SUSPENDED') {
              <h2 id="c-pub-title">Seu restaurante está suspenso.</h2>
              <p>A página pública está indisponível e a publicação não pode ser alterada pelo painel. Fale com o suporte do Boraê para entender a situação.</p>
            }
            @default {
              <h2 id="c-pub-title">Seu restaurante ainda não está público.</h2>
              <p>Publique seu perfil para disponibilizar sua página pública e permitir que clientes acessem seus cardápios.</p>
              <div class="d-form-actions">
                <button type="button" class="d-btn d-btn-primary" (click)="publish()" [disabled]="busy()">
                  <bo-icon [name]="busy() ? 'loader' : 'globe'" [size]="16" [spin]="busy()" /> Publicar restaurante
                </button>
              </div>
            }
          }
          <ul class="c-pub-notes">
            <li><bo-icon name="info" [size]="14" /> A página mostra apenas cardápios publicados, categorias ativas e produtos não arquivados.</li>
            <li><bo-icon name="book-open" [size]="14" /> Publicar o restaurante não publica cardápios: cada cardápio tem sua própria publicação em <a routerLink="/app/cardapios">Cardápios</a>.</li>
          </ul>
        </section>

        <section class="d-card c-qr-card" aria-labelledby="c-qr-title">
          <h2 id="c-qr-title">QR Code do seu restaurante</h2>
          <p>Compartilhe o acesso à sua página pública. Seus clientes poderão escanear o código para conhecer seu restaurante e consultar os cardápios publicados.</p>
          <div class="c-qr-box">
            @if (qrLoading()) {
              <span class="c-qr-empty"><bo-icon name="loader" [size]="24" [spin]="true" /> Gerando QR Code...</span>
            } @else if (qrUrl()) {
              <img [src]="qrUrl()" [alt]="'QR Code da página pública de ' + p.name" width="240" height="240">
            } @else if (qrError()) {
              <span class="c-qr-empty c-qr-error" role="alert"><bo-icon name="alert" [size]="22" /> {{ qrError() }}</span>
            } @else {
              <span class="c-qr-empty"><bo-icon name="qr-code" [size]="32" /> Prévia indisponível</span>
            }
          </div>
          <div class="d-form-actions">
            <button type="button" class="d-btn d-btn-primary" (click)="downloadQr()" [disabled]="qrLoading()"><bo-icon name="arrow-right" [size]="16" class="c-rot-down" /> Baixar QR Code</button>
            @if (qrError()) { <button type="button" class="d-btn d-btn-secondary" (click)="loadQr()">Tentar novamente</button> }
          </div>
          @if (p.publicationStatus !== 'PUBLISHED') {
            <p class="c-inline-note"><bo-icon name="eye-off" [size]="14" /> Enquanto o restaurante não estiver publicado, quem escanear o código verá a mensagem de página indisponível.</p>
          }
          <p class="d-hint"><span>Um QR Code por restaurante. Ele abre a página pública, onde o cliente escolhe o cardápio.</span></p>
        </section>
      </div>
    }

    <bo-confirm-dialog [open]="confirmUnpublish()" title="Despublicar o restaurante?"
      message="Sua página pública deixará de ficar disponível. Cardápios e produtos continuam salvos no painel." confirmLabel="Despublicar"
      [danger]="true" [busy]="busy()" (confirmed)="unpublish()" (cancelled)="confirmUnpublish.set(false)" />
  `
})
export class PublicationPage {
  protected readonly ctx = inject(RestaurantContext);
  private readonly profiles = inject(RestaurantProfileService);
  private readonly toast = inject(ToastService);

  protected get statusLabel() { return PUBLICATION_LABEL; }
  protected readonly profile = signal<RestaurantProfile | null>(null);
  protected readonly loading = signal(false);
  protected readonly loadError = signal('');
  protected readonly busy = signal(false);
  protected readonly confirmUnpublish = signal(false);

  private readonly qrBlob = signal<Blob | null>(null);
  protected readonly qrUrl = signal<string | null>(null);
  protected readonly qrLoading = signal(false);
  protected readonly qrError = signal('');

  protected readonly fileName = computed(() => `qrcode-${sanitizeFileName(this.profile()?.name ?? 'restaurante')}.png`);

  constructor() {
    effect(() => {
      const id = this.ctx.selectedId();
      const allowed = this.ctx.canManageCatalog();
      untracked(() => {
        this.profile.set(null);
        this.setQr(null);
        if (id && allowed) this.load();
      });
    });
    inject(DestroyRef).onDestroy(() => this.setQr(null));
  }

  load(): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId) return;
    this.loading.set(true);
    this.loadError.set('');
    this.profiles.getProfile(restaurantId).subscribe({
      next: profile => {
        if (restaurantId !== this.ctx.selectedId()) return;
        this.profile.set(profile);
        this.loading.set(false);
        this.loadQr();
      },
      error: error => {
        this.loading.set(false);
        this.loadError.set(catalogErrorMessage(error, 'load-menu'));
      }
    });
  }

  loadQr(): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId) return;
    this.qrLoading.set(true);
    this.qrError.set('');
    this.profiles.downloadQrCode(restaurantId).subscribe({
      next: blob => {
        this.qrLoading.set(false);
        this.setQr(blob);
      },
      error: error => {
        this.qrLoading.set(false);
        this.setQr(null);
        this.qrError.set(catalogErrorMessage(error, 'qrcode'));
      }
    });
  }

  protected downloadQr(): void {
    const blob = this.qrBlob();
    if (!blob) { this.loadQr(); return; }
    saveBlob(blob, this.fileName());
  }

  protected publish(): void { this.change('publish'); }
  protected unpublish(): void { this.change('unpublish'); }

  private change(action: 'publish' | 'unpublish'): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId || this.busy()) return;
    this.busy.set(true);
    const request$ = action === 'publish' ? this.profiles.publish(restaurantId) : this.profiles.unpublish(restaurantId);
    request$.subscribe({
      next: profile => {
        this.busy.set(false);
        this.confirmUnpublish.set(false);
        this.profile.set(profile);
        this.toast.success(action === 'publish' ? 'Restaurante publicado. Sua página pública já está disponível.' : 'Restaurante despublicado.');
      },
      error: error => {
        this.busy.set(false);
        this.confirmUnpublish.set(false);
        this.toast.error(catalogErrorMessage(error, action === 'publish' ? 'publish-restaurant' : 'unpublish-restaurant'));
        // Recarrega para refletir o estado real (por exemplo, restaurante suspenso).
        this.load();
      }
    });
  }

  private setQr(blob: Blob | null): void {
    const previous = this.qrUrl();
    if (previous) URL.revokeObjectURL(previous);
    this.qrBlob.set(blob);
    this.qrUrl.set(blob ? URL.createObjectURL(blob) : null);
  }
}

/** "Cantina Bella Vista!" → "cantina-bella-vista". */
export function sanitizeFileName(name: string): string {
  const slug = name.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 60);
  return slug || 'restaurante';
}

function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(new Blob([blob], { type: 'image/png' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
