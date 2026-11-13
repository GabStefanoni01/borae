# Documentação do Boraê

Esta pasta é a referência de produto e engenharia do Boraê. Antes de iniciar uma funcionalidade, consulte os documentos abaixo e mantenha-os atualizados quando uma decisão ou implementação mudar.

## Ordem de leitura para quem está entrando no projeto

1. [Guia de desenvolvimento e integração](development/guide.md) — como trabalhar no repositório, padrões e regras que não devem ser quebradas.
2. [Visão do produto](product/vision.md) — problema, público, proposta e limites do produto.
3. [Requisitos e escopo funcional](product/requirements.md) — o que cada área deve fazer e seus critérios de aceite.
4. [Arquitetura](architecture/overview.md) — decisões técnicas e visão dos módulos.
5. [Modelo de domínio](architecture/domain-model.md) — entidades, relações, papéis e regras de acesso.
6. [API atual](api/current-endpoints.md) — endpoints que já existem no backend.
7. [Roadmap](roadmap.md) — ordem de execução e situação real das entregas.

## Regra de ouro

A documentação diferencia explicitamente:
- **Implementado:** existe no código da branch `main`.
- **Planejado:** faz parte da visão, mas ainda não deve ser tratado como funcionalidade disponível.
- **Pendente:** necessário para completar uma etapa ou preparar a funcionalidade para produção.

Se o código e a documentação divergirem, não presuma. Confira a implementação, abra uma discussão/issue e atualize a documentação na mesma entrega da mudança.

## Estado de referência

Esta documentação foi alinhada à estrutura do repositório e aos recursos presentes na branch `main` na revisão de 13/11/2026. O roadmap deve ser atualizado a cada PR relevante.
