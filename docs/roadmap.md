# Roadmap

O roadmap é incremental. A ordem pode mudar com base em entrevistas, testes e validação comercial.

## Sprint 0 — Fundação
- [x] Criar repositório.
- [ ] Registrar visão do produto e arquitetura.
- [ ] Definir convenções de código e fluxo Git.
- [ ] Gerar esqueleto executável do backend Spring Boot.
- [ ] Gerar esqueleto executável do frontend Angular.
- [ ] Configurar PostgreSQL e migrations.
- [ ] Configurar OpenAPI e testes básicos.
- [ ] Preparar CI.

## Etapa 1 — Identidade e restaurantes
Cadastro e autenticação; usuários, organizações e vínculos; papéis e permissões; perfil do restaurante; isolamento multi-tenant testado.

## Etapa 2 — Cardápio digital
Categorias e itens; preços, opções e disponibilidade; imagens; página pública do cardápio; QR Code.

## Etapa 3 — Visualização 3D/AR
Upload e validação de GLB; associação ao item; visualização 3D; AR em dispositivos compatíveis e fallback.

## Etapa 4 — Assistente gastronômico
Coleta de intenção e preferências; recomendações limitadas ao catálogo real; validação de preço e disponibilidade; proteções para ingredientes e alergênicos.

## Etapa 5 — Fidelidade individual
Configuração por restaurante; ledger de pontos, recompensas e validade; histórico; resgate transacional e idempotente.

## Etapa 6 — Boraê Club e descoberta
Parceiros participantes; carteira compartilhada separada; regras de financiamento e compensação; catálogo de benefícios; diretório público.

## Futuro
Pedidos, pagamentos, integração PDV, notificações, campanhas avançadas, analytics e aplicativos nativos se houver demanda.

Cada etapa deve ter critérios de aceite, testes automatizados e validação com usuários antes de ser considerada concluída.