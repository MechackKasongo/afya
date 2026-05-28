# Afya Health — interface web

Application **React + TypeScript + Vite** pour consommer l’API Afya Health (`/api/v1/`).

## Prérequis

- Node.js 20+ recommandé  
- Backend **monolithe SOA** sur **8090** (`./mvnw -pl afya-server spring-boot:run` depuis la racine du dépôt). Le proxy Vite envoie tout **`/api`** vers **8090** (`vite.config.ts`).

## Démarrage

```bash
cd frontend
npm install
# Terminal 1 (racine du dépôt) : ./mvnw -pl afya-server spring-boot:run
npm run dev
```

Ouvrir [http://localhost:5173](http://localhost:5173). Avec **Docker** (`podman-compose`), Nginx envoie **`/api/`** vers le monolithe **`api:8090`**.

## Production

```bash
npm run build
```

Déployer le dossier `dist/` derrière un serveur statique. Définir **`VITE_API_BASE_URL`** vers l’URL du backend et ajouter cette origine dans **`APP_CORS_ALLOWED_ORIGINS`** côté Spring.
