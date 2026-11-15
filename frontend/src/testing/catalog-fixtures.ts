import { computed, signal } from '@angular/core';
import { Category, Menu, Product } from '../app/dashboard/catalog/catalog.models';
import { RestaurantRole } from '../app/dashboard/restaurant-context.service';

/** Contexto do painel simulado: mesmas propriedades usadas pelas telas do catálogo. */
export function fakeContext(role: RestaurantRole | null = 'OWNER', restaurantId = 'r1') {
  const roleSignal = signal<RestaurantRole | null>(role);
  return {
    selectedId: signal<string | null>(restaurantId),
    role: roleSignal,
    canManageCatalog: computed(() => roleSignal() === 'OWNER' || roleSignal() === 'MANAGER'),
    canChangeAvailability: computed(() => roleSignal() !== null),
    canEditProfile: computed(() => roleSignal() === 'OWNER' || roleSignal() === 'MANAGER')
  };
}

export function menu(overrides: Partial<Menu> = {}): Menu {
  return { id: 'm1', name: 'Almoço', description: 'Servido ao meio-dia', status: 'DRAFT', displayOrder: 0, ...overrides };
}

export function category(overrides: Partial<Category> = {}): Category {
  return { id: 'c1', menuId: 'm1', name: 'Entradas', description: null, active: true, displayOrder: 0, ...overrides };
}

export function product(overrides: Partial<Product> = {}): Product {
  return {
    id: 'p1', menuId: 'm1', categoryId: 'c1', name: 'Bruschetta', description: 'Pão e tomate', priceInCents: 2890,
    lifecycleStatus: 'ACTIVE', availability: 'AVAILABLE', displayOrder: 0, ingredients: ['Pão', 'Tomate'],
    addons: [{ name: 'Burrata', priceDeltaInCents: 1500, active: true }], imageUrl: null, ...overrides
  };
}

/** Texto visível de um elemento sem quebras múltiplas. */
export function text(el: Element | null | undefined): string {
  return (el?.textContent ?? '').replace(/\s+/g, ' ').trim();
}

export function buttonByText(root: ParentNode, label: string): HTMLButtonElement | undefined {
  return [...root.querySelectorAll('button')].find(b => text(b) === label);
}
