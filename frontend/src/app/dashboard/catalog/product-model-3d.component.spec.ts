import { HttpErrorResponse, HttpEventType, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { buttonByText, fakeContext, product, text } from '../../../testing/catalog-fixtures';
import { MODEL_VIEWER_LOADER } from '../../shared/model-3d-viewer.component';
import { RestaurantContext } from '../restaurant-context.service';
import { ToastService } from '../ui/feedback';
import { Product, Product3dModel } from './catalog.models';
import { CatalogService } from './catalog.service';
import { ProductModel3dComponent, readGlbHeader, validateGlbFile } from './product-model-3d.component';

const MODEL: Product3dModel = {
  id: 'md1', productId: 'p1', status: 'READY', modelUrl: 'https://ik.imagekit.io/borae/m.glb', contentType: 'model/gltf-binary',
  fileSize: 2 * 1024 * 1024, widthMm: 280, heightMm: 70, depthMm: 280, createdAt: '2026-11-17T12:00:00Z', updatedAt: '2026-11-17T12:00:00Z'
};

/** Cabeçalho GLB válido (magic "glTF", versão 2) seguido de bytes quaisquer. */
function glbFile(name = 'prato.glb', size = 64): File {
  const buffer = new ArrayBuffer(size);
  const view = new DataView(buffer);
  view.setUint32(0, 0x46546c67, true);
  view.setUint32(4, 2, true);
  view.setUint32(8, size, true);
  return new File([buffer], name, { type: 'model/gltf-binary' });
}

function setup(get$: Observable<Product3dModel> = of(MODEL), p: Product | null = product()) {
  const ctx = fakeContext('OWNER');
  const catalog = {
    getModel3d: vi.fn(() => get$),
    uploadModel3d: vi.fn(() => of(new HttpResponse({ body: { ...MODEL, widthMm: 300, updatedAt: '2026-11-17T13:00:00Z' } }))),
    setModel3dPublication: vi.fn((_r: string, _m: string, _p: string, published: boolean) => of({ ...MODEL, status: published ? 'PUBLISHED' as const : 'READY' as const })),
    deleteModel3d: vi.fn(() => of(undefined))
  };
  TestBed.configureTestingModule({
    imports: [ProductModel3dComponent],
    providers: [
      ToastService,
      { provide: RestaurantContext, useValue: ctx },
      { provide: CatalogService, useValue: catalog },
      { provide: MODEL_VIEWER_LOADER, useValue: () => Promise.resolve() }
    ]
  });
  const fixture = TestBed.createComponent(ProductModel3dComponent);
  fixture.componentRef.setInput('menuId', 'm1');
  fixture.componentRef.setInput('product', p);
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;
  const pick = async (file: File) => {
    const input = el.querySelector<HTMLInputElement>('.c-file input')!;
    Object.defineProperty(input, 'files', { value: [file], configurable: true });
    input.dispatchEvent(new Event('change'));
    await new Promise(r => setTimeout(r, 60));
    fixture.detectChanges();
  };
  const setDims = async (w: string, h: string, d: string) => {
    await fixture.whenStable();
    for (const [id, value] of [['widthMm', w], ['heightMm', h], ['depthMm', d]]) {
      const input = el.querySelector<HTMLInputElement>(`#pm3-${id}`)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    fixture.detectChanges();
  };
  const openDialog = () => [...el.querySelectorAll('dialog')].find(d => d.hasAttribute('open') && !d.classList.contains('d-panel-dialog'));
  return { fixture, el, catalog, ctx, pick, setDims, openDialog, toast: TestBed.inject(ToastService) };
}

describe('ProductModel3dComponent', () => {
  it('produto novo: orienta a salvar antes e não consulta a API', () => {
    const { el, catalog } = setup(of(MODEL), null);
    expect(text(el)).toContain('Salve o produto primeiro para adicionar um modelo 3D.');
    expect(catalog.getModel3d).not.toHaveBeenCalled();
  });

  it('404 significa produto sem modelo (não é erro)', () => {
    const { el } = setup(throwError(() => new HttpErrorResponse({ status: 404 })));
    expect(text(el)).toContain('Nenhum modelo 3D adicionado.');
    expect(el.querySelector('.d-alert-error')).toBeNull();
    expect(buttonByText(el, 'Enviar modelo 3D')).toBeTruthy();
  });

  it('erro de carregamento mostra mensagem e permite tentar novamente', () => {
    const { el, catalog, fixture } = setup(throwError(() => new HttpErrorResponse({ status: 500 })));
    expect(el.querySelector('.d-alert-error')).toBeTruthy();
    catalog.getModel3d.mockReturnValue(of(MODEL));
    buttonByText(el, 'Tentar novamente')!.click();
    fixture.detectChanges();
    expect(text(el.querySelector('.c-model-status'))).toBe('Pronto, não publicado');
  });

  it('mostra modelo pronto com tamanho, dimensões e ações', () => {
    const { el, catalog } = setup();
    expect(catalog.getModel3d).toHaveBeenCalledWith('r1', 'm1', 'p1');
    expect(text(el.querySelector('.c-model-info'))).toContain('GLB · 2 MB');
    expect(text(el.querySelector('.c-model-info'))).toContain('280 × 70 × 280 mm');
    expect(text(el)).toContain('ainda não aparece no cardápio público');
    expect(buttonByText(el, 'Publicar modelo')).toBeTruthy();
  });

  it('publica e despublica pelo endpoint de publicação', () => {
    const { el, fixture, catalog, toast } = setup();
    buttonByText(el, 'Publicar modelo')!.click();
    fixture.detectChanges();
    expect(catalog.setModel3dPublication).toHaveBeenCalledWith('r1', 'm1', 'p1', true);
    expect(text(el.querySelector('.c-model-status'))).toBe('Publicado');
    buttonByText(el, 'Despublicar modelo')!.click();
    fixture.detectChanges();
    expect(catalog.setModel3dPublication).toHaveBeenLastCalledWith('r1', 'm1', 'p1', false);
    expect(toast.toasts().at(-1)?.text).toContain('despublicado');
  });

  it('envia novo modelo com as dimensões em milímetros', async () => {
    const { el, fixture, catalog, pick, setDims, toast } = setup(throwError(() => new HttpErrorResponse({ status: 404 })));
    await setDims('280', '70', '280');
    const file = glbFile();
    await pick(file);
    expect(text(el.querySelector('.c-model-file'))).toContain('prato.glb');
    buttonByText(el, 'Enviar modelo 3D')!.click();
    fixture.detectChanges();
    expect(catalog.uploadModel3d).toHaveBeenCalledWith('r1', 'm1', 'p1', file, { widthMm: 280, heightMm: 70, depthMm: 280 });
    expect(text(el.querySelector('.c-model-info'))).toContain('300 × 70 × 280 mm');
    expect(toast.toasts().at(-1)?.text).toContain('Modelo 3D enviado');
  });

  it('substituição pede confirmação antes de enviar', async () => {
    const { el, fixture, catalog, pick, openDialog } = setup();
    await fixture.whenStable();
    await pick(glbFile('novo.glb'));
    buttonByText(el, 'Substituir modelo 3D')!.click();
    fixture.detectChanges();
    expect(catalog.uploadModel3d).not.toHaveBeenCalled();
    expect(text(openDialog())).toContain('Substituir o modelo 3D?');
    buttonByText(openDialog()!, 'Substituir')!.click();
    fixture.detectChanges();
    expect(catalog.uploadModel3d).toHaveBeenCalledTimes(1);
  });

  it('valida arquivo e dimensões antes de chamar a API', async () => {
    const { el, fixture, catalog, pick, setDims } = setup(throwError(() => new HttpErrorResponse({ status: 404 })));
    await pick(new File(['x'], 'prato.gltf'));
    expect(text(el.querySelector('.c-field-error'))).toContain('Envie um arquivo .glb');
    await pick(new File(['isto nao e glb'], 'falso.glb'));
    expect(text(el.querySelector('.c-field-error'))).toContain('não é um GLB válido');
    await pick(glbFile());
    await setDims('280', '0', '280');
    buttonByText(el, 'Enviar modelo 3D')!.click();
    fixture.detectChanges();
    expect(text(el.querySelector('.c-field-error'))).toContain('largura, altura e profundidade');
    expect(el.querySelector('#pm3-heightMm')!.getAttribute('aria-invalid')).toBe('true');
    expect(catalog.uploadModel3d).not.toHaveBeenCalled();
  });

  it('mostra progresso e erro de validação do servidor', async () => {
    const { el, fixture, catalog, pick, setDims } = setup(throwError(() => new HttpErrorResponse({ status: 404 })));
    catalog.uploadModel3d.mockReturnValueOnce(of({ type: HttpEventType.UploadProgress, loaded: 40, total: 100 }) as never);
    await setDims('280', '70', '280');
    await pick(glbFile());
    buttonByText(el, 'Enviar modelo 3D')!.click();
    fixture.detectChanges();
    expect(text(el.querySelector('.c-upload'))).toContain('Enviando modelo 3D... 40%');

    catalog.uploadModel3d.mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 400 })));
    (fixture.componentInstance as unknown as { busy: { set(v: null): void } }).busy.set(null);
    fixture.detectChanges();
    buttonByText(el, 'Enviar modelo 3D')!.click();
    fixture.detectChanges();
    expect(text(el.querySelector('.c-field-error'))).toContain('GLB 2.0 válido');
  });

  it('recusa arquivo acima de 10 MB antes do envio', () => {
    expect(validateGlbFile(new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'grande.glb'))).toContain('10 MB');
    expect(validateGlbFile(new File([], 'vazio.glb'))).toContain('vazio');
  });

  it('remove com confirmação e volta ao estado sem modelo', () => {
    const { el, fixture, catalog, openDialog } = setup();
    buttonByText(el, 'Remover modelo')!.click();
    fixture.detectChanges();
    expect(catalog.deleteModel3d).not.toHaveBeenCalled();
    buttonByText(openDialog()!, 'Remover')!.click();
    fixture.detectChanges();
    expect(catalog.deleteModel3d).toHaveBeenCalledWith('r1', 'm1', 'p1');
    expect(text(el)).toContain('Nenhum modelo 3D adicionado.');
  });

  it('prévia abre o visualizador com a foto do produto como alternativa', async () => {
    const { el, fixture } = setup(of(MODEL), product({ imageUrl: 'https://ik.imagekit.io/borae/foto.webp' }));
    buttonByText(el, 'Visualizar em 3D')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const mv = el.querySelector('model-viewer')!;
    expect(mv.getAttribute('src')).toBe(MODEL.modelUrl);
    expect(mv.getAttribute('poster')).toContain('foto.webp');
  });

  it('trocar de restaurante descarta o estado local e consulta de novo', () => {
    const { fixture, ctx, catalog, el } = setup();
    catalog.getModel3d.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    ctx.selectedId.set('r2');
    fixture.detectChanges();
    expect(catalog.getModel3d).toHaveBeenLastCalledWith('r2', 'm1', 'p1');
    expect(text(el)).toContain('Nenhum modelo 3D adicionado.');
  });
});

describe('readGlbHeader', () => {
  it('aceita GLB 2.0 e recusa versão 1 ou outro conteúdo', async () => {
    expect(await readGlbHeader(glbFile())).toBeNull();
    const v1 = new DataView(new ArrayBuffer(12));
    v1.setUint32(0, 0x46546c67, true);
    v1.setUint32(4, 1, true);
    expect(await readGlbHeader(new Blob([v1.buffer]))).toContain('glTF 2.0');
    expect(await readGlbHeader(new Blob(['curto']))).toContain('não é um GLB válido');
  });
});
