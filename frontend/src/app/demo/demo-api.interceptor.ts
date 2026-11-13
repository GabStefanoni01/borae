import { HttpErrorResponse, HttpInterceptorFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { Observable, delay, of, throwError } from 'rxjs';
import { API_URL } from '../auth.service';
import { DemoDatabase, DemoMembership, DemoRole, DemoUser, createDemoSeed } from './demo-data';

/**
 * MODO DEMONSTRAÇÃO — simulador local da API do Boraê.
 *
 * Ativo somente na configuração "demo" (src/environments/environment.demo.ts).
 * Responde às chamadas para API_URL sem nunca acessar a rede, reproduzindo os contratos
 * e regras atuais do backend (docs/API.md e RestaurantTeamService):
 *   auth/login, auth/register, users/me, users/me/restaurants, POST restaurants,
 *   members (GET/PATCH/DELETE), invitations (POST) e restaurant-invitations/accept.
 * O "banco" fica no sessionStorage da aba: some ao fechar a aba.
 */
const STORAGE_KEY = 'borae.demo.db';
const LATENCY_MS = 350;
const PREFIX = `${API_URL}/api/v1`;

export const demoApiInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(API_URL)) return next(request);
  try {
    return handle(request);
  } catch (error) {
    return fail(500, 'Erro inesperado no modo demonstração.');
  }
};

function handle(req: HttpRequest<unknown>): Observable<HttpResponse<unknown>> {
  const db = load();
  const path = req.url.slice(PREFIX.length).split('?')[0];
  const body = (req.body ?? {}) as Record<string, string>;
  const method = req.method;

  // ---------- Autenticação ----------
  if (method === 'POST' && path === '/auth/login') {
    const user = db.users.find(u => u.email.toLowerCase() === String(body['email'] ?? '').trim().toLowerCase());
    if (!user || user.password !== body['password']) return fail(401, 'E-mail ou senha inválidos.');
    return ok(authResponse(user));
  }
  if (method === 'POST' && path === '/auth/register') {
    const fullName = String(body['fullName'] ?? '').trim();
    const email = String(body['email'] ?? '').trim().toLowerCase();
    const password = String(body['password'] ?? '');
    if (fullName.length < 2 || !/^\S+@\S+\.\S+$/.test(email) || password.length < 8) return fail(400, 'Dados inválidos.');
    if (db.users.some(u => u.email === email)) return fail(409, 'E-mail já cadastrado.');
    const user: DemoUser = { id: uid('u'), fullName, email, password };
    db.users.push(user);
    save(db);
    return ok(authResponse(user), 201);
  }

  // ---------- Rotas autenticadas ----------
  const user = currentUser(req, db);
  if (!user) return fail(401, 'Sessão inválida ou usuário inativo.');

  if (method === 'GET' && path === '/users/me') return ok(profile(user));
  if (method === 'GET' && path === '/users/me/restaurants') {
    return ok(db.memberships.filter(m => m.userId === user.id && m.active).map(m => access(db, m)));
  }
  if (method === 'POST' && path === '/restaurants') {
    const name = String(body['name'] ?? '').trim();
    if (!name || name.length > 180 || String(body['description'] ?? '').length > 1000) return fail(400, 'Dados inválidos.');
    const restaurant = { id: uid('r'), name, slug: uniqueSlug(db, name), description: body['description']?.trim() || undefined };
    const membership: DemoMembership = { id: uid('m'), restaurantId: restaurant.id, userId: user.id, role: 'OWNER', active: true };
    db.restaurants.push(restaurant);
    db.memberships.push(membership);
    save(db);
    return ok(access(db, membership), 201);
  }
  if (method === 'POST' && path === '/restaurant-invitations/accept') {
    const invitation = db.invitations.find(i => i.token === String(body['token'] ?? '').trim());
    if (!invitation) return fail(404, 'Convite inválido ou inexistente.');
    if (invitation.accepted || new Date(invitation.expiresAt) < new Date()) return fail(410, 'Este convite expirou ou já foi utilizado.');
    if (invitation.email !== user.email) return fail(403, 'Este convite foi enviado para outro e-mail.');
    const existing = db.memberships.find(m => m.restaurantId === invitation.restaurantId && m.userId === user.id);
    if (existing?.active) return fail(409, 'Você já faz parte deste restaurante.');
    const membership = existing ?? { id: uid('m'), restaurantId: invitation.restaurantId, userId: user.id, role: invitation.role, active: true };
    membership.role = invitation.role;
    membership.active = true;
    if (!existing) db.memberships.push(membership);
    invitation.accepted = true;
    save(db);
    return ok(member(db, membership));
  }

  // ---------- Equipe do restaurante ----------
  const team = path.match(/^\/restaurants\/([^/]+)\/(members|invitations)(?:\/([^/]+))?$/);
  if (team) {
    const [, restaurantId, resource, membershipId] = team;
    if (!db.restaurants.some(r => r.id === restaurantId)) return fail(404, 'Restaurante não encontrado.');
    const actor = db.memberships.find(m => m.restaurantId === restaurantId && m.userId === user.id && m.active);
    if (!actor) return fail(403, 'Você não tem acesso a este restaurante.');

    if (resource === 'members' && method === 'GET' && !membershipId) {
      if (actor.role === 'STAFF') return fail(403, 'Seu papel não permite esta operação.');
      return ok(db.memberships.filter(m => m.restaurantId === restaurantId && m.active).map(m => member(db, m)));
    }
    if (actor.role !== 'OWNER') return fail(403, 'Seu papel não permite esta operação.');

    if (resource === 'invitations' && method === 'POST') {
      const email = String(body['email'] ?? '').trim().toLowerCase();
      const role = body['role'] as DemoRole;
      if (!/^\S+@\S+\.\S+$/.test(email)) return fail(400, 'E-mail inválido.');
      if (role !== 'MANAGER' && role !== 'STAFF') return fail(400, 'O papel OWNER não pode ser concedido por convite.');
      const invitedUser = db.users.find(u => u.email === email);
      if (invitedUser && db.memberships.some(m => m.restaurantId === restaurantId && m.userId === invitedUser.id && m.active)) {
        return fail(409, 'Este usuário já faz parte da equipe.');
      }
      if (db.invitations.some(i => i.restaurantId === restaurantId && i.email === email && !i.accepted && new Date(i.expiresAt) > new Date())) {
        return fail(409, 'Já existe um convite pendente para este e-mail.');
      }
      const invitation = { id: uid('i'), restaurantId, email, role, token: randomToken(), expiresAt: new Date(Date.now() + 7 * 86400000).toISOString(), accepted: false };
      db.invitations.push(invitation);
      save(db);
      return ok({ invitationId: invitation.id, email, role, expiresAt: invitation.expiresAt, token: invitation.token }, 201);
    }

    const target = db.memberships.find(m => m.id === membershipId && m.restaurantId === restaurantId && m.active);
    if (resource === 'members' && membershipId && (method === 'PATCH' || method === 'DELETE')) {
      if (!target) return fail(404, 'Membro não encontrado.');
      if (method === 'PATCH') {
        const role = body['role'] as DemoRole;
        if (role !== 'MANAGER' && role !== 'STAFF') return fail(400, 'Não é permitido atribuir OWNER por esta operação.');
        if (target.role === 'OWNER') return fail(403, 'O papel do proprietário não pode ser alterado.');
        target.role = role;
        save(db);
        return ok(member(db, target));
      }
      if (target.role === 'OWNER') return fail(403, 'O proprietário não pode ser removido da própria equipe.');
      target.active = false;
      save(db);
      return ok(null, 204);
    }
  }

  return fail(404, 'Recurso não disponível no modo demonstração.');
}

