# VIVI Music — sync relay (Hugging Face Spaces fallback)
# SDK: Docker. HF exposes HTTPS in front of this process, so clients connect
# with wss://<username>-<space>.hf.space
FROM node:18-slim

WORKDIR /app

# Install dependencies first (better layer caching)
COPY package.json package-lock.json ./
RUN npm install --omit=dev

# Then the server
COPY server.js ./

# HF Spaces injects PORT (default 7860); server.js honors it.
EXPOSE 7860

CMD ["node", "server.js"]