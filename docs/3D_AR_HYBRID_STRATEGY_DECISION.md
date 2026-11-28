# Decisão de produto — geração 3D híbrida no Boraê

**Decisão:** estratégia híbrida: fotos + IA como fluxo principal, com captura guiada como fluxo assistido para melhorar a qualidade da entrada.

**Escopo:** registro de decisão para a PR #30. Esta decisão orienta a prova de conceito e a especificação; não significa que a geração esteja pronta para produção.

## 1. Experiência do restaurante

O restaurante parte de um produto já cadastrado e escolhe como fornecer as imagens:

1. **Fotos + IA (fluxo principal):** enviar fotos existentes do prato. O Boraê valida os arquivos e encaminha as imagens ao pipeline de geração compatível.
2. **Captura guiada (fluxo assistido):** fotografar o prato pelo próprio Boraê, seguindo orientações de enquadramento, iluminação, estabilidade e cobertura de diferentes ângulos. A interface mostra o progresso e permite revisar as imagens antes do envio.

Os dois caminhos convergem para o mesmo fluxo assíncrono de geração, validação, prévia e aprovação. Não criar dois sistemas de jobs, duas entidades ou dois processos de publicação.

A captura guiada deve produzir uma sequência de imagens multiângulo. A quantidade, resolução e cobertura exigidas serão definidas pela prova de conceito e pelo pipeline efetivamente escolhido; não fixar limites sem teste.

## 2. Pipeline e escolha do modelo

- Manter uma abstração de provedor/pipeline (Dish3DGenerationProvider ou equivalente), sem acoplar o domínio a TripoSR, Hugging Face ou outro fornecedor.
- Distinguir explicitamente modelos de imagem única de métodos que realmente utilizam múltiplas vistas.
- TripoSR permanece como baseline experimental para imagem única; não presumir que ele combina automaticamente várias fotos.
- Avaliar um pipeline multi-view para as imagens da captura guiada (por exemplo, fotogrametria/COLMAP ou outro modelo comprovadamente multi-view), comparando fidelidade em pratos reais.
- É permitido que a estratégia de geração selecione o pipeline de acordo com o tipo/quantidade de entrada, desde que o contrato externo permaneça estável.
- Não acionar serviço pago automaticamente. Qualquer fallback pago exige decisão explícita e avaliação de custo.
- A prova de conceito deve registrar qualidade, taxa de sucesso, tempo/fila, requisitos de GPU, custo efetivo, limites e licenças comerciais do código e dos pesos.

## 3. Fluxo comum após a captura

1. Validar tipo real, tamanho, resolução, quantidade e qualidade mínima das imagens.
2. Criar job durável e idempotente no backend Spring Boot.
3. Processar de forma assíncrona no worker/pipeline experimental.
4. Converter/exportar o resultado e validar o GLB com as regras existentes.
5. Armazenar o artefato aprovado no ImageKit após homologação.
6. Exibir prévia 3D para o restaurante.
7. Exigir aprovação explícita antes de publicar/substituir o modelo do produto.

Se a captura for insuficiente, retornar uma falha recuperável com orientação para refazer as fotos; não publicar automaticamente um modelo de baixa qualidade.

## 4. Regras arquiteturais que permanecem

- Spring Boot é a autoridade para autenticação, autorização, isolamento multi-tenant, cotas, jobs, estado, auditoria e publicação.
- O serviço Python/worker de geração é substituível e não altera diretamente o domínio do catálogo.
- Reutilizar Product3DModel, GlbFileValidator, ImageKit e as rotas/visualizador 3D/AR existentes na main.
- Não criar entidade paralela de modelo 3D nem fluxo independente de publicação.
- Manter o modelo publicado anteriormente até a nova geração ser validada e aprovada.
- O cardápio público carrega 3D sob demanda; AR é progressiva e depende do dispositivo. Foto convencional continua sendo fallback.
- Render Free pode coordenar tarefas leves, mas não executar inferência 3D pesada. ZeroGPU é experimental, sujeito a cotas, fila e indisponibilidade; não prometer custo zero ou disponibilidade de produção.

## 5. Critérios específicos de aceite da estratégia híbrida

- [ ] O restaurante consegue iniciar geração com fotos já existentes.
- [ ] O restaurante consegue usar captura guiada multiângulo no próprio Boraê.
- [ ] Ambos os métodos usam o mesmo fluxo de job, validação, revisão e publicação.
- [ ] O backend distingue entradas de imagem única e multi-view sem alegar suporte que o modelo não possui.
- [ ] A prova de conceito compara pelo menos um fluxo de imagem única e um fluxo que realmente consuma múltiplas vistas.
- [ ] A captura insuficiente gera orientação para refazer, sem publicação automática.
- [ ] O GLB é validado e aprovado antes de substituir o modelo ativo.
- [ ] Não há dependência paga automática nem promessa de GPU gratuita ilimitada.
- [ ] A implementação reaproveita o módulo 3D/AR existente, sem duplicar domínio ou rotas.

## 6. Fora desta decisão

Esta decisão não escolhe definitivamente o modelo de IA, não define limites de fotos, não aprova fornecedor pago e não autoriza liberar a geração para todos os restaurantes. Esses pontos dependem da prova de conceito com pratos reais e da verificação de licenças e custos.
