import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconComponent } from '../../shared/icon.component';
import { RestaurantContext } from '../restaurant-context.service';
import { StateComponent } from '../ui/ui.components';
import { ProfileStore } from './profile.store';
import { RestaurantProfile, RestaurantProfileService, UpdateRestaurantProfile, profileErrorMessage } from './restaurant-profile.service';

interface ProfileForm {
  name: string; slug: string; description: string; category: string;
  phone: string; whatsapp: string; instagram: string; website: string;
  address: string; latitude: string; longitude: string;
}

const TEXT_FIELDS = ['description', 'category', 'phone', 'whatsapp', 'instagram', 'website', 'address'] as const;

/** Aba Perfil — GET/PATCH /restaurants/{id}. */
@Component({
  selector: 'bo-profile-form-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, IconComponent, StateComponent],
  template: `
    @if (store.loading() && !store.profile()) {
      <bo-state kind="loading" [compact]="true" title="Carregando perfil" />
    } @else if (store.error() && !store.profile()) {
      <bo-state kind="error" [compact]="true" title="Não foi possível carregar o perfil" [text]="store.error()">
        <button type="button" class="d-btn d-btn-secondary" (click)="store.load()">Tentar novamente</button>
      </bo-state>
    } @else if (store.profile()) {
      <form class="pf" (ngSubmit)="save()" #f="ngForm" novalidate>
        <section class="d-card">
          <div class="d-card-head"><span class="d-card-icon"><bo-icon name="store" [size]="18" /></span><h2>Informações básicas</h2></div>
          <div class="d-form">
            <div class="d-form-row">
              <div class="d-field">
                <label for="pf-name">Nome público</label>
                <input id="pf-name" name="name" class="d-input" [(ngModel)]="form.name" (ngModelChange)="touch()" required maxlength="180" autocomplete="organization">
                <span class="d-hint"><span>Como o restaurante aparece no Boraê.</span><span>{{ form.name.length }}/180</span></span>
              </div>
              <div class="d-field">
                <label for="pf-category">Categoria gastronômica</label>
                <input id="pf-category" name="category" class="d-input" [(ngModel)]="form.category" (ngModelChange)="touch()" maxlength="80" list="pf-categories" placeholder="Ex.: Italiana">
                <datalist id="pf-categories">@for (c of categories; track c) { <option [value]="c"></option> }</datalist>
                <span class="d-hint"><span>Escolha uma sugestão ou escreva a sua.</span><span>{{ form.category.length }}/80</span></span>
              </div>
            </div>
            <div class="d-field">
              <label for="pf-slug">Endereço público (slug)</label>
              <input id="pf-slug" name="slug" class="d-input d-mono" [(ngModel)]="form.slug" (ngModelChange)="touch(); slugError.set('')" required maxlength="120" pattern="^[a-zA-Z0-9À-ÿ-]+$" spellcheck="false" [attr.aria-invalid]="!!slugError() || null" aria-describedby="pf-slug-hint">
              @if (slugError()) { <span class="pf-field-error" role="alert"><bo-icon name="alert" [size]="14" /> {{ slugError() }}</span> }
              <span class="d-hint" id="pf-slug-hint"><span>Letras, números e hífens. O servidor converte para minúsculas e remove acentos.</span><span>{{ form.slug.length }}/120</span></span>
            </div>
            <div class="d-field">
              <label for="pf-desc">Descrição <span>(opcional)</span></label>
              <textarea id="pf-desc" name="description" class="d-input" rows="4" [(ngModel)]="form.description" (ngModelChange)="touch()" maxlength="1000" placeholder="Conte a história, a proposta e o que torna o restaurante especial."></textarea>
              <span class="d-hint"><span></span><span>{{ form.description.length }}/1000</span></span>
            </div>
          </div>
        </section>

        <section class="d-card">
          <div class="d-card-head"><span class="d-card-icon"><bo-icon name="smartphone" [size]="18" /></span><h2>Contato e redes</h2></div>
          <div class="d-form-row">
            <div class="d-field"><label for="pf-phone">Telefone</label><input id="pf-phone" name="phone" type="tel" class="d-input" [(ngModel)]="form.phone" (ngModelChange)="touch()" maxlength="30" placeholder="(11) 3333-4444" autocomplete="tel"></div>
            <div class="d-field"><label for="pf-whats">WhatsApp</label><input id="pf-whats" name="whatsapp" type="tel" class="d-input" [(ngModel)]="form.whatsapp" (ngModelChange)="touch()" maxlength="30" placeholder="(11) 99999-0000"></div>
            <div class="d-field"><label for="pf-insta">Instagram</label><input id="pf-insta" name="instagram" class="d-input" [(ngModel)]="form.instagram" (ngModelChange)="touch()" maxlength="120" placeholder="@seurestaurante"></div>
            <div class="d-field"><label for="pf-site">Site</label><input id="pf-site" name="website" type="url" class="d-input" [(ngModel)]="form.website" (ngModelChange)="touch()" maxlength="255" placeholder="https://"></div>
          </div>
        </section>

        <section class="d-card">
          <div class="d-card-head"><span class="d-card-icon"><bo-icon name="map-pin" [size]="18" /></span><h2>Localização</h2></div>
          <div class="d-form">
            <div class="d-field">
              <label for="pf-address">Endereço</label>
              <textarea id="pf-address" name="address" class="d-input pf-address" rows="2" [(ngModel)]="form.address" (ngModelChange)="touch()" maxlength="500" placeholder="Rua, número, bairro, cidade e estado" autocomplete="street-address"></textarea>
              <span class="d-hint"><span></span><span>{{ form.address.length }}/500</span></span>
            </div>
            <div class="d-form-row pf-coords">
              <div class="d-field"><label for="pf-lat">Latitude</label><input id="pf-lat" name="latitude" type="number" step="0.0000001" min="-90" max="90" class="d-input" [(ngModel)]="form.latitude" (ngModelChange)="touch()" placeholder="-23.5505200"></div>
              <div class="d-field"><label for="pf-lng">Longitude</label><input id="pf-lng" name="longitude" type="number" step="0.0000001" min="-180" max="180" class="d-input" [(ngModel)]="form.longitude" (ngModelChange)="touch()" placeholder="-46.6333090"></div>
              <div class="pf-coord-actions">
                <button type="button" class="d-btn d-btn-secondary" (click)="useMyLocation()" [disabled]="locating()">
                  <bo-icon [name]="locating() ? 'loader' : 'map-pin'" [size]="16" [spin]="locating()" /> Usar minha localização
                </button>
                @if (mapUrl(); as url) { <a class="d-link" [href]="url" target="_blank" rel="noopener">Ver no mapa <bo-icon name="arrow-up-right" [size]="14" /></a> }
              </div>
            </div>
            @if (coordsError(); as msg) { <span class="pf-field-error" role="alert"><bo-icon name="alert" [size]="14" /> {{ msg }}</span> }
            <p class="d-note"><bo-icon name="info" [size]="14" /> Latitude e longitude são informadas juntas. Nesta versão da API, coordenadas salvas podem ser alteradas, mas não removidas.</p>
          </div>
        </section>

        <div class="pf-bar" [class.visible]="dirty() || saving()">
          @if (feedback(); as fb) {
            <p class="pf-feedback" [class.error]="fb.type === 'error'" [attr.role]="fb.type === 'error' ? 'alert' : 'status'">
              <bo-icon [name]="fb.type === 'error' ? 'alert' : 'check-circle'" [size]="16" /> {{ fb.text }}
            </p>
          } @else {
            <p class="pf-feedback muted">{{ dirty() ? 'Você tem alterações não salvas.' : 'Tudo salvo.' }}</p>
          }
          <div class="pf-actions">
            <button type="button" class="d-btn d-btn-ghost" (click)="reset()" [disabled]="!dirty() || saving()">Descartar</button>
            <button type="submit" class="d-btn d-btn-primary" [disabled]="!dirty() || saving() || !f.valid || !!coordsError()">
              <bo-icon [name]="saving() ? 'loader' : 'check'" [size]="16" [spin]="saving()" /> {{ saving() ? 'Salvando...' : 'Salvar perfil' }}
            </button>
          </div>
        </div>
      </form>
    }
  `,
  styles: [`
    .pf{display:grid;gap:16px}
    .pf-address{min-height:64px}
    .pf-coords{align-items:end}
    .pf-coord-actions{display:flex;flex-wrap:wrap;align-items:center;gap:12px;min-height:42px}
    .pf-field-error{display:inline-flex;align-items:center;gap:6px;color:var(--d-error);font-size:12.5px;font-weight:500}
    .pf-bar{position:sticky;bottom:12px;z-index:5;display:flex;flex-wrap:wrap;align-items:center;justify-content:space-between;gap:12px;padding:12px 16px;border:1px solid var(--d-border);border-radius:14px;background:var(--d-surface);box-shadow:var(--d-shadow-pop)}
    .pf-bar:not(.visible){box-shadow:var(--d-shadow)}
    .pf-feedback{display:flex;align-items:center;gap:8px;margin:0;font-size:13.5px;color:var(--d-success);font-weight:500}
    .pf-feedback.error{color:var(--d-error)}
    .pf-feedback.muted{color:var(--d-muted);font-weight:400}
    .pf-actions{display:flex;gap:10px;margin-left:auto}
  `]
})
export class ProfileFormPage {
  protected readonly store = inject(ProfileStore);
  private readonly ctx = inject(RestaurantContext);
  private readonly api = inject(RestaurantProfileService);

