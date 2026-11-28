import { TestBed } from '@angular/core/testing';
import { PhotoArViewerComponent } from './photo-ar-viewer.component';

describe('PhotoArViewerComponent', () => {
  const imageUrl = 'https://ik.imagekit.io/borae/prato.webp';

  function createFixture() {
    TestBed.configureTestingModule({ imports: [PhotoArViewerComponent] });
    const fixture = TestBed.createComponent(PhotoArViewerComponent);
    fixture.componentRef.setInput('imageUrl', imageUrl);
    fixture.detectChanges();
    return fixture;
  }

  it('mantém a fotografia convencional e só oferece AR após ação explícita', () => {
    const fixture = createFixture();
    const host = fixture.nativeElement as HTMLElement;

    expect(host.querySelector('.par-preview img')?.getAttribute('src')).toBe(imageUrl);
    expect(host.querySelector('.par-start')?.textContent).toContain('Posicionar na mesa');
    expect(host.querySelector('bo-model-3d-viewer')).toBeNull();
  });

  it('mantém a fotografia original visível quando a geração de AR falha', () => {
    const fixture = createFixture();
    (fixture.componentInstance as any).state.set('error');
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.par-fallback')?.getAttribute('src')).toBe(imageUrl);
    expect(host.textContent).toContain('Você ainda pode ver a fotografia original');
    expect(host.querySelector('.par-start')?.textContent).toContain('Tentar novamente');
  });

  it('entra em estado de erro quando a imagem não pode ser carregada para exportação', async () => {
    const fixture = createFixture();
    const component = fixture.componentInstance as any;
    spyOn(component, 'loadImage').and.returnValue(Promise.reject(new Error('CORS bloqueado')));

    (fixture.nativeElement as HTMLElement).querySelector('.par-start')!.dispatchEvent(new Event('click'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(component.state()).toBe('error');
    expect((fixture.nativeElement as HTMLElement).querySelector('.par-fallback')).toBeTruthy();
  });
});
