/**
 * MODO DEMONSTRAÇÃO — dados fictícios e credenciais de teste.
 *
 * Usados somente com `npm run start:demo` (configuração "demo" do angular.json).
 * Não fazem parte do build de produção e nunca são enviados à API real.
 *
 * Contas de teste (todas com a mesma senha DEMO_PASSWORD):
 *   dono@demo.borae.test     → Proprietário da Cantina Bella Vista · Equipe no Café da Praça
 *   gerente@demo.borae.test  → Gerente da Cantina Bella Vista · Proprietário do Café da Praça
 *   equipe@demo.borae.test   → Equipe da Cantina Bella Vista (sem acesso à lista da equipe)
 *   novo@demo.borae.test     → Sem restaurante; possui convite pendente (DEMO_INVITE_TOKEN)
 */
export const DEMO_PASSWORD = 'Demo@2026';
export const DEMO_INVITE_TOKEN = 'DEMO-CONVITE-2026';

export type DemoRole = 'OWNER' | 'MANAGER' | 'STAFF';

export interface DemoUser { id: string; fullName: string; email: string; password: string; }
export interface DemoRestaurant { id: string; name: string; slug: string; description?: string; }
export interface DemoMembership { id: string; restaurantId: string; userId: string; role: DemoRole; active: boolean; }
export interface DemoInvitation { id: string; restaurantId: string; email: string; role: 'MANAGER' | 'STAFF'; token: string; expiresAt: string; accepted: boolean; }

export interface DemoDatabase {
  users: DemoUser[];
  restaurants: DemoRestaurant[];
  memberships: DemoMembership[];
  invitations: DemoInvitation[];
}

const CANTINA = 'demo-r-cantina';
const CAFE = 'demo-r-cafe';

export function createDemoSeed(): DemoDatabase {
  const users: DemoUser[] = [
    { id: 'demo-u-dono', fullName: 'Marina Souza', email: 'dono@demo.borae.test', password: DEMO_PASSWORD },
    { id: 'demo-u-gerente', fullName: 'Rafael Lima', email: 'gerente@demo.borae.test', password: DEMO_PASSWORD },
    { id: 'demo-u-equipe', fullName: 'Júlia Andrade', email: 'equipe@demo.borae.test', password: DEMO_PASSWORD },
    { id: 'demo-u-novo', fullName: 'Carlos Mendes', email: 'novo@demo.borae.test', password: DEMO_PASSWORD }
  ];
  return {
    users,
    restaurants: [
      { id: CANTINA, name: 'Cantina Bella Vista', slug: 'cantina-bella-vista', description: 'Massas artesanais e cozinha italiana contemporânea.' },
      { id: CAFE, name: 'Café da Praça', slug: 'cafe-da-praca' }
    ],
    memberships: [
      { id: 'demo-m-1', restaurantId: CANTINA, userId: 'demo-u-dono', role: 'OWNER', active: true },
      { id: 'demo-m-2', restaurantId: CANTINA, userId: 'demo-u-gerente', role: 'MANAGER', active: true },
      { id: 'demo-m-3', restaurantId: CANTINA, userId: 'demo-u-equipe', role: 'STAFF', active: true },
      { id: 'demo-m-4', restaurantId: CAFE, userId: 'demo-u-gerente', role: 'OWNER', active: true },
      { id: 'demo-m-5', restaurantId: CAFE, userId: 'demo-u-dono', role: 'STAFF', active: true }
    ],
    invitations: [
      {
        id: 'demo-i-1', restaurantId: CANTINA, email: 'novo@demo.borae.test', role: 'STAFF',
        token: DEMO_INVITE_TOKEN, expiresAt: new Date(Date.now() + 7 * 86400000).toISOString(), accepted: false
      }
    ]
  };
}