  protected form: ProfileForm = this.emptyForm();
  private original = '';
  protected readonly version = signal(0);
  protected readonly saving = signal(false);
  protected readonly locating = signal(false);
  protected readonly slugError = signal('');
  protected readonly feedback = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  protected readonly categories = ['Brasileira', 'Italiana', 'Pizzaria', 'Japonesa', 'Hamburgueria', 'Cafeteria', 'Padaria', 'Doceria', 'Bar', 'Vegetariana', 'Frutos do mar', 'Árabe', 'Mexicana', 'Churrascaria', 'Contemporânea'];

  protected readonly dirty = computed(() => { this.version(); return JSON.stringify(this.form) !== this.original; });

  protected readonly coordsError = computed(() => {
    this.version();
    const lat = this.form.latitude?.toString().trim() ?? '';
    const lng = this.form.longitude?.toString().trim() ?? '';
    if (!!lat !== !!lng) return 'Informe latitude e longitude juntas.';
    const p = this.store.profile();
    if (!lat && !lng && p?.latitude != null) return 'A API ainda não permite remover coordenadas já salvas. Informe novos valores ou descarte a alteração.';
    if (lat && (Number(lat) < -90 || Number(lat) > 90)) return 'A latitude deve estar entre -90 e 90.';
    if (lng && (Number(lng) < -180 || Number(lng) > 180)) return 'A longitude deve estar entre -180 e 180.';
    return '';
  });

