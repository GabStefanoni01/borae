import { environment } from '../../environments/environment';
import { API_URL } from '../auth.service';

let warmedUp = false;

/**
 * "Acorda" a API assim que o site abre.
 *
 * O backend roda no plano gratuito do Render, que desliga o serviço após ~15 min sem uso;
 * a primeira requisição seguinte pode levar de 30 a 60 s. Este ping simples em /actuator/health
 * (rota pública) começa a reativação enquanto a pessoa ainda está na landing ou digitando no login.
 *
 * - `no-cors`: não lê a resposta, então não depende da configuração de CORS do /actuator.
 * - Executa no máximo uma vez por carregamento da página e nunca no modo demonstração.
 */
export function warmUpApi(): void {
  if (warmedUp || environment.demo || typeof fetch === 'undefined') return;
  warmedUp = true;
  fetch(`${API_URL}/actuator/health`, { mode: 'no-cors', cache: 'no-store' }).catch(() => undefined);
}

/** Tempo (ms) a partir do qual uma requisição é considerada lenta e a interface avisa a pessoa. */
export const SLOW_REQUEST_MS = 5000;

export const SLOW_SERVER_MESSAGE =
  'O servidor do Boraê está sendo iniciado e a primeira resposta pode levar até 1 minuto. Não feche esta página.';
