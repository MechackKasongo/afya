# Build statique Vite ; sert avec Nginx et proxifie /api vers le service Docker « api ».
FROM docker.io/library/node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY index.html vite.config.ts tsconfig.json tsconfig.node.json ./
COPY src ./src
# Même origine : requêtes relatives /api/v1/... (voir nginx-default.conf)
ENV VITE_API_BASE_URL=
RUN npm run build

FROM docker.io/library/nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY docker/nginx-default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