  protected readonly mapUrl = computed(() => {
    this.version();
    const lat = Number(this.form.latitude), lng = Number(this.form.longitude);
    if (!this.form.latitude?.toString() || !this.form.longitude?.toString() || Number.isNaN(lat) || Number.isNaN(lng)) return '';
    return `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=17/${lat}/${lng}`;
  });

  constructor() {
    // Preenche o formulário quando o perfil carrega (ou muda de restaurante).
    effect(() => {
      const profile = this.store.profile();
      if (profile) this.fill(profile);
    });
  }

  protected touch(): void {
    this.feedback.set(null);
    this.version.update(v => v + 1);
  }

  protected reset(): void {
    const profile = this.store.profile();
    if (profile) this.fill(profile);
    this.slugError.set('');
    this.feedback.set(null);
  }

  protected useMyLocation(): void {
    if (!navigator.geolocation) {
      this.feedback.set({ type: 'error', text: 'Este navegador não oferece localização.' });
      return;
    }
    this.locating.set(true);
    navigator.geolocation.getCurrentPosition(
      position => {
        this.form.latitude = position.coords.latitude.toFixed(7);
        this.form.longitude = position.coords.longitude.toFixed(7);
        this.locating.set(false);
        this.touch();
      },
      () => {
        this.locating.set(false);
        this.feedback.set({ type: 'error', text: 'Não foi possível obter sua localização. Verifique a permissão do navegador ou preencha manualmente.' });
      },
      { enableHighAccuracy: true, timeout: 15000 }
    );
  }

  protected save(): void {
    const restaurantId = this.ctx.selectedId();
    const profile = this.store.profile();
    if (!restaurantId || !profile || this.saving() || this.coordsError()) return;

    const changes: UpdateRestaurantProfile = { name: this.form.name.trim(), slug: this.form.slug.trim() };
    // Campos de texto sempre enviados: string vazia limpa o campo na API.
    for (const key of TEXT_FIELDS) changes[key] = this.form[key].trim();
    if (this.form.latitude?.toString().trim() && this.form.longitude?.toString().trim()) {
      changes.latitude = Number(this.form.latitude);
      changes.longitude = Number(this.form.longitude);
    }

    this.saving.set(true);
    this.feedback.set(null);
    this.slugError.set('');
    this.api.updateProfile(restaurantId, changes).subscribe({
      next: saved => {
        this.saving.set(false);
        this.store.applySaved(saved);
        this.feedback.set({ type: 'success', text: 'Perfil salvo com sucesso.' });
      },
      error: error => {
        this.saving.set(false);
        const message = profileErrorMessage(error, 'profile');
        if (error instanceof HttpErrorResponse && error.status === 409) this.slugError.set(message);
        this.feedback.set({ type: 'error', text: message });
      }
    });
  }

  private fill(profile: RestaurantProfile): void {
    this.form = {
      name: profile.name ?? '',
      slug: profile.slug ?? '',
      description: profile.description ?? '',
      category: profile.category ?? '',
      phone: profile.phone ?? '',
      whatsapp: profile.whatsapp ?? '',
      instagram: profile.instagram ?? '',
      website: profile.website ?? '',
      address: profile.address ?? '',
      latitude: profile.latitude != null ? String(profile.latitude) : '',
      longitude: profile.longitude != null ? String(profile.longitude) : ''
    };
    this.original = JSON.stringify(this.form);
    this.version.update(v => v + 1);
  }

  private emptyForm(): ProfileForm {
    return { name: '', slug: '', description: '', category: '', phone: '', whatsapp: '', instagram: '', website: '', address: '', latitude: '', longitude: '' };
  }
}
