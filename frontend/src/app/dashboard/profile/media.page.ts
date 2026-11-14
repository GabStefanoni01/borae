import { HttpEventType } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext } from '../restaurant-context.service';
import { ConfirmDialogComponent, StateComponent } from '../ui/ui.components';
import { MEDIA_LIMITS, MediaType, RestaurantMedia, RestaurantProfileService, profileErrorMessage } from './restaurant-profile.service';

interface UploadItem { id: number; name: string; type: MediaType; progress: number; error: string; }

/**
 * Aba Imagens — GET/POST(multipart)/DELETE /restaurants/{id}/media.
 * O servidor processa (redimensiona até 2.000 px, corrige orientação) e envia ao ImageKit;
 * a prévia usa a URL pública devolvida pela API. Enviar uma nova logo substitui a anterior.
 */
@Component({
  selector: 'bo-media-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [IconComponent, StateComponent, ConfirmDialogComponent],
  template: `
    @if (loading()) {
      <bo-state kind="loading" [compact]="true" title="Carregando imagens" />
    } @else if (loadError()) {
      <bo-state kind="error" [compact]="true" title="Não foi possível carregar as imagens" [text]="loadError()">
        <button type="button" class="d-btn d-btn-secondary" (click)="load()">Tentar novamente</button>
      </bo-state>
    } @else {
      @if (feedback(); as fb) {
        <div class="d-alert md-alert" [class.d-alert-success]="fb.type === 'success'" [class.d-alert-error]="fb.type === 'error'" [attr.role]="fb.type === 'error' ? 'alert' : 'status'">
          <bo-icon [name]="fb.type === 'success' ? 'check-circle' : 'alert'" [size]="18" /><p>{{ fb.text }}</p>
        </div>
      }

      <div class="md-grid">
        <!-- Logo -->
        <section class="d-card">
          <div class="d-card-head"><span class="d-card-icon"><bo-icon name="award" [size]="18" /></span><h2>Logo</h2></div>
          <div class="md-logo">
            @if (logo(); as l) {
              <img [src]="l.url" alt="Logo do restaurante" class="md-logo-img" loading="lazy">
            } @else {
              <div class="md-logo-empty"><bo-icon name="image" [size]="26" /><span>Sem logo</span></div>
            }
            <div class="md-logo-info">
              @if (logo(); as l) { <p class="md-meta">{{ l.width }}×{{ l.height }} px · {{ size(l.sizeBytes) }}</p> }
              <p class="md-meta">JPEG, PNG ou WebP · até 5 MB. Uma nova logo substitui a atual.</p>
              <div class="md-actions">
                <label class="d-btn d-btn-primary d-btn-sm md-file" [class.disabled]="busyLogo()">
                  <input type="file" accept="image/jpeg,image/png,image/webp" (change)="pick($event, 'LOGO')" [disabled]="busyLogo()">
                  <bo-icon name="image" [size]="14" /> {{ logo() ? 'Trocar logo' : 'Enviar logo' }}
                </label>
                @if (logo(); as l) {
                  <button type="button" class="d-btn d-btn-ghost d-btn-sm md-remove" (click)="pendingDelete.set(l)"><bo-icon name="trash" [size]="14" /> Remover</button>
                }
              </div>
            </div>
          </div>
        </section>

        <!-- Fotos -->
        <section class="d-card">
          <div class="d-card-head">
            <span class="d-card-icon"><bo-icon name="camera" [size]="18" /></span>
            <h2>Fotos</h2>
            <span class="md-count">{{ photos().length }}/{{ limits.maxPhotos }}</span>
          </div>
          <p class="md-meta">Ambiente, fachada e pratos. JPEG, PNG ou WebP · até 10 MB cada. Você pode selecionar várias de uma vez.</p>
          <label class="d-btn d-btn-secondary d-btn-sm md-file md-add" [class.disabled]="photos().length >= limits.maxPhotos">
            <input type="file" accept="image/jpeg,image/png,image/webp" multiple (change)="pick($event, 'PHOTO')" [disabled]="photos().length >= limits.maxPhotos">
            <bo-icon name="plus" [size]="14" /> Adicionar fotos
          </label>
        </section>
      </div>

      @if (uploads().length) {
        <ul class="md-uploads" aria-label="Envios">
          @for (u of uploads(); track u.id) {
            <li [class.failed]="u.error">
              <bo-icon [name]="u.error ? 'alert' : (u.progress < 100 ? 'loader' : 'check')" [size]="16" [spin]="!u.error && u.progress < 100" />
              <span class="md-up-name">{{ u.name }}</span>
              @if (u.error) {
                <span class="md-up-error">{{ u.error }}</span>
                <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="dismiss(u.id)" aria-label="Dispensar aviso"><bo-icon name="x" [size]="14" /></button>
              } @else {
                <span class="md-progress" role="progressbar" [attr.aria-valuenow]="u.progress" aria-valuemin="0" aria-valuemax="100"><span [style.width.%]="u.progress"></span></span>
                <span class="md-up-pct">{{ u.progress < 100 ? u.progress + '%' : 'Processando...' }}</span>
              }
            </li>
          }
        </ul>
      }

      <section class="d-section">
        @if (photos().length === 0) {
          <bo-state kind="empty" icon="camera" [compact]="true" title="Nenhuma foto ainda" text="Fotos ajudam o cliente a conhecer o ambiente e os pratos antes da visita." />
        } @else {
          <ul class="md-photos">
            @for (p of photos(); track p.id) {
              <li>
                <img [src]="p.url" [alt]="'Foto do restaurante enviada em ' + date(p.createdAt)" loading="lazy" [width]="p.width" [height]="p.height">
                <div class="md-photo-bar">
                  <span>{{ p.width }}×{{ p.height }} · {{ size(p.sizeBytes) }}</span>
                  <button type="button" class="md-photo-del" (click)="pendingDelete.set(p)" aria-label="Excluir foto"><bo-icon name="trash" [size]="14" /></button>
                </div>
              </li>
            }
          </ul>
        }
      </section>
    }

    <bo-confirm-dialog [open]="!!pendingDelete()" [danger]="true" [busy]="deleting()"
      [title]="pendingDelete()?.type === 'LOGO' ? 'Remover a logo?' : 'Excluir esta foto?'"
      message="A imagem será removida do restaurante e do armazenamento. Esta ação não pode ser desfeita."
      confirmLabel="Excluir" (confirmed)="confirmDelete()" (cancelled)="pendingDelete.set(null)" />
  `,
  styles: [`
    .md-alert{margin-bottom:16px}
    .md-grid{display:grid;gap:16px;grid-template-columns:repeat(auto-fit,minmax(300px,1fr))}
    .md-logo{display:flex;flex-wrap:wrap;gap:16px;align-items:flex-start}
    .md-logo-img,.md-logo-empty{width:112px;height:112px;border-radius:16px;border:1px solid var(--d-border);background:var(--d-head);object-fit:contain}
    .md-logo-empty{display:grid;place-items:center;align-content:center;gap:6px;color:var(--d-faint);font-size:12px;border-style:dashed}
    .md-logo-info{flex:1;min-width:180px;display:grid;gap:6px}
    .md-meta{margin:0;color:var(--d-muted);font-size:12.5px}
    .md-actions{display:flex;flex-wrap:wrap;gap:8px;margin-top:4px}
    .md-file{position:relative;overflow:hidden;cursor:pointer}
    .md-file input{position:absolute;inset:0;opacity:0;cursor:pointer}
    .md-file.disabled{opacity:.55;pointer-events:none}
    .md-file:focus-within{outline:2px solid var(--d-primary);outline-offset:2px}
    .md-add{margin-top:12px}
    .md-remove{color:var(--d-error)}
    .md-count{margin-left:auto;font-size:12.5px;font-weight:600;color:var(--d-muted)}
    .md-uploads{list-style:none;margin:16px 0 0;padding:0;display:grid;gap:6px}
    .md-uploads li{display:flex;flex-wrap:wrap;align-items:center;gap:10px;padding:10px 12px;border:1px solid var(--d-border);border-radius:10px;background:var(--d-surface);font-size:13px}
    .md-uploads li.failed{border-color:var(--d-error-line);background:var(--d-error-soft)}
    .md-uploads bo-icon{color:var(--d-primary)}
    .md-uploads li.failed bo-icon{color:var(--d-error)}
    .md-up-name{flex:1;min-width:120px;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
    .md-up-error{color:var(--d-error-ink);flex-basis:100%;order:3}
    .md-progress{width:140px;height:6px;border-radius:999px;background:var(--d-hover);overflow:hidden}
    .md-progress span{display:block;height:100%;background:var(--d-primary);transition:width .2s}
    .md-up-pct{width:84px;color:var(--d-muted);font-size:12px;text-align:right}
    .md-photos{list-style:none;margin:0;padding:0;display:grid;gap:12px;grid-template-columns:repeat(auto-fill,minmax(180px,1fr))}
    .md-photos li{position:relative;border-radius:14px;overflow:hidden;border:1px solid var(--d-border);background:var(--d-head)}
    .md-photos img{display:block;width:100%;aspect-ratio:4/3;height:auto;object-fit:cover}
    .md-photo-bar{display:flex;align-items:center;justify-content:space-between;gap:8px;padding:6px 8px 6px 10px;font-size:11.5px;color:var(--d-muted);background:var(--d-surface)}
    .md-photo-del{display:grid;place-items:center;width:28px;height:28px;border:0;border-radius:8px;background:none;color:var(--d-error);cursor:pointer}
    .md-photo-del:hover{background:var(--d-error-soft)}
  `]
})
export class MediaPage {
  private readonly ctx = inject(RestaurantContext);
  private readonly api = inject(RestaurantProfileService);
  protected readonly limits = MEDIA_LIMITS;

