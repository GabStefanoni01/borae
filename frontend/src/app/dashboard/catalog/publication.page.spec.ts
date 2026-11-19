import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { buttonByText, fakeContext, text } from '../../../testing/catalog-fixtures';
import { RestaurantProfile, RestaurantProfileService } from '../profile/restaurant-profile.service';
import { RestaurantContext, RestaurantRole } from '../restaurant-context.service';
import { ToastService } from '../ui/feedback';
import { PublicationPage, sanitizeFileName } from './publication.page';

const PROFILE = {
  id: 'r1',
  name: 'Cantina Bella Vista!',
  slug: 'cantina-bella-vista',
  category: 'Italiana',
  address: 'Rua Augusta, 500 - São Paulo/SP',
  publicationStatus: 'DRAFT'
} as RestaurantProfile;

function setup(role: RestaurantRole = 'OWNER', profile: RestaurantProfile = PROFILE) {
  const profiles = {
    getProfile: vi.fn(() => of(profile)),
    publish: vi.fn(() => of({ ...profile, publicationStatus: 'PUBLISHED' as const })),
    unpublish: vi.fn(() => of({ ...profile, publicationStatus: 'DRAFT' as const })),
    downloadQrCode: vi.fn(() => of(new Blob(['png'], { type: 'image/png' })))
  };
  TestBed.configureTestingModule({
    imports: [PublicationPage],
    providers: [provideRouter([]), ToastService, { provide: RestaurantContext, useValue: fakeContext(role) }, { provide: RestaurantProfileService, useValue: profiles }]
  });
  const fixture = TestBed.createComponent(PublicationPage);
  fixture.detectChanges();
  return { fixture, profiles, el: fixture.nativeElement as HTMLElement, toast: TestBed.inject(ToastService) };
}

describe('PublicationPage', () => {
  it('publica o perfil do restaurante', () => {
    const { el, fixture, profiles, toast } = setup();
    expect(text(el)).toContain('Seu restaurante ainda não está público.');
    buttonByText(el, 'Publicar restaurante')!.click();
    fixture.detectChanges();
    expect(profiles.publish).toHaveBeenCalledWith('r1');
    expect(text(el)).toContain('Seu restaurante está publicado.');
    expect(toast.toasts().at(-1)?.kind).toBe('success');
  });

  it('despublica somente após confirmar', () => {
    const { el, fixture, profiles } = setup('MANAGER', { ...PROFILE, publicationStatus: 'PUBLISHED' });
    buttonByText(el, 'Despublicar restaurante')!.click();
    fixture.detectChanges();
    expect(profiles.unpublish).not.toHaveBeenCalled();
    const dialog = [...el.querySelectorAll('dialog')].find(d => d.hasAttribute('open'))!;
    buttonByText(dialog, 'Despublicar')!.click();
    fixture.detectChanges();
    expect(profiles.unpublish).toHaveBeenCalledWith('r1');
  });

  it('restaurante suspenso não oferece ações de publicação', () => {
    const { el } = setup('OWNER', { ...PROFILE, publicationStatus: 'SUSPENDED' });
    expect(text(el)).toContain('Seu restaurante está suspenso.');
    expect(buttonByText(el, 'Publicar restaurante')).toBeUndefined();
    expect(buttonByText(el, 'Despublicar restaurante')).toBeUndefined();
  });

  it('baixa o QR Code do backend como PNG com nome sanitizado', () => {
    const { el, profiles } = setup();
    expect(profiles.downloadQrCode).toHaveBeenCalledWith('r1');
    expect(el.querySelector<HTMLImageElement>('.c-qr-box img')).toBeTruthy();
    let downloaded = '';
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) { downloaded = this.download; });
    buttonByText(el, 'Baixar QR Code')!.click();
    click.mockRestore();
    expect(downloaded).toBe('qrcode-cantina-bella-vista.png');
    expect(sanitizeFileName('Café & Bar São João')).toBe('cafe-bar-sao-joao');
  });

  it('erro ao gerar QR Code é exibido com opção de tentar novamente', () => {
    const profiles = { getProfile: vi.fn(() => of(PROFILE)), downloadQrCode: vi.fn(() => throwError(() => new HttpErrorResponse({ status: 500 }))), publish: vi.fn(), unpublish: vi.fn() };
    TestBed.configureTestingModule({
      imports: [PublicationPage],
      providers: [provideRouter([]), ToastService, { provide: RestaurantContext, useValue: fakeContext('OWNER') }, { provide: RestaurantProfileService, useValue: profiles }]
    });
    const fixture = TestBed.createComponent(PublicationPage);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(text(el.querySelector('.c-qr-error'))).toContain('Não foi possível gerar o QR Code');
    expect(buttonByText(el, 'Tentar novamente')).toBeTruthy();
  });

  it('alerta quando faltam campos obrigatórios de D-01 antes da publicação', () => {
    const incomplete = { id: 'r1', name: 'Cantina Bella Vista!', slug: 'cantina-bella-vista', category: null, address: null, publicationStatus: 'DRAFT' } as RestaurantProfile;
    const { el } = setup('OWNER', incomplete);
    expect(text(el)).toContain('Categoria gastronômica');
    expect(text(el)).toContain('Endereço');
  });

  it('STAFF não vê publicação nem QR Code', () => {
    const { el, profiles } = setup('STAFF');
    expect(text(el)).toContain('Disponível para proprietário e gerentes');
    expect(profiles.getProfile).not.toHaveBeenCalled();
    expect(profiles.downloadQrCode).not.toHaveBeenCalled();
    expect(buttonByText(el, 'Baixar QR Code')).toBeUndefined();
  });
});
