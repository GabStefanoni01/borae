import {
  ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA, InjectionToken, afterNextRender, computed, inject, input, output, signal
} from '@angular/core';
import { IconComponent } from './icon.component';

/**
 * Carrega o web component <model-viewer> (Google) sob demanda. Fica num token para que os testes
 * não baixem a biblioteca (three.js) e para que nada disso entre no carregamento inicial do app.
 */
export const MODEL_VIEWER_LOADER = new InjectionToken<() => Promise<unknown>>('MODEL_VIEWER_LOADER', {
  providedIn: 'root',
  factory: () => () => import('@google/model-viewer')
});

/** Subconjunto da API do elemento <model-viewer> usado aqui. */
interface ModelViewerElement extends HTMLElement {
  getDimensions?(): { x: number; y: number; z: number };
  scale: string;
}

type ViewerState = 'loading' | 'ready' | 'error';

/**
 * Visualizador 3D (GLB) com rotação, zoom e AR progressiva.
 * - A biblioteca é baixada somente quando o componente é exibido.
 * - AR: WebXR / Scene Viewer (Android) e Quick Look (iOS, USDZ gerado a partir do GLB).
 *   O botão só aparece quando o navegador informa suporte (slot "ar-button" do model-viewer).
 * - Escala: quando o restaurante informa as dimensões físicas, o modelo é ajustado para elas
 *   (mediana das três proporções). É uma aproximação — a percepção varia por dispositivo.
 * - Falha: mostra a imagem convencional do produto (poster) e emite `failed`.
 */
@Component({
  selector: 'bo-model-3d-viewer',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  imports: [IconComponent],
  template: `
    <div class="mv" [class.mv-error]="state() === 'error'">
      @if (state() === 'error') {
        <div class="mv-fallback" role="status">
          @if (poster()) { <img [src]="poster()" [alt]="alt()" loading="lazy"> }
          <p><bo-icon name="alert" [size]="16" /> Não foi possível carregar o modelo 3D. {{ poster() ? 'Exibindo a foto do produto.' : '' }}</p>
        </div>
      } @else {
        @if (libReady()) {
          <model-viewer class="mv-el" [attr.src]="src()" [attr.alt]="alt()" [attr.poster]="poster() || null"
            loading="eager" camera-controls touch-action="pan-y" shadow-intensity="1" exposure="1" interaction-prompt="auto"
            [attr.auto-rotate]="reduceMotion ? null : ''" [attr.ar]="ar() ? '' : null"
            [attr.ar-modes]="arModes()" ar-placement="floor" [attr.ar-scale]="hasDimensions() ? 'fixed' : 'auto'"
            (load)="onLoad($event)" (error)="onError()" (progress)="onProgress($event)">
            @if (ar()) {
              <button slot="ar-button" type="button" class="mv-ar"><bo-icon name="cube" [size]="16" /> Ver no seu ambiente</button>
            }
          </model-viewer>
        }
        @if (state() === 'loading') {
          <div class="mv-loading" role="status" aria-live="polite">
            <bo-icon name="loader" [size]="20" [spin]="true" />
            <span>Carregando modelo 3D{{ progress() ? ' · ' + progress() + '%' : '...' }}</span>
          </div>
        }
      }
    </div>
    @if (state() === 'ready') {
      <p class="mv-help">Arraste para girar e use dois dedos ou a roda do mouse para aproximar.{{ hasDimensions() ? ' Tamanho aproximado: ' + sizeLabel() + '.' : '' }}</p>
    }
  `,
  styles: [`
    :host{display:block}
    .mv{position:relative;aspect-ratio:1/1;max-height:70vh;width:100%;border-radius:14px;overflow:hidden;background:radial-gradient(circle at 50% 40%,#FFFFFF,#F3EEE8)}
    .mv-el{width:100%;height:100%;--poster-color:transparent;--progress-bar-color:#FF7200}
    .mv-loading{position:absolute;inset:auto 12px 12px 12px;display:flex;align-items:center;gap:8px;padding:8px 12px;border-radius:10px;background:rgba(255,255,255,.92);color:#3D3D3D;font-size:13px;box-shadow:0 2px 10px rgba(0,0,0,.08)}
    .mv-ar{position:absolute;left:50%;bottom:14px;transform:translateX(-50%);display:inline-flex;align-items:center;gap:8px;min-height:44px;padding:0 18px;border:0;border-radius:999px;background:#191919;color:#fff;font:inherit;font-weight:700;cursor:pointer;box-shadow:0 6px 18px rgba(0,0,0,.2)}
    .mv-ar:focus-visible{outline:2px solid #FF7200;outline-offset:2px}
    .mv-fallback{display:grid;height:100%;place-items:center;align-content:center;gap:10px;padding:12px;text-align:center}
    .mv-fallback img{max-width:100%;max-height:75%;border-radius:12px;object-fit:cover}
    .mv-fallback p{display:flex;align-items:center;gap:6px;margin:0;font-size:13px;color:#5E5E5E}
    .mv-help{margin:8px 0 0;font-size:12px;color:var(--d-muted,#707070)}
  `]
})
export class Model3dViewerComponent {
  readonly src = input.required<string>();
  readonly alt = input('Modelo 3D do produto');
  /** Imagem convencional exibida antes do carregamento e como fallback. */
  readonly poster = input<string | null>(null);
  readonly ar = input(false);\n  /** Permite excluir Scene Viewer quando o GLB é um Blob local, inacessível fora do navegador. */\n  readonly arModes = input('webxr scene-viewer quick-look');
  readonly widthMm = input<number | null>(null);
  readonly heightMm = input<number | null>(null);
  readonly depthMm = input<number | null>(null);
  readonly failed = output<void>();

