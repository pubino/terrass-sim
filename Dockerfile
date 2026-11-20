FROM python:3.13.12-slim-bookworm AS assets
WORKDIR /workspace
COPY scripts/build_site.py scripts/
COPY web web
COPY LICENSE.md .
RUN python scripts/build_site.py

FROM python:3.13.12-slim-bookworm AS preview
COPY --from=assets /workspace/build/site /site
WORKDIR /site
EXPOSE 8000
CMD ["python", "-m", "http.server", "8000", "--bind", "0.0.0.0"]

FROM ghcr.io/astral-sh/uv:0.11.3 AS uv
FROM eclipse-temurin:25.0.2_10-jdk-noble AS java
FROM python:3.13.12-slim-bookworm AS test
COPY --from=uv /uv /usr/local/bin/uv
COPY --from=java /opt/java/openjdk /opt/java/openjdk
WORKDIR /workspace
ENV PATH="/workspace/.venv/bin:/opt/java/openjdk/bin:${PATH}" \
    PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
COPY pyproject.toml uv.lock ./
RUN uv sync --locked --python 3.13.12 \
    && playwright install --with-deps --only-shell chromium \
    && rm -rf /var/lib/apt/lists/*
COPY web web
COPY terrass terrass
COPY myutil myutil
COPY scripts scripts
COPY tests tests
COPY LICENSE.md .

FROM node:22.22.0-bookworm-slim AS js-check
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts --no-audit --no-fund
COPY web web
COPY eslint.config.mjs tsconfig.json ./
CMD ["npm", "run", "check"]