  protected readonly media = signal<RestaurantMedia[]>([]);
  protected readonly logo = computed(() => this.media().find(m => m.type === 'LOGO') ?? null);
  protected readonly photos = computed(() => this.media().filter(m => m.type === 'PHOTO'));
  protected readonly loading = signal(false);
  protected readonly loadError = signal('');
  protected readonly uploads = signal<UploadItem[]>([]);
  protected readonly busyLogo = computed(() => this.uploads().some(u => u.type === 'LOGO' && !u.error));
  protected readonly pendingDelete = signal<RestaurantMedia | null>(null);
  protected readonly deleting = signal(false);
  protected readonly feedback = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  private nextId = 1;

  constructor() {
    effect(() => {
      const restaurant = this.ctx.selected();
      this.uploads.set([]);
      this.feedback.set(null);
      if (restaurant && this.ctx.canEditProfile()) this.load();
    });
  }

  load(): void {
    const id = this.ctx.selectedId();
    if (!id) return;
    this.loading.set(true);
    this.loadError.set('');
    this.api.listMedia(id).subscribe({
      next: media => { this.media.set(media); this.loading.set(false); },
      error: error => { this.loading.set(false); this.loadError.set(profileErrorMessage(error, 'load')); }
    });
  }

  protected pick(event: Event, type: MediaType): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    this.feedback.set(null);
    const room = this.limits.maxPhotos - this.photos().length;
    const selected = type === 'LOGO' ? files.slice(0, 1) : files.slice(0, Math.max(0, room));
    if (type === 'PHOTO' && files.length > selected.length) {
      this.feedback.set({ type: 'error', text: `Limite de ${this.limits.maxPhotos} fotos: ${files.length - selected.length} arquivo(s) não foram enviados.` });
    }
    for (const file of selected) this.upload(file, type);
  }

  private upload(file: File, type: MediaType): void {
    const restaurantId = this.ctx.selectedId();
    if (!restaurantId) return;
    const item: UploadItem = { id: this.nextId++, name: file.name, type, progress: 0, error: this.precheck(file, type) };
    this.uploads.update(list => [...list, item]);
    if (item.error) return;

    this.api.uploadMedia(restaurantId, type, file).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.patch(item.id, { progress: Math.min(99, Math.round((event.loaded / event.total) * 100)) });
        } else if (event.type === HttpEventType.Response && event.body) {
          const saved = event.body;
          // Uma logo por restaurante: a nova substitui a anterior.
          this.media.update(list => [saved, ...list.filter(m => !(saved.type === 'LOGO' && m.type === 'LOGO'))]);
          this.uploads.update(list => list.filter(u => u.id !== item.id));
          this.feedback.set({ type: 'success', text: saved.type === 'LOGO' ? 'Logo atualizada.' : 'Foto adicionada.' });
        }
      },
      error: error => this.patch(item.id, { error: profileErrorMessage(error, 'media-upload') })
    });
  }

  /** Antecipação dos limites documentados da API (o servidor valida de novo o conteúdo real). */
  private precheck(file: File, type: MediaType): string {
    const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
    if ((file.type && !this.limits.types.includes(file.type)) || !this.limits.extensions.includes(extension)) {
      return 'Formato não aceito. Use JPEG, PNG ou WebP.';
    }
    if (file.size > this.limits[type]) {
      return type === 'LOGO' ? 'A logo pode ter no máximo 5 MB.' : 'Cada foto pode ter no máximo 10 MB.';
    }
    return '';
  }

  protected dismiss(uploadId: number): void {
    this.uploads.update(list => list.filter(u => u.id !== uploadId));
  }

  protected confirmDelete(): void {
    const restaurantId = this.ctx.selectedId();
    const target = this.pendingDelete();
    if (!restaurantId || !target) return;
    this.deleting.set(true);
    this.api.deleteMedia(restaurantId, target.id).subscribe({
      next: () => {
        this.media.update(list => list.filter(m => m.id !== target.id));
        this.deleting.set(false);
        this.pendingDelete.set(null);
        this.feedback.set({ type: 'success', text: target.type === 'LOGO' ? 'Logo removida.' : 'Foto excluída.' });
      },
      error: error => {
        this.deleting.set(false);
        this.pendingDelete.set(null);
        this.feedback.set({ type: 'error', text: profileErrorMessage(error, 'media-delete') });
        if (error?.status === 404) this.media.update(list => list.filter(m => m.id !== target.id));
      }
    });
  }

  protected size(bytes: number): string {
    return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1).replace('.', ',')} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }

  protected date(iso: string): string {
    return new Date(iso).toLocaleDateString('pt-BR');
  }

  private patch(uploadId: number, changes: Partial<UploadItem>): void {
    this.uploads.update(list => list.map(u => u.id === uploadId ? { ...u, ...changes } : u));
  }
}
