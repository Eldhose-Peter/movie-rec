# Build stage
FROM node:22-alpine AS builder
WORKDIR /app

COPY package*.json yarn.lock* ./
RUN npm install
COPY src ./src
COPY tsconfig.json ./
RUN npm run build

# Production stage
FROM node:22-alpine
WORKDIR /app

COPY package*.json yarn.lock* ./
RUN npm install --production
COPY --from=builder /app/dist ./dist
EXPOSE 3000
CMD ["npm", "start"]
