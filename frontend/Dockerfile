# Etapa 1: transforma o código React em arquivos HTML/JS/CSS prontos (pasta dist/)
FROM node:20.18.0-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etapa 2: o nginx entrega esses arquivos e repassa /api ao backend
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
