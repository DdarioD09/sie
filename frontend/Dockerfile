# Stage 1: build the static Angular bundle with Node. Stage 2: serve it with nginx, which also
# reverse-proxies /api to the backend container so the browser only ever talks to one origin -
# no CORS to configure in production, same as the ng serve proxy used in local dev.

FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

FROM nginx:1.27-alpine
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
