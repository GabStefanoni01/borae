import { HttpErrorResponse } from '@angular/common/http';
import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService, RestaurantAccess, RestaurantMember } from '../../auth.service';
import { buttonByText, text } from '../../../testing/catalog-fixtures';
import { AcceptInvitationPage } from './accept-invitation.page';
import { TeamPage } from './team.page';
import { BusinessHoursPage } from '../profile/business-hours.page';
import { ProfileFormPage } from '../profile/profile-form.page';
import { ProfileStore } from '../profile/profile.store';
import { RestaurantProfile, RestaurantProfileService, profileErrorMessage } from '../profile/restaurant-profile.service';
import { RestaurantContext, RestaurantRole, describeHttpError } from '../restaurant-context.service';
import { RestaurantSwitcherComponent } from '../shell/restaurant-switcher.component';

const SAMPLE_PROFILE: RestaurantProfile = {
  id: 'r1',
  name: 'Cantina Central',
  slug: 'cantina-central',
  description: 'Culinária italiana',
  active: true,
  phone: '(11) 3333-4444',
  whatsapp: '11999990000',
  instagram: '@cantinacentral',
  website: 'https://cantina.example.com',
  address: 'Rua Central, 100 - São Paulo/SP',
  latitude: -23.55052,
  longitude: -46.633308,
  category: 'Italiana',
  publicationStatus: 'DRAFT'
};

function makeContext(role: RestaurantRole = 'OWNER', restaurantsList?: RestaurantAccess[]) {
  const list = restaurantsList ?? [
    { membershipId: 'm1', restaurantId: 'r1', restaurantName: 'Cantina Central', restaurantSlug: 'cantina-central', role }
  ];
  const restaurants = signal<RestaurantAccess[]>(list);
  const selectedId = signal<string | null>(list[0]?.restaurantId ?? null);
  const selected = computed(() => restaurants().find(r => r.restaurantId === selectedId()) ?? null);
  const roleSignal = computed<RestaurantRole | null>(() => selected()?.role ?? null);
  return {
    user: signal({ id: 'u1', fullName: 'Ana Dona', email: 'ana@borae.com' }),
    restaurants,
    selectedId,
    selected,
    role: roleSignal,
    hasRestaurants: computed(() => restaurants().length > 0),
    canViewTeam: computed(() => roleSignal() === 'OWNER' || roleSignal() === 'MANAGER'),
    canManageTeam: computed(() => roleSignal() === 'OWNER'),
    canEditProfile: computed(() => roleSignal() === 'OWNER' || roleSignal() === 'MANAGER'),
    canManageCatalog: computed(() => roleSignal() === 'OWNER' || roleSignal() === 'MANAGER'),
    canChangeAvailability: computed(() => roleSignal() !== null),
    select: vi.fn((id: string) => selectedId.set(id)),
    reloadRestaurants: vi.fn(),
    updateRestaurantInfo: vi.fn()
  };
}

