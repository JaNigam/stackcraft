# Web: static build (theory MDX + content bundled), served by nginx which also forwards /api to the api.
FROM node:24-alpine AS build
WORKDIR /repo/apps/web
COPY apps/web/package.json apps/web/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY content /repo/content
COPY docs/api /repo/docs/api
COPY apps/web ./
RUN npm run build

FROM nginx:1.29-alpine
COPY infra/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /repo/apps/web/dist /usr/share/nginx/html
EXPOSE 80
