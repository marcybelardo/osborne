# Stage 1: Build
FROM node:26-alpine AS build
WORKDIR /app

RUN npm install -g pnpm

COPY package.json pnpm-lock.yaml .
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm run build

# Stage 2: Serve
FROM node:26-alpine
WORKDIR /app

RUN npm install -g pnpm

COPY --from=build /app/dist ./dist
COPY --from=build /app/package.json .
COPY --from=build /app/pnpm-lock.yaml .

# Install prod dependencies only
RUN pnpm install --frozen-lockfile --prod

EXPOSE 3000
CMD ["node", "dist/server/server.js"]
