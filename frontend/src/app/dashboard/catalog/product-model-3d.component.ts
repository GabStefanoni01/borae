import { HttpEventType } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { IconComponent } from '../../shared/icon.component';
import { Model3dViewerComponent } from '../../shared/model-3d-viewer.component';
import { RestaurantContext } from '../restaurant-context.service';
import { PanelDialogComponent, ToastService } from '../ui/feedback';
import { ConfirmDialogComponent } from '../ui/ui.components';
import { catalogErrorMessage, httpStatus } from './catalog-errors';
import { MODEL_3D_LIMITS, MODEL_3D_STATUS_LABEL, Product, Product3dModel } from './catalog.models';
import { CatalogService } from './catalog.service';

type LoadState = 'idle' | 'loading' | 'none' | 'ready' | 'error';

/**
 * Seção "Experiência 3D" do formulário de produto — endpoints /model-3d da PR #27.
 * Somente OWNER/MANAGER (o pai só exibe a seção para esses papéis; o backend confere).
 * Upload, substituição, publicação e remoção têm efeito imediato e independem do botão
 * "Salvar produto": falhas aqui não bloqueiam a gravação dos demais dados.
 */
@Component({
  selector: 'bo-product-model-3d',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconComponent, ConfirmDialogComponent, PanelDialogComponent, Model3dViewerComponent],
  template: `
    <section class="d-card c-model" aria-labelledby="pm3-title">
      <div class="d-card-head">
        <span class="d-card-icon"><bo-icon name="cube" [size]="18" /></span>
        <h2 id="pm3-title">Experiência 3D</h2>
        @if (model(); as m) {
          <span class="c-model-status" [attr.data-status]="m.status"><span class="c-status-dot" aria-hidden="true"></span>{{ statusLabel[m.status] }}</span>
        }
      </div>
      <p class="d-card-text">Associe um modelo 3D (GLB) ao produto para o cliente girar o prato na tela e, em celulares compatíveis, vê-lo sobre a mesa em realidade aumentada. A foto do produto continua sendo exibida quando o 3D não estiver disponível.</p>

      @if (!product()) {
        <p class="c-empty-line"><bo-icon name="info" [size]="14" /> Salve o produto primeiro para adicionar um modelo 3D.</p>
      } @else if (state() === 'loading') {
        <p class="c-empty-line" role="status"><bo-icon name="loader" [size]="14" [spin]="true" /> Carregando modelo 3D...</p>
      } @else if (state() === 'error') {
        <div class="d-alert d-alert-error" role="alert"><bo-icon name="alert" [size]="16" /><p>{{ loadError() }}</p></div>
        <button type="button" class="d-btn d-btn-secondary d-btn-sm" (click)="load()">Tentar novamente</button>
      } @else {
        @if (model(); as m) {
          <dl class="c-model-info">
            <div><dt>Arquivo</dt><dd>GLB · {{ fileSize(m.fileSize) }}</dd></div>
            <div><dt>Dimensões</dt><dd>{{ m.widthMm }} × {{ m.heightMm }} × {{ m.depthMm }} mm</dd></div>
            <div><dt>Atualizado</dt><dd>{{ date(m.updatedAt) }}</dd></div>
          </dl>
          @if (m.status === 'READY') {
            <p class="c-inline-note"><bo-icon name="eye-off" [size]="14" /> Pronto, mas ainda não aparece no cardápio público. Revise a prévia e publique.</p>
          } @else {
            <p class="c-inline-note"><bo-icon name="globe" [size]="14" /> Publicado: aparece no cardápio público enquanto o produto, o cardápio e o restaurante estiverem publicados.</p>
          }
          <div class="c-model-actions">
            <button type="button" class="d-btn d-btn-secondary d-btn-sm" (click)="previewOpen.set(true)"><bo-icon name="eye" [size]="15" /> Visualizar em 3D</button>
            @if (m.status === 'READY') {
              <button type="button" class="d-btn d-btn-primary d-btn-sm" (click)="setPublished(true)" [disabled]="busy()">
                @if (busy() === 'publish') { <bo-icon name="loader" [size]="14" [spin]="true" /> } Publicar modelo
              </button>
            } @else {
              <button type="button" class="d-btn d-btn-secondary d-btn-sm" (click)="setPublished(false)" [disabled]="busy()">
                @if (busy() === 'publish') { <bo-icon name="loader" [size]="14" [spin]="true" /> } Despublicar modelo
              </button>
            }
            <button type="button" class="d-btn d-btn-ghost d-btn-sm" (click)="removeOpen.set(true)" [disabled]="busy()">Remover modelo</button>
          </div>
        } @else {
          <p class="c-empty-line">Nenhum modelo 3D adicionado.</p>
        }

        <fieldset class="c-model-upload" [disabled]="!!busy()">
          <legend>{{ model() ? 'Substituir modelo' : 'Adicionar modelo' }}</legend>
          <div class="c-model-dims">
            @for (dim of dims; track dim.key) {
              <div class="d-field">
                <label [for]="'pm3-' + dim.key">{{ dim.label }} <span>(mm)</span></label>
                <input [id]="'pm3-' + dim.key" class="d-input d-input-sm" type="number" inputmode="numeric" min="1" step="1" [max]="limits.maxDimensionMm"
                       [name]="dim.key" [ngModel]="size()[dim.key]" (ngModelChange)="setDim(dim.key, $event)" [attr.aria-invalid]="dimInvalid(dim.key) || null">
              </div>
            }
          </div>
          <p class="d-hint"><span>Medidas reais do prato servido, em milímetros (ex.: um prato de 28 cm = 280). Elas ajustam a escala no 3D e na realidade aumentada; o tamanho percebido pode variar por aparelho e ambiente.</span></p>
          <div class="c-image-actions">
            <label class="d-btn d-btn-secondary d-btn-sm c-file" [class.disabled]="!!busy()">
              <bo-icon name="cube" [size]="15" /> {{ file() ? 'Trocar arquivo' : 'Escolher arquivo GLB' }}
              <input type="file" accept=".glb,model/gltf-binary" (change)="pick($event)" [disabled]="!!busy()">
            </label>
            @if (file(); as f) { <span class="c-model-file">{{ f.name }} · {{ fileSize(f.size) }}</span> }
          </div>
          @if (uploadProgress() !== null) {
            <div class="c-upload c-upload-inline" role="status">
              <span>{{ uploadProgress()! < 100 ? 'Enviando modelo 3D... ' + uploadProgress() + '%' : 'Validando o modelo no servidor...' }}</span>
              <span class="c-upload-bar"><span [style.width.%]="uploadProgress()"></span></span>
            </div>
          }
          @if (formError()) { <p class="c-field-error" role="alert"><bo-icon name="alert" [size]="14" /> {{ formError() }}</p> }
          <button type="button" class="d-btn d-btn-primary d-btn-sm" (click)="submit()" [disabled]="!!busy()">
            @if (busy() === 'upload') { <bo-icon name="loader" [size]="14" [spin]="true" /> }
            {{ model() ? 'Substituir modelo 3D' : 'Enviar modelo 3D' }}
          </button>
          <p class="d-hint"><span>Formato GLB 2.0 com texturas embutidas, até 10 MB. Para alterar só as dimensões, envie o arquivo novamente com as novas medidas.</span></p>
        </fieldset>
      }
    </section>

    <bo-panel-dialog [open]="previewOpen()" title="Prévia em 3D" subtitle="Como o cliente verá o modelo no cardápio digital." (closed)="previewOpen.set(false)">
      @if (previewOpen() && model(); as m) {
        <bo-model-3d-viewer [src]="m.modelUrl" [alt]="'Modelo 3D de ' + (product()?.name ?? 'produto')" [poster]="product()?.imageUrl ?? null"
          [widthMm]="m.widthMm" [heightMm]="m.heightMm" [depthMm]="m.depthMm" [ar]="true" />
      }
    </bo-panel-dialog>

    <bo-confirm-dialog [open]="replaceOpen()" title="Substituir o modelo 3D?" [message]="model()?.status === 'PUBLISHED'
        ? 'O novo arquivo substitui o atual e passa a aparecer no cardápio público assim que for aceito.'
        : 'O novo arquivo substitui o modelo atual.'"
      confirmLabel="Substituir" (confirmed)="replaceOpen.set(false); upload()" (cancelled)="replaceOpen.set(false)" />

    <bo-confirm-dialog [open]="removeOpen()" title="Remover o modelo 3D?" message="O modelo deixa de aparecer no cardápio e o arquivo é apagado. A foto do produto continua igual."
      confirmLabel="Remover" [danger]="true" [busy]="busy() === 'delete'" (confirmed)="remove()" (cancelled)="removeOpen.set(false)" />
  `
})
export class ProductModel3dComponent {
  readonly menuId = input.required<string>();
  /** Produto salvo (null em produto novo: os endpoints exigem productId). */
  readonly product = input<Product | null>(null);

