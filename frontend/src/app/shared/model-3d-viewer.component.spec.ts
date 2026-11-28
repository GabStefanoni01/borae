import { TestBed } from '@angular/core/testing';
import { MODEL_VIEWER_LOADER, Model3dViewerComponent, calibrationFactor } from './model-3d-viewer.component';

async function setup(loader: () => Promise<unknown> = () => Promise.resolve(), inputs: Record<string, unknown> = {}) {
  TestBed.configureTestingModule({ imports: [Model3dViewerComponent], providers: [{ provide: MODEL_VIEWER_LOADER, useValue: vi.fn(loader) }] });
  const fixture = TestBed.createComponent(Model3dViewerComponent);
  fixture.componentRef.setInput('src', 'https://ik.imagekit.io/borae/prato.glb');
  for (const [key, value] of Object.entries(inputs)) fixture.componentRef.setInput(key, value);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement, loader: TestBed.inject(MODEL_VIEWER_LOADER) };
}

describe('Model3dViewerComponent', () => {
  it('carrega a biblioteca sob demanda e renderiza o <model-viewer> com o GLB', async () => {
    const { el, loader } = await setup();
    expect(loader).toHaveBeenCalledTimes(1);
    const mv = el.querySelector('model-viewer')!;
    expect(mv.getAttribute('src')).toBe('https://ik.imagekit.io/borae/prato.glb');
    expect(mv.hasAttribute('camera-controls')).toBe(true);
    expect(mv.hasAttribute('ar')).toBe(false);
    expect(el.querySelector('.mv-loading')?.textContent).toContain('Carregando modelo 3D');
  });

  it('AR é opcional: botão no slot próprio do model-viewer (exibido só quando há suporte)', async () => {
    const { el } = await setup(undefined, { ar: true, widthMm: 280, heightMm: 70, depthMm: 280 });
    const mv = el.querySelector('model-viewer')!;
    expect(mv.hasAttribute('ar')).toBe(true);
    expect(mv.getAttribute('ar-modes')).toBe('webxr scene-viewer quick-look');
    expect(mv.getAttribute('ar-scale')).toBe('fixed');
    expect(mv.querySelector('[slot="ar-button"]')?.textContent).toContain('Ver no seu ambiente');
  });

  it('informa quando o aparelho não oferece suporte a AR e mantém a visualização', async () => {
    const { el, fixture } = await setup(undefined, { ar: true, arModes: 'webxr quick-look' });
    const mv = el.querySelector('model-viewer') as HTMLElement & { canActivateAR: boolean };
    mv.canActivateAR = false;
    mv.dispatchEvent(new Event('load'));
    fixture.detectChanges();

    expect(el.querySelector('model-viewer')).toBeTruthy();
    expect(el.textContent).toContain('A realidade aumentada não está disponível neste navegador ou aparelho');
  });

  it('ao carregar, ajusta a escala para as dimensões físicas e mostra o tamanho', async () => {
    const { el, fixture } = await setup(undefined, { widthMm: 300, heightMm: 70, depthMm: 300 });
    const mv = el.querySelector('model-viewer') as HTMLElement & { getDimensions: () => object; scale: string };
    mv.getDimensions = () => ({ x: 0.15, y: 0.035, z: 0.15 });
    mv.dispatchEvent(new Event('load'));
    fixture.detectChanges();
    expect(mv.scale).toBe('2 2 2');
    expect(el.querySelector('.mv-help')?.textContent).toContain('30 cm × 7 cm × 30 cm');
    expect(el.querySelector('.mv-loading')).toBeNull();
  });

  it('erro no modelo mostra a foto do produto e avisa o componente pai', async () => {
    const { el, fixture } = await setup(undefined, { poster: 'https://ik.imagekit.io/borae/foto.webp' });
    let failed = false;
    fixture.componentInstance.failed.subscribe(() => (failed = true));
    el.querySelector('model-viewer')!.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(failed).toBe(true);
    expect(el.querySelector('model-viewer')).toBeNull();
    expect(el.querySelector<HTMLImageElement>('.mv-fallback img')!.src).toContain('foto.webp');
    expect(el.textContent).toContain('Exibindo a foto do produto');
  });

  it('falha ao baixar a biblioteca também cai para a foto', async () => {
    const { el } = await setup(() => Promise.reject(new Error('offline')));
    expect(el.querySelector('.mv-fallback')).toBeTruthy();
  });
});

describe('calibrationFactor', () => {
  it('usa a mediana das proporções (mm → m) e ignora ajustes desprezíveis', () => {
    expect(calibrationFactor({ x: 0.14, y: 0.034, z: 0.14 }, 280, 68, 280)).toBe(2);
    expect(calibrationFactor({ x: 0.28, y: 0.068, z: 0.28 }, 280, 68, 280)).toBeNull();
    expect(calibrationFactor({ x: 0.1, y: 1, z: 0.1 }, 200, 100, 210)).toBe(2);
  });

  it('sem dados confiáveis não altera a escala', () => {
    expect(calibrationFactor(undefined, 280, 68, 280)).toBeNull();
    expect(calibrationFactor({ x: 0, y: 0, z: 0 }, 280, 68, 280)).toBeNull();
    expect(calibrationFactor({ x: 0.2, y: 0.05, z: 0.2 }, null, 68, 280)).toBeNull();
  });
});
