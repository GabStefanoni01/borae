import { Injectable, effect, inject, signal } from '@angular/core';
import { RestaurantContext } from '../restaurant-context.service';
import { RestaurantProfile, RestaurantProfileService, profileErrorMessage } from './restaurant-profile.service';

/**
 * Estado do perfil do restaurante selecionado, compartilhado entre o cabeçalho de
 * "Meu restaurante" e a aba Perfil. Fornecido pelo componente de layout (escopo da página).
 */
@Injectable()
export class ProfileStore {
  private readonly api = inject(RestaurantProfileService);
  private readonly ctx = inject(RestaurantContext);

  readonly profile = signal<RestaurantProfile | null>(null);
  readonly loading = signal(false);
  readonly error = signal('');

  constructor() {
    effect(() => {
      const restaurant = this.ctx.selected();
      const allowed = this.ctx.canEditProfile();
      this.profile.set(null);
      this.error.set('');
      if (restaurant && allowed) this.load(restaurant.restaurantId);
    });
  }

  load(restaurantId = this.ctx.selectedId()): void {
    if (!restaurantId) return;
    this.loading.set(true);
    this.error.set('');
    this.api.getProfile(restaurantId).subscribe({
      next: profile => { this.profile.set(profile); this.loading.set(false); },
      error: error => { this.loading.set(false); this.error.set(profileErrorMessage(error, 'load')); }
    });
  }

  /** Aplica o perfil devolvido pelo PATCH e reflete nome/slug na navegação. */
  applySaved(profile: RestaurantProfile): void {
    this.profile.set(profile);
    this.ctx.updateRestaurantInfo(profile.id, profile.name, profile.slug);
  }
}