  private readonly ctx = inject(RestaurantContext);
  private readonly catalog = inject(CatalogService);
  private readonly toast = inject(ToastService);

  protected get limits() { return MODEL_3D_LIMITS; }
  protected get statusLabel() { return MODEL_3D_STATUS_LABEL; }
  protected readonly dims = [
    { key: 'widthMm', label: 'Largura' }, { key: 'heightMm', label: 'Altura' }, { key: 'depthMm', label: 'Profundidade' }
  ] as const;

  protected readonly state = signal<LoadState>('idle');
  protected readonly model = signal<Product3dModel | null>(null);
  protected readonly loadError = signal('');
  protected readonly size = signal<Record<'widthMm' | 'heightMm' | 'depthMm', number | null>>({ widthMm: null, heightMm: null, depthMm: null });
  protected readonly file = signal<File | null>(null);
  protected readonly submitted = signal(false);
  protected readonly formError = signal('');
  protected readonly busy = signal<'upload' | 'publish' | 'delete' | null>(null);
  protected readonly uploadProgress = signal<number | null>(null);
  protected readonly previewOpen = signal(false);
  protected readonly replaceOpen = signal(false);
  protected readonly removeOpen = signal(false);
  private request?: Subscription;

  private readonly productId = computed(() => this.product()?.id ?? null);

