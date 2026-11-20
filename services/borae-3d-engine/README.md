---
title: Boraê 3D Engine
emoji: 🍽️
colorFrom: orange
colorTo: red
sdk: gradio
python_version: "3.10.13"
app_file: app.py
pinned: false
license: mit
---

# Boraê 3D Engine

Worker experimental de geração 3D para o Boraê, executado em um Hugging Face Space com hardware **ZeroGPU**.

## Estado desta entrega

Esta é a primeira PoC: uma imagem de prato entra e o TripoSR retorna um arquivo GLB. Ainda não está integrado ao Spring Boot, não recebe jobs de produção e não deve ser usado por restaurantes.

## Por que Gradio?

O Hugging Face ZeroGPU atualmente aceita Spaces com SDK Gradio. O Space expõe automaticamente uma API Gradio que pode ser chamada pelo backend; não é um servidor FastAPI hospedado no ZeroGPU.

## Implantação manual no Hugging Face

1. Crie um Space novo na sua conta Hugging Face.
2. Selecione **Gradio** como SDK e **ZeroGPU** como hardware.
3. Defina o Space como **Private**. Não publique fotos de clientes em um Space público.
4. Envie o conteúdo desta pasta para a raiz do repositório do Space:
   `app.py`, `requirements.txt` e `README.md`.
5. Aguarde o build e confira os logs. O primeiro build baixa o código/pesos do TripoSR e compila a extensão `torchmcubes`.
6. Teste o endpoint `generate_model` pela seção **Use via API** do Space.

Não é necessário cadastrar cartão nem escolher GPU paga. Não adicione créditos pré-pagos: em contas elegíveis, a cota gratuita diária é limitada e, em planos com créditos, o uso excedente pode consumir saldo automaticamente.

## Integração futura com o Boraê

- O Spring Boot será o único cliente do Space.
- O Angular nunca receberá o token Hugging Face.
- O token será armazenado como segredo no backend, nunca neste repositório.
- O backend enviará um `generationId` UUID e a imagem; receberá metadados e um arquivo GLB temporário.
- O backend deverá baixar o artefato, validar novamente com `GlbFileValidator`, persistir no ImageKit e controlar aprovação/publicação.
- O Space não será fonte de verdade do estado do job e não publicará modelos no catálogo.

## Limitações conhecidas

- Conta Free: cota ZeroGPU publicada de 5 minutos de GPU por janela diária; fila e disponibilidade variam.
- O processamento pode falhar ou ficar aguardando quando a GPU/cota estiver indisponível.
- TripoSR reconstrói a partir de uma única imagem e pode inventar partes ocultas. Não garante fidelidade dimensional nem aparência de alimento real.
- A saída desta PoC usa cores por vértice; ainda precisamos avaliar qualidade visual e tamanho em pratos reais.
- O arquivo temporário retornado pelo Gradio precisa ser baixado pelo backend logo após a conclusão.
- Nenhuma geração paga ou fallback pago deve ser acionado automaticamente.

## Modelo e licenças

O código TripoSR é obtido do repositório oficial `VAST-AI-Research/TripoSR`, fixado no commit `107cefdc244c39106fa830359024f6a2f1c78871`. O projeto declara licença MIT, incluindo código e pesos. Dependências e seus termos devem ser revisados separadamente antes de qualquer liberação comercial.

## Execução local

A execução local requer Python compatível, PyTorch com CUDA, compilador C++ e dependências do TripoSR. O ZeroGPU é o ambiente-alvo desta PoC; execução local sem GPU pode ser muito lenta ou não ser viável.
