# Documentação do Boraê

Este diretório descreve o produto, suas regras, a arquitetura, o estado real do código e a sequência planejada de desenvolvimento.

## Ordem de leitura para quem está entrando no projeto

1. [Produto e regras de negócio](PRODUCT.md) — entenda o problema, os usuários, as funcionalidades e as regras que não podem ser quebradas.
2. [Arquitetura](ARCHITECTURE.md) — entenda a stack, os limites entre módulos, segurança, dados e integrações.
3. [API e estado implementado](API.md) — saiba o que já existe no backend e o que ainda não deve ser tratado como pronto.
4. [Roadmap](ROADMAP.md) — siga a ordem de implementação e os critérios de aceite.
5. [Contribuição](CONTRIBUTING.md) — siga o padrão de branches, PRs, testes e documentação.

## Como interpretar os status

- **Implementado:** existe no código integrado à branch principal, conforme o estado registrado nesta documentação.
- **Em validação:** existe implementação, mas ainda precisa de validação funcional/operacional ou de confirmação dos testes.
- **Planejado:** faz parte da visão do Boraê, mas não deve ser presumido como disponível.
- **Fora do MVP:** ideia futura que não deve desviar o foco atual.

A documentação acompanha o código. Quando um PR alterar comportamento, endpoints, entidades, permissões, variáveis ou prioridades, atualize os documentos correspondentes no mesmo PR.

## Princípio de desenvolvimento

Não inventar requisitos. Quando uma regra não estiver descrita aqui, procure o responsável pelo produto antes de decidir. Prefira a menor implementação coerente com a arquitetura existente, sem antecipar microserviços, integrações ou funcionalidades que ainda não foram aprovadas.