  constructor() {
    // Trocar de restaurante ou de produto descarta tudo o que é local, para não misturar contextos.
    effect(() => {
      const restaurantId = this.ctx.selectedId();
      const productId = this.productId();
      this.menuId();
      untracked(() => {
        this.reset();
        if (restaurantId && productId) this.load();
      });
    });
    inject(DestroyRef).onDestroy(() => this.request?.unsubscribe());
  }

  load(): void {
    const restaurantId = this.ctx.selectedId();
    const productId = this.productId();
    if (!restaurantId || !productId) return;
    this.state.set('loading');
    this.request?.unsubscribe();
    this.request = this.catalog.getModel3d(restaurantId, this.menuId(), productId).subscribe({
      next: model => this.applyModel(model),
      error: error => {
        if (httpStatus(error) === 404) { this.model.set(null); this.state.set('none'); return; }
        this.loadError.set(catalogErrorMessage(error, 'load-model'));
        this.state.set('error');
      }
    });
  }

  protected setDim(key: 'widthMm' | 'heightMm' | 'depthMm', value: number | string | null): void {
    const parsed = value === null || value === '' ? null : Number(value);
    this.size.update(s => ({ ...s, [key]: parsed }));
  }

  protected dimInvalid(key: 'widthMm' | 'heightMm' | 'depthMm'): boolean {
    return this.submitted() && !validDimension(this.size()[key]);
  }

  protected pick(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    const file = inputEl.files?.[0];
    inputEl.value = '';
    if (!file) return;
    this.formError.set('');
    const problem = validateGlbFile(file);
    if (problem) { this.file.set(null); this.formError.set(problem); return; }
    readGlbHeader(file).then(headerProblem => {
      if (headerProblem) { this.file.set(null); this.formError.set(headerProblem); return; }
      this.file.set(file);
    });
  }

  protected submit(): void {
    this.submitted.set(true);
    this.formError.set('');
    if (!this.file()) { this.formError.set('Escolha um arquivo GLB.'); return; }
    const { widthMm, heightMm, depthMm } = this.size();
    if (![widthMm, heightMm, depthMm].every(validDimension)) {
      this.formError.set(`Informe largura, altura e profundidade em milímetros: números inteiros entre 1 e ${MODEL_3D_LIMITS.maxDimensionMm}.`);
      return;
    }
    if (this.model()) { this.replaceOpen.set(true); return; }
    this.upload();
  }

