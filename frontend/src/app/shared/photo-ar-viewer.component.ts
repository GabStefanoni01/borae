import {
  ChangeDetectionStrategy, Component, OnDestroy, input, signal
} from '@angular/core';
import { IconComponent } from './icon.component';
import { Model3dViewerComponent } from './model-3d-viewer.component';
import * as THREE from 'three';
import { GLTFExporter } from 'three/examples/jsm/exporters/GLTFExporter.js';

type PhotoArState = 'idle' | 'generating' | 'ready' | 'error';

/**
 * Converte a fotografia original em uma placa horizontal GLB no próprio navegador.
 * Não cria geometria do alimento nem chama serviço remoto: é somente uma textura 2D.
 */
@Component({
  selector: 'bo-photo-ar-viewer',
  standalone: true,
  imports: [IconComponent, Model3dViewerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="par" aria-live="polite">
      @if (state() === 'idle' || state() === 'generating') {
        <div class="par-preview">
          <img [src]="imageUrl()" [alt]="alt()" crossorigin="anonymous">
          @if (state() === 'generating') {
            <div class="par-progress"><bo-icon name="loader" [size]="18" [spin]="true" /> Preparando realidade aumentada…</div>
          }
        </div>
        @if (state() === 'idle') {
          <button type="button" class="par-start" (click)="generate()">
            <bo-icon name="scan" [size]="17" /> Posicionar na mesa
          </button>
        }
      } @else if (state() === 'ready' && modelUrl()) {
        <bo-model-3d-viewer [src]="modelUrl()!" [alt]="alt()" [poster]="imageUrl()" [ar]="true" [arModes]="'webxr quick-look'" />
        <p class="par-note">A fotografia é apresentada como uma superfície plana. A escala e a detecção da mesa dependem do aparelho.</p>
      } @else {
        <div class="par-error" role="alert">
          <p>Não foi possível preparar a experiência AR neste aparelho.</p>
          <button type="button" class="par-start" (click)="reset()">Tentar novamente</button>
        </div>
      }
    </section>
  `,
  styles: [`
    :host{display:block}
    .par{display:grid;gap:12px}
    .par-preview{position:relative;aspect-ratio:4/3;overflow:hidden;border-radius:14px;background:#f5f0ea}
    .par-preview img{width:100%;height:100%;object-fit:contain}
    .par-progress{position:absolute;inset:auto 10px 10px;display:flex;align-items:center;gap:8px;padding:10px 12px;border-radius:10px;background:rgba(255,255,255,.94);font-size:13px}
    .par-start{justify-self:center;display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:44px;padding:0 18px;border:0;border-radius:999px;background:#191919;color:#fff;font:inherit;font-weight:700}
    .par-note{font-size:12px;color:#666;line-height:1.5}
    .par-error{display:grid;gap:12px;justify-items:center;padding:20px;text-align:center}
  `]
})
export class PhotoArViewerComponent implements OnDestroy {
  readonly imageUrl = input.required<string>();
  readonly alt = input('Fotografia do prato');
  protected readonly state = signal<PhotoArState>('idle');
  protected readonly modelUrl = signal<string | null>(null);
  private objectUrl: string | null = null;
  private generation = 0;

  generate(): void {
    if (this.state() === 'generating') return;
    const run = ++this.generation;
    this.state.set('generating');
    this.loadImage(this.imageUrl()).then(image => this.createFlatGlb(image)).then(blob => {
      if (run !== this.generation) return;
      this.revokeObjectUrl();
      this.objectUrl = URL.createObjectURL(blob);
      this.modelUrl.set(this.objectUrl);
      this.state.set('ready');
    }).catch(() => {
      if (run === this.generation) this.state.set('error');
    });
  }

  reset(): void {
    this.generation++;
    this.revokeObjectUrl();
    this.modelUrl.set(null);
    this.state.set('idle');
  }

  ngOnDestroy(): void {
    this.generation++;
    this.revokeObjectUrl();
  }

  private loadImage(url: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const image = new Image();
      image.crossOrigin = 'anonymous';
      image.onload = () => image.naturalWidth && image.naturalHeight ? resolve(image) : reject(new Error('Imagem vazia'));
      image.onerror = () => reject(new Error('Imagem indisponível ou sem CORS'));
      image.src = url;
    });
  }

  private async createFlatGlb(image: HTMLImageElement): Promise<Blob> {
    const canvas = document.createElement('canvas');
    const maxDimension = 2048;
    const scale = Math.min(1, maxDimension / Math.max(image.naturalWidth, image.naturalHeight));
    canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
    canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
    const context = canvas.getContext('2d');
    if (!context) throw new Error('Canvas indisponível');
    context.drawImage(image, 0, 0, canvas.width, canvas.height);

    // A maior dimensão física fica em 30 cm; a proporção da fotografia é preservada.
    const longestSide = 0.3;
    const aspect = canvas.width / canvas.height;
    const width = aspect >= 1 ? longestSide : longestSide * aspect;
    const height = aspect >= 1 ? longestSide / aspect : longestSide;
    const texture = new THREE.CanvasTexture(canvas);
    texture.colorSpace = THREE.SRGBColorSpace;
    texture.needsUpdate = true;

    const geometry = new THREE.PlaneGeometry(width, height);
    const material = new THREE.MeshBasicMaterial({ map: texture, side: THREE.DoubleSide });
    const plane = new THREE.Mesh(geometry, material);
    plane.rotation.x = -Math.PI / 2;
    const scene = new THREE.Scene();
    scene.add(plane);

    try {
      const exporter = new GLTFExporter();
      const result = await exporter.parseAsync(scene, { binary: true, onlyVisible: true });
      if (!(result instanceof ArrayBuffer)) throw new Error('Exportação GLB inválida');
      return new Blob([result], { type: 'model/gltf-binary' });
    } finally {
      geometry.dispose();
      material.dispose();
      texture.dispose();
    }
  }

  private revokeObjectUrl(): void {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
    this.objectUrl = null;
  }
}