  private readonly loader = inject(MODEL_VIEWER_LOADER);
  protected readonly libReady = signal(false);
  protected readonly state = signal<ViewerState>('loading');
  protected readonly progress = signal(0);
  protected readonly reduceMotion = typeof window !== 'undefined' && !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

  protected readonly hasDimensions = computed(() => [this.widthMm(), this.heightMm(), this.depthMm()].every(v => !!v && v > 0));
  protected readonly sizeLabel = computed(() => `${formatMm(this.widthMm())} × ${formatMm(this.heightMm())} × ${formatMm(this.depthMm())} (L × A × P)`);

  constructor() {
    afterNextRender(() => {
      this.loader().then(
        () => this.libReady.set(true),
        () => this.onError()
      );
    });
  }

  protected onLoad(event: Event): void {
    const el = event.target as ModelViewerElement;
    const factor = calibrationFactor(el.getDimensions?.(), this.widthMm(), this.heightMm(), this.depthMm());
    if (factor) el.scale = `${factor} ${factor} ${factor}`;
    this.state.set('ready');
  }

  protected onProgress(event: Event): void {
    const total = (event as CustomEvent<{ totalProgress?: number }>).detail?.totalProgress ?? 0;
    this.progress.set(Math.round(total * 100));
  }

  protected onError(): void {
    this.state.set('error');
    this.failed.emit();
  }
}

/**
 * Fator de escala uniforme para aproximar o modelo das dimensões físicas (mm → m).
 * glTF usa metros e eixo Y para cima: x = largura, y = altura, z = profundidade.
 * Usa a mediana das proporções para tolerar um eixo discrepante; null quando não há dados confiáveis.
 */
export function calibrationFactor(
  size: { x: number; y: number; z: number } | undefined,
  widthMm: number | null, heightMm: number | null, depthMm: number | null
): number | null {
  if (!size || !widthMm || !heightMm || !depthMm) return null;
  const ratios = [widthMm / 1000 / size.x, heightMm / 1000 / size.y, depthMm / 1000 / size.z]
    .filter(r => Number.isFinite(r) && r > 0)
    .sort((a, b) => a - b);
  if (ratios.length !== 3) return null;
  const median = ratios[1];
  return Math.abs(median - 1) < 0.01 ? null : Number(median.toFixed(4));
}

function formatMm(value: number | null): string {
  if (!value) return '—';
  return value >= 10 ? `${(value / 10).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} cm` : `${value} mm`;
}
