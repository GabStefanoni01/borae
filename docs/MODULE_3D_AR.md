# Especificação de módulo — Experiência 3D e realidade aumentada

**Status:** backend de modelos GLB existente; experiência de fotografia plana em AR implementada na branch `feat/photo-ar-menu`. A experiência ainda depende de validação em aparelhos físicos Android e iPhone.

## Decisão de produto — fotografia plana em AR (novembro/2026)

O escopo aprovado para o cardápio público é **somente a fotografia original do prato posicionada horizontalmente sobre a mesa**. Não gerar geometria do alimento, não usar IA, TripoSR ou fotogrametria.

- A fotografia existente é carregada pelo navegador e convertida em textura de um plano horizontal GLB.
- A conversão é feita no dispositivo do consumidor com Three.js/GLTFExporter; não há GPU, API de geração ou processamento remoto.
- Android: usar WebXR no Chrome com dispositivo compatível com ARCore.
- iPhone: usar AR Quick Look com USDZ gerado pelo `model-viewer` a partir do GLB.
- **Não usar Scene Viewer para este GLB local:** o app externo não consegue ler com segurança URLs `blob:` criadas pelo navegador. Para preservar custo zero e não fazer upload intermediário, o modo Android fica restrito a WebXR no navegador.
- A fotografia é uma placa 2D, sem volume ou espessura real. A escala inicial é aproximada (maior lado de 30 cm) e pode ser ajustada pelo usuário conforme suporte da plataforma.
- Se a imagem não puder ser carregada por CORS, se o navegador não suportar a exportação ou se AR não estiver disponível, manter a fotografia convencional e informar a indisponibilidade.
- [x] Leitura CORS do ImageKit validada manualmente no domínio público do Boraê com uma foto real de produto: carregamento com `crossOrigin = 'anonymous'`, desenho em Canvas e exportação via `canvas.toBlob()` concluídos sem erro. A validação confirma a URL testada; outras imagens/origens devem seguir a mesma política.

Essa abordagem não exige alteração no backend, no storage nem nos modelos de dados. O cadastro de fotos existente é suficiente.

## 1. Objetivo

Permitir que o restaurante associe representação tridimensional aos produtos, para que o consumidor visualize o prato em 3D e, em dispositivos compatíveis, em realidade aumentada.

## 2. Fluxo do restaurante

1. Seleciona um produto cadastrado.
2. Envia um arquivo GLB 2.0.
3. Backend valida a estrutura do arquivo e registra os metadados.
4. Restaurante associa o modelo ao produto existente.
5. Informa largura, altura e profundidade físicas em milímetros.
6. Visualiza prévia no painel (etapa de frontend pendente).
7. Publica o modelo após conferência.

## 3. Fluxo do consumidor

1. Abre o detalhe do produto.
2. Se houver modelo publicado, carrega o visualizador 3D sob demanda (frontend pendente).
3. Pode rotacionar/zoom conforme capacidades do componente.
4. Se navegador/dispositivo suportar AR, apresenta ação para abrir experiência (frontend pendente).
5. Se não suportar AR, mantém visualização 3D ou imagem convencional.

## 4. Regras técnicas

- O formato aceito no backend MVP é GLB 2.0; glTF JSON separado não é aceito nesta etapa.
- O backend valida assinatura, versão, comprimento, chunks e JSON principal do GLB.
- Referências externas em `buffers` e URLs de imagem são rejeitadas; imagens embutidas por data URI são aceitas.
- A validação estrutural não substitui inspeção de geometria, polígonos, texturas ou análise antimalware. Esses controles ainda precisam ser definidos antes de produção.
- PostgreSQL guarda metadados e referência do arquivo; o binário é armazenado no ImageKit.
- ImageKit é o provedor oficial de mídia do Boraê nesta implementação. Não utilizar Cloudflare R2 nem migrar imagens existentes.
- O limite de upload de 10 MB é provisório e acompanha a configuração multipart atual do backend; precisa ser homologado com modelos reais e limites da conta ImageKit.
- O arquivo recebe MIME `model/gltf-binary` e é enviado ao ImageKit como `non-image`; upload e entrega reais ainda precisam ser homologados.
- OWNER e MANAGER podem enviar, substituir, publicar/despublicar e remover modelos; STAFF não administra modelos 3D.
- Cada produto pode ter um modelo associado por vez.
- A URL pública do modelo só é incluída na resposta pública quando o modelo está publicado e o restaurante, cardápio, categoria e produto estão elegíveis.
- A imagem convencional do produto permanece intacta e deve ser usada como fallback.
- Assets devem ser carregados sob demanda no frontend para não penalizar o carregamento inicial do cardápio.

## 5. Escala e precisão

O restaurante informa largura, altura e profundidade em milímetros. Esses valores são metadados de calibração fornecidos pelo operador; não comprovam que o arquivo 3D tenha escala real 1:1. A interface deverá explicar que a escala percebida pode variar conforme dispositivo e ambiente.

## 6. Critérios de aceite

- [x] Backend rejeita arquivos que não atendem à estrutura básica GLB 2.0.
- [x] Backend rejeita referências externas em buffers e imagens.
- [x] OWNER/MANAGER podem gerenciar o modelo; STAFF é bloqueado.
- [x] API pública só inclui modelo publicado de produto elegível.
- [ ] Upload e entrega GLB homologados com a conta ImageKit.
- [x] Testes de integração com PostgreSQL concluídos no CI com Testcontainers (6 testes, sem falhas ou skips). Falhas reais do storage ImageKit ainda dependem de homologação.
- [x] Leitura CORS da foto de produto do ImageKit validada no cardápio público por Canvas e `toBlob()`.
- [ ] Experiência de fotografia plana em AR validada em Android/Chrome/WebXR e iPhone/Safari/Quick Look (teste físico pendente).
- [ ] Visualizador de modelos GLB antigos (se mantido) validado separadamente; não faz parte do novo escopo de foto AR.
- [x] Imagem convencional permanece visível como fallback quando a geração local falha ou AR não está disponível.
- [ ] Testes em dispositivos móveis representativos e acessibilidade concluídos.
