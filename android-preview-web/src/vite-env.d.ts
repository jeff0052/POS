/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_PROXY_TARGET?: string;
  readonly VITE_QR_ORDERING_URL_BASE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