describe('Fluxos de perfil, horários, equipe, convites e troca de restaurante', () => {
  it('ProfileFormPage carrega e salva alterações do restaurante e reflete erro 400 de D-01', async () => {
    const profileSig = signal<RestaurantProfile | null>(SAMPLE_PROFILE);
    const applySaved = vi.fn((saved: RestaurantProfile) => profileSig.set(saved));
    const store = {
      profile: profileSig,
      loading: signal(false),
      error: signal(''),
      load: vi.fn(),
      applySaved
    };
    const api = {
      updateProfile: vi.fn(() => of({ ...SAMPLE_PROFILE, name: 'Cantina Nova' }))
    };

    TestBed.configureTestingModule({
      imports: [ProfileFormPage],
      providers: [
        { provide: ProfileStore, useValue: store },
        { provide: RestaurantContext, useValue: makeContext('OWNER') },
        { provide: RestaurantProfileService, useValue: api }
      ]
    });
    const fixture = TestBed.createComponent(ProfileFormPage);
    fixture.detectChanges();
    await fixture.whenStable();

    const nameInput = fixture.nativeElement.querySelector('#pf-name') as HTMLInputElement;
    nameInput.value = 'Cantina Nova';
    nameInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(api.updateProfile).toHaveBeenCalledWith('r1', expect.objectContaining({ name: 'Cantina Nova' }));
    expect(applySaved).toHaveBeenCalled();
    expect(text(fixture.nativeElement)).toContain('Perfil salvo com sucesso.');

    expect(profileErrorMessage(
      new HttpErrorResponse({ status: 400, error: { message: 'Para publicar o restaurante, preencha os campos obrigatórios.' } }),
      'profile'
    )).toContain('campos obrigatórios');
    expect(profileErrorMessage(new HttpErrorResponse({ status: 429 }), 'media-upload'))
      .toContain('Muitas solicitações');
  });

  it('BusinessHoursPage salva grade semanal válida via PUT /business-hours', () => {
    const api = {
      getBusinessHours: vi.fn(() => of([
        { dayOfWeek: 'MONDAY', intervals: [{ opensAt: '11:00:00', closesAt: '15:00:00' }] }
      ])),
      replaceBusinessHours: vi.fn((_id: string, days: unknown) => of(days))
    };

    TestBed.configureTestingModule({
      imports: [BusinessHoursPage],
      providers: [
        { provide: RestaurantContext, useValue: makeContext('MANAGER') },
        { provide: RestaurantProfileService, useValue: api }
      ]
    });
    const fixture = TestBed.createComponent(BusinessHoursPage);
    fixture.detectChanges();

    expect(api.getBusinessHours).toHaveBeenCalledWith('r1');
    buttonByText(fixture.nativeElement, 'Adicionar intervalo')!.click();
    fixture.detectChanges();

    buttonByText(fixture.nativeElement, 'Salvar horários')!.click();
    fixture.detectChanges();

    expect(api.replaceBusinessHours).toHaveBeenCalledWith('r1', [
      {
        dayOfWeek: 'MONDAY',
        intervals: [
          { opensAt: '11:00', closesAt: '15:00' },
          { opensAt: '18:00', closesAt: '23:00' }
        ]
      }
    ]);
    expect(text(fixture.nativeElement)).toContain('Horários salvos com sucesso.');
  });

  it('TeamPage permite ao OWNER listar membros e gerar convite e bloqueia STAFF', async () => {
    const members: RestaurantMember[] = [
      { membershipId: 'm1', restaurantId: 'r1', userId: 'u1', fullName: 'Ana Dona', email: 'ana@borae.com', role: 'OWNER' }
    ];
    const auth = {
      getRestaurantMembers: vi.fn(() => of(members)),
      inviteRestaurantMember: vi.fn(() => of({
        invitationId: 'inv-1',
        email: 'novo@borae.com',
        role: 'STAFF' as const,
        expiresAt: '2026-11-26T12:00:00Z',
        token: 'tok-abc-123'
      })),
      updateRestaurantMemberRole: vi.fn(),
      removeRestaurantMember: vi.fn()
    };

    TestBed.configureTestingModule({
      imports: [TeamPage],
      providers: [
        provideRouter([]),
        { provide: RestaurantContext, useValue: makeContext('OWNER') },
        { provide: AuthService, useValue: auth }
      ]
    });
    const fixture = TestBed.createComponent(TeamPage);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text(fixture.nativeElement)).toContain('Ana Dona');
    const emailInput = fixture.nativeElement.querySelector('#invite-email') as HTMLInputElement;
    emailInput.value = 'novo@borae.com';
    emailInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(auth.inviteRestaurantMember).toHaveBeenCalledWith('r1', 'novo@borae.com', 'STAFF');
    expect(text(fixture.nativeElement)).toContain('tok-abc-123');
  });

  it('AcceptInvitationPage aceita convite válido, recarrega restaurantes e trata token expirado (410)', async () => {
    const ctx = makeContext('STAFF');
    ctx.user.set({ id: 'u2', fullName: 'Bruno', email: 'bruno@borae.com' });
    const auth = {
      acceptRestaurantInvitation: vi.fn()
        .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 410 })))
        .mockReturnValueOnce(of({
          membershipId: 'm2', restaurantId: 'r2', userId: 'u2', fullName: 'Bruno', email: 'bruno@borae.com', role: 'MANAGER' as const
        }))
    };

    TestBed.configureTestingModule({
      imports: [AcceptInvitationPage],
      providers: [
        provideRouter([]),
        { provide: RestaurantContext, useValue: ctx },
        { provide: AuthService, useValue: auth }
      ]
    });
    const fixture = TestBed.createComponent(AcceptInvitationPage);
    fixture.detectChanges();
    await fixture.whenStable();

    const tokenInput = fixture.nativeElement.querySelector('#inv-token') as HTMLTextAreaElement;
    tokenInput.value = 'token-expirado';
    tokenInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(text(fixture.nativeElement)).toContain('expirou ou já foi utilizado');

    tokenInput.value = 'token-valido';
    tokenInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(ctx.reloadRestaurants).toHaveBeenCalledWith('r2');
    expect(text(fixture.nativeElement)).toContain('Convite aceito!');
  });

  it('RestaurantSwitcherComponent redireciona para /app/cardapios ao trocar restaurante dentro de um cardápio', async () => {
    const ctx = makeContext('OWNER', [
      { membershipId: 'm1', restaurantId: 'r1', restaurantName: 'Restaurante A', restaurantSlug: 'rest-a', role: 'OWNER' },
      { membershipId: 'm2', restaurantId: 'r2', restaurantName: 'Restaurante B', restaurantSlug: 'rest-b', role: 'OWNER' }
    ]);

    TestBed.configureTestingModule({
      imports: [RestaurantSwitcherComponent],
      providers: [
        provideRouter([
          { path: 'app/cardapios', children: [] },
          { path: 'app/cardapios/:menuId', children: [] }
        ]),
        { provide: RestaurantContext, useValue: ctx }
      ]
    });

    const router = TestBed.inject(Router);
    await router.navigateByUrl('/app/cardapios/menu-do-rest-a');

    const fixture = TestBed.createComponent(RestaurantSwitcherComponent);
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.d-switch-trigger') as HTMLButtonElement).click();
    fixture.detectChanges();
    const restBButton = [...fixture.nativeElement.querySelectorAll('button')]
      .find((b: HTMLButtonElement) => text(b).includes('Restaurante B')) as HTMLButtonElement;
    restBButton.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(ctx.select).toHaveBeenCalledWith('r2');
    expect(router.url).toBe('/app/cardapios');
    expect(describeHttpError(new HttpErrorResponse({ status: 429 }), 'gerar o convite')).toContain('Muitas solicitações');
  });
});
