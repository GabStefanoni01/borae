# Fluxos de tela e experiência

Este documento define comportamento e hierarquia funcional. Não fixa identidade visual final, cores ou layout pixel-perfect; isso deve ser validado em protótipos.

## 1. Aplicação e áreas

### Área pública
- Página pública do restaurante.
- Cardápio e detalhes de produtos.
- Descoberta pública (fase posterior).
- Visualização 3D/AR quando houver mídia válida.

### Área autenticada
- Login/cadastro.
- Seleção de restaurante quando o usuário tiver mais de um.
- Dashboard do restaurante selecionado.
- Perfil/configurações.
- Equipe e convites.
- Catálogo/cardápio.
- Configurações futuras de IA, fidelidade e mídia.

### Área administrativa Boraê
Futura e separada da área do restaurante, com autorização global própria.

## 2. Fluxo de autenticação

**Login**
1. Usuário informa e-mail e senha.
2. Frontend valida campos obrigatórios.
3. Backend autentica e devolve sessão/token conforme contrato atual.
4. Frontend mantém sessão e direciona para área autenticada.
5. Erro deve ser claro sem revelar se uma conta existe quando isso representar risco.

**Sessão expirada**
1. Requisição protegida recebe 401.
2. Frontend limpa a sessão local.
3. Redireciona para login.
4. Exibe aviso de sessão expirada.

**Critérios:** não renderizar dados privados sem sessão; não aceitar autorização baseada apenas em guard Angular.

## 3. Onboarding de restaurante

1. Usuário autenticado acessa área de restaurantes.
2. Escolhe criar restaurante.
3. Preenche os campos obrigatórios definidos pelo backend.
4. Backend cria restaurante e membership OWNER na mesma transação.
5. Frontend atualiza lista de restaurantes e seleciona o novo.
6. Próximo passo conduz ao preenchimento do perfil.

**Estados:** carregando, sucesso, validação por campo, erro de rede e erro de negócio.

## 4. Equipe e convites

### Convidar
1. OWNER abre Equipe.
2. Informa e-mail e papel MANAGER ou STAFF.
3. Backend valida permissão e duplicidade de convite.
4. Token é criado e exibido uma única vez para compartilhamento.
5. Interface informa validade e alerta para compartilhar apenas com o destinatário.

### Aceitar
1. Convidado autentica com conta cujo e-mail corresponda.
2. Informa/abre token.
3. Backend valida token, expiração, uso e e-mail.
4. Vínculo é criado ou reativado.
5. Restaurante passa a aparecer na lista do usuário.

### Gerenciar
- OWNER consulta, altera papel e remove membro.
- MANAGER consulta equipe conforme implementação atual.
- Ações não permitidas não devem aparecer como disponíveis, mas o backend também precisa rejeitá-las.

## 5. Perfil do restaurante (futuro)

Tela de edição com seções:
- Informações básicas.
- Endereço/localização.
- Contato e links.
- Logo e capa.
- Identificador público.
- Publicação/visibilidade.

Mostrar prévia pública e avisar quais campos estão incompletos. Campos obrigatórios e publicação final ainda devem ser definidos.

## 6. Gestão de cardápio (futuro)

### Categorias
- Listar, criar, renomear, ordenar, ativar/desativar.
- Não apagar categoria com produtos sem regra de realocação/arquivamento definida.

### Produtos
- Listar com busca e filtro por categoria/status.
- Criar/editar dados, preço, ingredientes, imagem e disponibilidade.
- Arquivar sem perder histórico necessário.
- Pré-visualizar como consumidor.

### Publicação
- Rascunho não aparece publicamente.
- Publicação deve validar consistência mínima dos itens.
- Despublicação deve refletir no acesso público sem apagar dados.

**Pendente:** definir publicação por produto, por menu ou por restaurante; definir se mudanças são publicadas imediatamente ou passam por revisão.

## 7. Cardápio público (futuro)

- Abrir por slug/URL pública.
- Mostrar nome, identidade visual, categorias e produtos publicados.
- Navegação por categoria e pesquisa simples quando houver volume.
- Detalhe do produto com imagem, descrição, preço, ingredientes e opções.
- Indicar indisponibilidade com clareza.
- Se falhar mídia 3D, mostrar imagem normal.
- Não exigir login para consulta.

## 8. Assistente gastronômico (futuro)

- Disponível dentro do cardápio de um restaurante específico.
- Coletar intenção e filtros de forma conversacional.
- Mostrar recomendações com nome, preço e justificativa baseada em dados.
- Permitir abrir detalhe do item recomendado.
- Se não houver item compatível, dizer que não encontrou opções; não inventar.
- Mostrar aviso para confirmar alergênicos diretamente com o estabelecimento.

## 9. Estados transversais

Toda tela com dados remotos deve prever:
- Carregamento (sem duplicar ações).
- Erro recuperável e opção de tentar novamente.
- Estado vazio com orientação para próxima ação.
- Sucesso com feedback acessível.
- Sem permissão.
- Recurso não encontrado ou desativado.
- Layout mobile e teclado/acessibilidade.

## 10. Navegação e responsividade

- Mobile-first, principalmente no cardápio público.
- Área administrativa pode usar layout responsivo com navegação adaptada.
- Não depender exclusivamente de hover.
- Formulários devem preservar dados não sensíveis quando ocorrer erro recuperável.
- Evitar modais para fluxos longos; priorizar páginas ou painéis claros.
