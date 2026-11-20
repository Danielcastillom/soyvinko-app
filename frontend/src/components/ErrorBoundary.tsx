import { Component, type ReactNode } from "react";

const RELOAD_KEY = "vinko_chunk_reload";

// Tras un despliegue, una pestaña abierta pide fragmentos JS con hashes que ya no existen.
function isChunkError(error: unknown) {
  const message = error instanceof Error ? `${error.name} ${error.message}` : String(error);
  return /ChunkLoadError|dynamically imported module|Importing a module script failed|error loading dynamically/i.test(
    message,
  );
}

export class ErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false };

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch(error: unknown) {
    if (!isChunkError(error)) return;
    try {
      // Como máximo una recarga automática cada 30 s, para no entrar en bucle si el servidor sigue fallando.
      const last = Number(sessionStorage.getItem(RELOAD_KEY) ?? 0);
      if (Date.now() - last < 30_000) return;
      sessionStorage.setItem(RELOAD_KEY, String(Date.now()));
    } catch {
      return;
    }
    window.location.reload();
  }

  render() {
    if (!this.state.failed) return this.props.children;
    return (
      <div className="panel" role="alert" style={{ margin: "40px auto", maxWidth: 520, textAlign: "center" }}>
        <h2>Algo salió mal al cargar esta sección</h2>
        <p className="muted">Puede que hayamos publicado una nueva versión. Recarga la página para continuar.</p>
        <button className="button primary" onClick={() => window.location.reload()}>
          Recargar página
        </button>
      </div>
    );
  }
}
