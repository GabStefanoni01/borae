# Boraê Frontend

Aplicação web responsiva do Boraê, construída com Angular 20 e TypeScript.

## Executar localmente
Requer Node.js 22 ou superior.
```bash
npm install
npm start
```
A aplicação ficará em `http://localhost:4200`.

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
