FROM node:22-alpine AS build
WORKDIR /app
# npm ci installs exactly what package-lock.json pins and fails if the lock
# file and package.json disagree, the same as the CI jobs.
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