  protected upload(): void {
    const restaurantId = this.ctx.selectedId();
    const productId = this.productId();
    const file = this.file();
    const { widthMm, heightMm, depthMm } = this.size();
    if (!restaurantId || !productId || !file || !widthMm || !heightMm || !depthMm) return;
    const replacing = !!this.model();
    this.busy.set('upload');
    this.uploadProgress.set(0);
    this.request = this.catalog.uploadModel3d(restaurantId, this.menuId(), productId, file, { widthMm, heightMm, depthMm }).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress) {
          this.uploadProgress.set(event.total ? Math.round((event.loaded / event.total) * 100) : 0);
        } else if (event.type === HttpEventType.Response && event.body) {
          this.busy.set(null);
          this.uploadProgress.set(null);
          this.file.set(null);
          this.submitted.set(false);
          this.applyModel(event.body);
          this.toast.success(replacing ? 'Modelo 3D substituído com sucesso.' : 'Modelo 3D enviado. Revise a prévia e publique quando estiver pronto.');
        }
      },
      error: error => {
        this.busy.set(null);
        this.uploadProgress.set(null);
        this.formError.set(catalogErrorMessage(error, 'upload-model'));
      }
    });
  }

  protected setPublished(published: boolean): void {
    const restaurantId = this.ctx.selectedId();
    const productId = this.productId();
    if (!restaurantId || !productId || this.busy()) return;
    this.busy.set('publish');
    this.catalog.setModel3dPublication(restaurantId, this.menuId(), productId, published).subscribe({
      next: model => {
        this.busy.set(null);
        this.applyModel(model);
        this.toast.success(published ? 'Modelo 3D publicado.' : 'Modelo 3D despublicado. Ele não aparece mais no cardápio público.');
      },
      error: error => {
        this.busy.set(null);
        if (httpStatus(error) === 404) { this.model.set(null); this.state.set('none'); }
        this.toast.error(catalogErrorMessage(error, 'publish-model'));
      }
    });
  }

  protected remove(): void {
    const restaurantId = this.ctx.selectedId();
    const productId = this.productId();
    if (!restaurantId || !productId) return;
    this.busy.set('delete');
    this.catalog.deleteModel3d(restaurantId, this.menuId(), productId).subscribe({
      next: () => {
        this.busy.set(null);
        this.removeOpen.set(false);
        this.model.set(null);
        this.state.set('none');
        this.toast.success('Modelo 3D removido.');
      },
      error: error => {
        this.busy.set(null);
        this.removeOpen.set(false);
        if (httpStatus(error) === 404) { this.model.set(null); this.state.set('none'); }
        this.toast.error(catalogErrorMessage(error, 'delete-model'));
      }
    });
  }

  protected fileSize(bytes: number): string {
    return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`;
  }

  protected date(iso: string): string {
    const d = new Date(iso);
    return Number.isNaN(d.getTime()) ? '—' : d.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
  }

  private applyModel(model: Product3dModel): void {
    this.model.set(model);
    this.size.set({ widthMm: model.widthMm, heightMm: model.heightMm, depthMm: model.depthMm });
    this.state.set('ready');
  }

  private reset(): void {
    this.request?.unsubscribe();
    this.state.set('idle');
    this.model.set(null);
    this.file.set(null);
    this.size.set({ widthMm: null, heightMm: null, depthMm: null });
    this.submitted.set(false);
    this.formError.set('');
    this.busy.set(null);
    this.uploadProgress.set(null);
    this.previewOpen.set(false);
  }
}

function validDimension(value: number | null): boolean {
  return value !== null && Number.isInteger(value) && value >= 1 && value <= MODEL_3D_LIMITS.maxDimensionMm;
}

/** Antecipa as regras do backend: extensão .glb e até 10 MB. */
export function validateGlbFile(file: File): string | null {
  const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
  if (extension !== MODEL_3D_LIMITS.extension) return 'Formato não aceito. Envie um arquivo .glb (glTF binário).';
  if (file.size === 0) return 'O arquivo selecionado está vazio.';
  if (file.size > MODEL_3D_LIMITS.bytes) return 'O modelo 3D pode ter no máximo 10 MB.';
  return null;
}

/** Confere o cabeçalho GLB (magic "glTF" e versão 2). A validação completa é do backend. */
export async function readGlbHeader(file: Blob): Promise<string | null> {
  try {
    const buffer = await readBytes(file.slice(0, 12));
    if (buffer.byteLength < 12) return 'O arquivo não é um GLB válido.';
    const view = new DataView(buffer);
    if (view.getUint32(0, true) !== 0x46546c67) return 'O arquivo não é um GLB válido (cabeçalho glTF ausente).';
    if (view.getUint32(4, true) !== 2) return 'Use um modelo glTF 2.0 em formato GLB.';
    return null;
  } catch {
    return null; // Se o navegador não conseguir ler, o servidor valida.
  }
}

function readBytes(blob: Blob): Promise<ArrayBuffer> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as ArrayBuffer);
    reader.onerror = () => reject(reader.error);
    reader.readAsArrayBuffer(blob);
  });
}
