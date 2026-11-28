import { TestBed } from '@angular/core/testing';
import { PhotoArViewerComponent } from './photo-ar-viewer.component';

describe('PhotoArViewerComponent', () => {
  it('mantém a fotografia convencional e só oferece AR após ação explícita', () => {
    TestBed.configureTestingModule({ imports: [PhotoArViewerComponent] });
    const fixture = TestBed.createComponent(PhotoArViewerComponent);
    fixture.componentRef.setInput('imageUrl', 'https://ik.imagekit.io/borae/prato.webp');
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.par-preview img')?.getAttribute('src')).toBe('https://ik.imagekit.io/borae/prato.webp');
    expect(host.querySelector('.par-start')?.textContent).toContain('Posicionar na mesa');
    expect(host.querySelector('bo-model-3d-viewer')).toBeNull();
  });
});