// ---------- utilidades ----------
function load(): DemoDatabase {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as DemoDatabase;
  } catch { /* recria abaixo */ }
  const seed = createDemoSeed();
  save(seed);
  return seed;
}

function save(db: DemoDatabase): void {
  try { sessionStorage.setItem(STORAGE_KEY, JSON.stringify(db)); } catch { /* sem persistência */ }
}

function currentUser(req: HttpRequest<unknown>, db: DemoDatabase): DemoUser | undefined {
  const token = req.headers.get('Authorization')?.replace(/^Bearer\s+/i, '') ?? '';
  return token.startsWith('demo.') ? db.users.find(u => u.id === token.slice(5)) : undefined;
}

function authResponse(user: DemoUser) {
  return { accessToken: `demo.${user.id}`, tokenType: 'Bearer', expiresIn: 86400, user: profile(user) };
}

function profile(user: DemoUser) {
  return { id: user.id, fullName: user.fullName, email: user.email };
}

function access(db: DemoDatabase, m: DemoMembership) {
  const r = db.restaurants.find(x => x.id === m.restaurantId)!;
  return { membershipId: m.id, restaurantId: r.id, restaurantName: r.name, restaurantSlug: r.slug, role: m.role };
}

function member(db: DemoDatabase, m: DemoMembership) {
  const u = db.users.find(x => x.id === m.userId)!;
  return { membershipId: m.id, restaurantId: m.restaurantId, userId: u.id, fullName: u.fullName, email: u.email, role: m.role };
}

function uniqueSlug(db: DemoDatabase, name: string): string {
  const base = name.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'restaurante';
  let slug = base;
  for (let n = 2; db.restaurants.some(r => r.slug === slug); n++) slug = `${base}-${n}`;
  return slug;
}

function uid(prefix: string): string {
  return `demo-${prefix}-${Math.random().toString(36).slice(2, 10)}`;
}

function randomToken(): string {
  const bytes = new Uint8Array(24);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('');
}

function ok(body: unknown, status = 200): Observable<HttpResponse<unknown>> {
  return of(new HttpResponse({ status, body })).pipe(delay(LATENCY_MS));
}

function fail(status: number, message: string): Observable<never> {
  return throwError(() => new HttpErrorResponse({ status, error: { message }, statusText: message })).pipe(delay(LATENCY_MS));
}
