# Boraê Frontend

Aplicação web responsiva do Boraê, construída com Angular 20 e TypeScript.

## Executar localmente
Requer Node.js 22 ou superior.
```bash
npm install
npm start
```
A aplicação ficará em `http://localhost:4200`.

## Modo demonstração (sem backend)
```bash
npm run start:demo
```
Simula a API localmente com dados fictícios, para testar login, painel e equipe sem acessar a API real. As contas de teste estão em `src/app/demo/demo-data.ts`. O modo demonstração não faz parte do build de produção.

## Tema
O painel e o login têm tema claro, escuro ou automático (segue o sistema), escolhido no alternador da interface.

## Executar com Docker
Na raiz:
```bash
cp .env.example .env
docker compose up --build
```
O frontend fica em `http://localhost:4200`, servido pelo Nginx. Node.js é usado somente na etapa de build; a imagem final contém Nginx e os arquivos estáticos.
Para construir apenas a imagem: `docker build -t borae-frontend ./frontend`.

## Próximas áreas
- Descoberta e cardápio digital.
- Visualização 3D/AR de pratos.
- Assistente gastronômico.
- Área do consumidor e painel do restaurante.
- Administração da plataforma.

A estrutura Angular inicial foi criada; os módulos funcionais serão implementados por etapas.
