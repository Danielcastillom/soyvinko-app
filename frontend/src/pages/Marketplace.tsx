import { useState, useMemo } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import {
  ShoppingBag,
  Plus,
  Package,
  Monitor,
  ArrowRight,
  ArrowUpRight,
  Store,
  Truck,
  Search,
  X,
  ShieldCheck,
  CheckCircle2,
  Clock,
  MapPin,
  CreditCard,
  User as UserIcon,
  Phone,
  Sparkles,
  Check,
  AlertCircle,
  Eye,
  Edit3,
} from "lucide-react";
import { api, request, newKey, money, date, type Data } from "../api";
import { useAuth, useToast } from "../context";
import {
  useData,
  PageTitle,
  Section,
  Empty,
  Loading,
  Badge,
  RunButton,
  Action,
  Modal,
  currencyField,
  ErrorBox,
  ExternalPayments,
} from "../components";
import {
  VinkoIconShieldSafe,
  VinkoIconHandshake,
  VinkoIconSparkle,
  VinkoIconWallet,
} from "../VinkoIcons";

export default function Marketplace() {
  const { user } = useAuth();
  const [params, setParams] = useSearchParams(),
    tab = params.get("tab") ?? "browse";

  return (
    <div className="marketplace-container">
      {/* Header del Marketplace */}
      <div className="marketplace-hero-banner">
        <div className="marketplace-hero-content">
          <div className="marketplace-eyebrow">
            <VinkoIconSparkle size={13} /> MERCADO ABIERTO & TIENDAS OFICIALES
          </div>
          <h1>Marketplace Multicategoría</h1>
          <p>
            Encuentra y vende productos de cualquier categoría: tecnología, herramientas, hogar, repuestos, insumos y mucho más.
            Sin sobreprecios y con <strong>0% de comisión por venta</strong>.
          </p>

          <div className="marketplace-trust-pills">
            <span className="mp-pill">
              <VinkoIconShieldSafe size={14} /> 0% Comisión por venta
            </span>
            <span className="mp-pill">
              <VinkoIconHandshake size={14} /> Trato directo y entrega acordada
            </span>
            <span className="mp-pill">
              <VinkoIconWallet size={14} /> Pagos directos en Bs. o Divisas
            </span>
          </div>
        </div>
      </div>

      {/* Tabs de Navegación del Marketplace */}
      <div className="tabs marketplace-tabs" role="tablist">
        <button
          className={tab === "browse" ? "active" : ""}
          onClick={() => setParams({ tab: "browse" })}
        >
          <ShoppingBag size={17} /> Explorar catálogo
        </button>
        {user && (
          <button
            className={tab === "orders" ? "active" : ""}
            onClick={() => setParams({ tab: "orders" })}
          >
            <Truck size={17} /> Mis pedidos
          </button>
        )}
        {user?.roles.includes("professional") && (
          <button
            className={tab === "store" ? "active" : ""}
            onClick={() => setParams({ tab: "store" })}
          >
            <Store size={17} /> Mi tienda
          </button>
        )}
      </div>

      {/* Vistas según Tab activo */}
      {tab === "orders" && user ? (
        <Orders />
      ) : tab === "store" && user?.roles.includes("professional") ? (
        <MyStore />
      ) : (
        <Browse onOrder={() => setParams({ tab: "orders" })} />
      )}
    </div>
  );
}

// --------------------------------------------------------------------------
// 1. Explorar Catálogo (Browse)
// --------------------------------------------------------------------------
function Browse({ onOrder }: { onOrder: () => void }) {
  const { user } = useAuth();
  const toast = useToast();
  const data = useData(() => api<Data[]>("marketplace"), []);

  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [sortBy, setSortBy] = useState("recommended");
  const [selectedProduct, setSelectedProduct] = useState<Data | null>(null);
  const [buyProduct, setBuyProduct] = useState<Data | null>(null);

  // Filtrado dinámico de productos
  const filteredProducts = useMemo(() => {
    let list = (data.data ?? []).filter((p) => {
      const matchSearch =
        search === "" ||
        `${p.name} ${p.description} ${p.store_name} ${p.sku}`
          .toLowerCase()
          .includes(search.toLowerCase());

      const nameLower = (p.name || "").toLowerCase();
      const descLower = (p.description || "").toLowerCase();
      let matchCat = true;

      if (categoryFilter === "tech") {
        matchCat =
          nameLower.includes("ssd") ||
          nameLower.includes("disco") ||
          nameLower.includes("comput") ||
          nameLower.includes("cable") ||
          nameLower.includes("electr") ||
          nameLower.includes("audio") ||
          descLower.includes("computador") ||
          descLower.includes("tecnología");
      } else if (categoryFilter === "home") {
        matchCat =
          nameLower.includes("limpieza") ||
          nameLower.includes("hogar") ||
          nameLower.includes("mueble") ||
          nameLower.includes("kit") ||
          descLower.includes("hogar") ||
          descLower.includes("limpieza");
      } else if (categoryFilter === "tools") {
        matchCat =
          nameLower.includes("herramienta") ||
          nameLower.includes("taladro") ||
          nameLower.includes("llave") ||
          nameLower.includes("maquin") ||
          descLower.includes("herramienta");
      } else if (categoryFilter === "parts") {
        matchCat =
          nameLower.includes("repuesto") ||
          nameLower.includes("tuber") ||
          nameLower.includes("valvula") ||
          nameLower.includes("filtro") ||
          nameLower.includes("pieza");
      } else if (categoryFilter === "supplies") {
        matchCat =
          nameLower.includes("insumo") ||
          nameLower.includes("accesorio") ||
          nameLower.includes("moda") ||
          nameLower.includes("ropa") ||
          descLower.includes("accesorio");
      }

      return matchSearch && matchCat;
    });

    if (sortBy === "price_asc") {
      list.sort((a, b) => a.price_minor - b.price_minor);
    } else if (sortBy === "price_desc") {
      list.sort((a, b) => b.price_minor - a.price_minor);
    } else if (sortBy === "stock_desc") {
      list.sort((a, b) => (b.stock ?? 0) - (a.stock ?? 0));
    }

    return list;
  }, [data.data, search, categoryFilter, sortBy]);

  return (
    <div className="marketplace-browse-section">
      {/* Barra de Filtros y Búsqueda */}
      <div className="marketplace-controls-card">
        <div className="marketplace-search-box">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            placeholder="Buscar cualquier producto, artículo, herramienta o tienda..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          {search && (
            <button
              type="button"
              className="search-clear-btn"
              onClick={() => setSearch("")}
              aria-label="Borrar búsqueda"
            >
              <X size={15} />
            </button>
          )}
        </div>

        <div className="marketplace-filters-row">
          <div className="category-filter-pills">
            {[
              { id: "all", label: "Todos los productos" },
              { id: "tech", label: "Tecnología & Gadgets" },
              { id: "home", label: "Hogar & Limpieza" },
              { id: "tools", label: "Herramientas & Equipos" },
              { id: "parts", label: "Repuestos & Partes" },
              { id: "supplies", label: "Insumos & Moda" },
            ].map((cat) => (
              <button
                key={cat.id}
                type="button"
                className={`filter-pill ${categoryFilter === cat.id ? "active" : ""}`}
                onClick={() => setCategoryFilter(cat.id)}
              >
                {cat.label}
              </button>
            ))}
          </div>

          <div className="marketplace-sort-box">
            <label htmlFor="marketplace-sort">Ordenar por:</label>
            <select
              id="marketplace-sort"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
            >
              <option value="recommended">Recomendados</option>
              <option value="price_asc">Menor precio</option>
              <option value="price_desc">Mayor precio</option>
              <option value="stock_desc">Mayor stock</option>
            </select>
          </div>
        </div>

        {!data.loading && (
          <div className="marketplace-results-count">
            Mostrando <strong>{filteredProducts.length}</strong>{" "}
            {filteredProducts.length === 1 ? "producto disponible" : "productos disponibles"}
            {search && ` para "${search}"`}
          </div>
        )}
      </div>

      {/* Grilla de Productos */}
      {data.loading ? (
        <div className="professional-grid" aria-busy="true">
          {[...Array(4)].map((_, i) => (
            <article key={i} className="product-card skeleton-card">
              <div className="product-visual skeleton-shimmer" />
              <div className="card-content">
                <div className="skeleton-line skeleton-title skeleton-shimmer" />
                <div className="skeleton-line skeleton-text skeleton-shimmer" />
                <div className="skeleton-line skeleton-price skeleton-shimmer" />
              </div>
            </article>
          ))}
        </div>
      ) : data.error ? (
        <ErrorBox message={data.error} retry={data.reload} />
      ) : filteredProducts.length ? (
        <div className="professional-grid marketplace-grid">
          {filteredProducts.map((p, i) => {
            const isOutOfStock = p.stock <= 0;
            const isLowStock = p.stock > 0 && p.stock <= 3;

            return (
              <article className="product-card" key={p.id}>
                {/* Visual del Producto */}
                <div className={`product-visual cover-${i % 4}`}>
                  <div className="product-visual-icon">
                    {p.name.toLowerCase().includes("ssd") ||
                    p.name.toLowerCase().includes("disco") ? (
                      <Monitor size={52} strokeWidth={1.5} />
                    ) : (
                      <Package size={52} strokeWidth={1.5} />
                    )}
                  </div>

                  {/* Badge de Stock */}
                  <div className="product-stock-tag">
                    {isOutOfStock ? (
                      <span className="stock-badge out">Agotado</span>
                    ) : isLowStock ? (
                      <span className="stock-badge low">¡Últimas {p.stock} unid!</span>
                    ) : (
                      <span className="stock-badge in">En stock ({p.stock})</span>
                    )}
                  </div>
                </div>

                {/* Contenido de la Tarjeta */}
                <div className="card-content product-body">
                  <div className="product-store-row">
                    <Link
                      to={`/professionals/${p.professional_id}`}
                      className="store-chip-link"
                      title="Ver perfil del profesional vendedor"
                    >
                      <Store size={13} />
                      <span>{p.store_name}</span>
                      <ArrowUpRight size={12} />
                    </Link>
                    {p.sku && <span className="sku-badge">SKU: {p.sku}</span>}
                  </div>

                  <h2 className="product-title" title={p.name}>
                    {p.name}
                  </h2>
                  <p className="product-desc clamp-2">{p.description}</p>

                  <div className="product-price-row">
                    <div>
                      <small className="muted">Precio final</small>
                      <strong className="product-price">
                        {money(p.price_minor, p.currency)}
                      </strong>
                    </div>
                  </div>

                  {/* Acciones de Compra y Detalle */}
                  <div className="product-actions-group">
                    <button
                      type="button"
                      className="button secondary small"
                      onClick={() => setSelectedProduct(p)}
                    >
                      <Eye size={15} /> Ver detalles
                    </button>

                    {user ? (
                      <button
                        type="button"
                        className="button primary small"
                        disabled={isOutOfStock}
                        onClick={() => setBuyProduct(p)}
                      >
                        <ShoppingBag size={15} /> Solicitar compra
                      </button>
                    ) : (
                      <Link className="button primary small" to="/login">
                        Ingresar para comprar
                      </Link>
                    )}
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      ) : (
        <Empty
          title="No encontramos productos con ese criterio"
          text="Intenta con otro término de búsqueda o selecciona 'Todos los productos'."
        >
          <button
            type="button"
            className="button secondary small"
            onClick={() => {
              setSearch("");
              setCategoryFilter("all");
            }}
          >
            Limpiar filtros
          </button>
        </Empty>
      )}

      {/* Modal de Detalle de Producto */}
      {selectedProduct && (
        <Modal
          title={selectedProduct.name}
          onClose={() => setSelectedProduct(null)}
        >
          <div className="product-detail-modal-body">
            <div className="modal-product-store">
              <Store size={16} />
              <span>Vendido por: <strong>{selectedProduct.store_name}</strong></span>
              <Link
                to={`/professionals/${selectedProduct.professional_id}`}
                className="button secondary small"
              >
                Ver perfil profesional <ArrowUpRight size={14} />
              </Link>
            </div>

            <p className="modal-product-description">
              {selectedProduct.description}
            </p>

            <div className="modal-product-meta-grid">
              <div>
                <small className="muted">Precio unitario</small>
                <div className="modal-price">
                  {money(selectedProduct.price_minor, selectedProduct.currency)}
                </div>
              </div>
              <div>
                <small className="muted">Disponibilidad</small>
                <div>
                  <strong>{selectedProduct.stock} unidades en almacén</strong>
                </div>
              </div>
              {selectedProduct.sku && (
                <div>
                  <small className="muted">Código SKU</small>
                  <div>
                    <code>{selectedProduct.sku}</code>
                  </div>
                </div>
              )}
            </div>

            <div className="modal-trust-box">
              <VinkoIconShieldSafe size={18} />
              <span>
                Al solicitar este producto, reservas inventario de inmediato. La entrega y el pago se coordinan directamente con el vendedor sin comisiones de plataforma.
              </span>
            </div>

            <div className="modal-footer-actions">
              <button
                type="button"
                className="button secondary"
                onClick={() => setSelectedProduct(null)}
              >
                Cerrar
              </button>
              {user ? (
                <button
                  type="button"
                  className="button primary"
                  disabled={selectedProduct.stock <= 0}
                  onClick={() => {
                    const prod = selectedProduct;
                    setSelectedProduct(null);
                    setBuyProduct(prod);
                  }}
                >
                  <ShoppingBag size={16} /> Comprar ahora
                </button>
              ) : (
                <Link className="button primary" to="/login">
                  Iniciar sesión para comprar
                </Link>
              )}
            </div>
          </div>
        </Modal>
      )}

      {/* Modal Interactivo de Compra (Checkout Rápido) */}
      {buyProduct && (
        <BuyModal
          product={buyProduct}
          onClose={() => setBuyProduct(null)}
          onSuccess={() => {
            setBuyProduct(null);
            toast("¡Solicitud de compra enviada con éxito! Revisa 'Mis pedidos'.");
            onOrder();
          }}
        />
      )}
    </div>
  );
}

// --------------------------------------------------------------------------
// 2. Modal de Compra con Contador Interactivo
// --------------------------------------------------------------------------
function BuyModal({
  product,
  onClose,
  onSuccess,
}: {
  product: Data;
  onClose: () => void;
  onSuccess: () => void;
}) {
  const [quantity, setQuantity] = useState(1);
  const [deliveryNote, setDeliveryNote] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const maxStock = Math.max(1, product.stock ?? 1);
  const totalAmount = product.price_minor * quantity;

  async function handleConfirmOrder() {
    setLoading(true);
    setError("");
    try {
      const order = await api<Data>("create_order", {
        store: product.store_id,
        items: [{ product_id: product.id, quantity }],
        key: newKey(),
      });

      // Si el comprador dejó nota de entrega, guardarla en el pedido
      if (deliveryNote.trim() && order?.id) {
        try {
          await request(`/api/v1/integrations/orders/${order.id}/delivery`, "POST", {
            name: "Comprador SoyVinko",
            phone: "Pendiente",
            address: deliveryNote,
          });
        } catch {
          // No bloquea el flujo principal
        }
      }

      onSuccess();
    } catch (e: any) {
      setError(e?.message || "No se pudo procesar la orden de compra.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <Modal title="Confirmar Solicitud de Compra" onClose={onClose}>
      <div className="buy-modal-content">
        <div className="buy-modal-product-summary">
          <div>
            <h3>{product.name}</h3>
            <small className="muted">Vendido por: {product.store_name}</small>
          </div>
          <div className="unit-price-chip">
            {money(product.price_minor, product.currency)} c/u
          </div>
        </div>

        {error && <div className="error-box" role="alert">{error}</div>}

        {/* Selector interactivo de cantidad */}
        <div className="quantity-control-section">
          <label>Cantidad a solicitar:</label>
          <div className="quantity-stepper">
            <button
              type="button"
              className="qty-btn"
              disabled={quantity <= 1 || loading}
              onClick={() => setQuantity((q) => Math.max(1, q - 1))}
            >
              −
            </button>
            <span className="qty-value">{quantity}</span>
            <button
              type="button"
              className="qty-btn"
              disabled={quantity >= maxStock || loading}
              onClick={() => setQuantity((q) => Math.min(maxStock, q + 1))}
            >
              +
            </button>
          </div>
          <small className="muted">Máximo disponible: {maxStock} unidades</small>
        </div>

        {/* Resumen del Total */}
        <div className="checkout-total-card">
          <div className="total-line">
            <span>Subtotal ({quantity} {quantity === 1 ? "unidad" : "unidades"}):</span>
            <strong>{money(totalAmount, product.currency)}</strong>
          </div>
          <div className="total-line commission">
            <span>Comisión de plataforma:</span>
            <span className="green-zero">$0.00 (Gratis)</span>
          </div>
          <div className="total-line grand-total">
            <span>Total a pagar al vendedor:</span>
            <strong>{money(totalAmount, product.currency)}</strong>
          </div>
        </div>

        {/* Campo de notas de entrega */}
        <div className="checkout-field-group">
          <label htmlFor="delivery-note">Indicaciones de entrega o retiro (opcional):</label>
          <textarea
            id="delivery-note"
            rows={3}
            placeholder="Ej: Deseo coordinar entrega personal en Caracas, o envío por encomienda..."
            value={deliveryNote}
            onChange={(e) => setDeliveryNote(e.target.value)}
          />
        </div>

        <div className="checkout-policy-notice">
          <CheckCircle2 size={16} className="notice-icon" />
          <p>
            Al confirmar, el inventario se aparta a tu nombre. Acordarás el pago directo (Pago Móvil, Efectivo, Zelle) y lugar de entrega directamente con el vendedor.
          </p>
        </div>

        <div className="modal-footer-actions">
          <button
            type="button"
            className="button secondary"
            disabled={loading}
            onClick={onClose}
          >
            Cancelar
          </button>
          <button
            type="button"
            className="button primary"
            disabled={loading}
            onClick={handleConfirmOrder}
          >
            {loading ? "Procesando..." : "Confirmar compra"}
          </button>
        </div>
      </div>
    </Modal>
  );
}

// --------------------------------------------------------------------------
// 3. Mis Pedidos (Orders con Stepper de Estado y Acciones Claras)
// --------------------------------------------------------------------------
function Orders() {
  const { user } = useAuth();
  const data = useData(() => api<Data[]>("my_orders"), [], 10000);
  const [deliveryModal, setDeliveryModal] = useState<Data | null>(null);
  const [filterStatus, setFilterStatus] = useState("all");

  const orders = useMemo(() => {
    let list = data.data ?? [];
    if (filterStatus === "active") {
      list = list.filter((o) => ["pending", "accepted", "dispatched"].includes(o.status));
    } else if (filterStatus === "completed") {
      list = list.filter((o) => ["delivered", "settled"].includes(o.status));
    }
    return list;
  }, [data.data, filterStatus]);

  return (
    <div className="marketplace-orders-section">
      <div className="orders-header-row">
        <div>
          <h2>Gestión de Pedidos</h2>
          <p className="muted">
            Monitorea el estado de tus compras y ventas de productos con seguimiento en tiempo real.
          </p>
        </div>
        <div className="orders-filter-chips">
          <button
            className={`pill-chip ${filterStatus === "all" ? "active" : ""}`}
            onClick={() => setFilterStatus("all")}
          >
            Todos ({data.data?.length ?? 0})
          </button>
          <button
            className={`pill-chip ${filterStatus === "active" ? "active" : ""}`}
            onClick={() => setFilterStatus("active")}
          >
            En curso
          </button>
          <button
            className={`pill-chip ${filterStatus === "completed" ? "active" : ""}`}
            onClick={() => setFilterStatus("completed")}
          >
            Finalizados
          </button>
        </div>
      </div>

      {data.loading ? (
        <Loading />
      ) : data.error ? (
        <ErrorBox message={data.error} retry={data.reload} />
      ) : orders.length ? (
        orders.map((o) => {
          const isCustomer = o.customer_id === user?.id;

          // Etapas del pedido para el stepper
          const isStep1 = true;
          const isStep2 = ["accepted", "dispatched", "delivered", "settled"].includes(o.status);
          const isStep3 = ["dispatched", "delivered", "settled"].includes(o.status);
          const isStep4 = ["delivered", "settled"].includes(o.status);
          const isCancelled = o.status === "cancelled";

          return (
            <section key={o.id} className={`panel order-card-panel ${isCancelled ? "cancelled-order" : ""}`}>
              <div className="order-panel-header">
                <div className="order-main-meta">
                  <span className={`role-badge ${isCustomer ? "buyer" : "seller"}`}>
                    {isCustomer ? "Mi compra" : "Mi venta"}
                  </span>
                  <h3>Pedido #{o.id.slice(0, 8)}</h3>
                  <span className="order-date-text">
                    <Clock size={14} /> {date(o.created_at)}
                  </span>
                </div>
                <div className="order-status-box">
                  <Badge value={o.status} />
                </div>
              </div>

              {/* Stepper Visual de Seguimiento */}
              {!isCancelled ? (
                <div className="order-stepper">
                  <div className={`step-item ${isStep1 ? "done" : ""}`}>
                    <div className="step-circle">{isStep1 ? "✓" : "1"}</div>
                    <span>Solicitado</span>
                  </div>
                  <div className={`step-bar ${isStep2 ? "done" : ""}`} />
                  <div className={`step-item ${isStep2 ? "done" : ""}`}>
                    <div className="step-circle">{isStep2 ? "✓" : "2"}</div>
                    <span>Aceptado</span>
                  </div>
                  <div className={`step-bar ${isStep3 ? "done" : ""}`} />
                  <div className={`step-item ${isStep3 ? "done" : ""}`}>
                    <div className="step-circle">{isStep3 ? "✓" : "3"}</div>
                    <span>En camino</span>
                  </div>
                  <div className={`step-bar ${isStep4 ? "done" : ""}`} />
                  <div className={`step-item ${isStep4 ? "done" : ""}`}>
                    <div className="step-circle">{isStep4 ? "✓" : "4"}</div>
                    <span>Entregado</span>
                  </div>
                </div>
              ) : (
                <div className="order-cancelled-notice">
                  <AlertCircle size={16} /> Pedido cancelado. Las unidades volvieron al inventario.
                </div>
              )}

              {/* Lista de Productos del Pedido */}
              <div className="order-items-table">
                {o.items?.map((i: Data) => (
                  <div key={i.id ?? i.product_id} className="order-item-row">
                    <div className="order-item-title">
                      <Package size={16} />
                      <strong>{i.name_snapshot ?? i.product_name ?? i.name}</strong>
                      <span className="item-qty">× {i.quantity}</span>
                    </div>
                    <div className="order-item-price">
                      {money(i.unit_price_minor * i.quantity, o.currency)}
                    </div>
                  </div>
                ))}
                <div className="order-total-bar">
                  <span>Total acordado:</span>
                  <strong>{money(o.total_minor, o.currency)}</strong>
                </div>
              </div>

              {/* Acciones del Pedido según Rol */}
              <div className="order-actions-bar">
                {/* Vendedor: Aceptar pedido */}
                {!isCustomer && o.status === "pending" && (
                  <RunButton
                    className="button primary small"
                    run={() => api("advance_order", { order: o.id, action: "accept" })}
                    after={data.reload}
                  >
                    <Check size={15} /> Aceptar pedido
                  </RunButton>
                )}

                {/* Vendedor: Marcar enviado */}
                {!isCustomer && o.status === "accepted" && (
                  <RunButton
                    className="button primary small"
                    run={() => api("advance_order", { order: o.id, action: "dispatch" })}
                    after={data.reload}
                  >
                    <Truck size={15} /> Marcar como enviado
                  </RunButton>
                )}

                {/* Comprador: Confirmar recepción */}
                {isCustomer && ["accepted", "dispatched"].includes(o.status) && (
                  <RunButton
                    className="button primary small"
                    confirm="¿Confirmas que recibiste los productos a satisfacción?"
                    run={() => api("advance_order", { order: o.id, action: "receive" })}
                    after={data.reload}
                  >
                    <CheckCircle2 size={15} /> Confirmar recepción
                  </RunButton>
                )}

                {/* Comprador: Compartir datos de entrega */}
                {isCustomer && ["accepted", "dispatched", "pending"].includes(o.status) && (
                  <Action
                    title="Datos de entrega"
                    className="button secondary small"
                    fields={[
                      { name: "name", label: "Nombre de quien recibe", required: true },
                      { name: "phone", label: "Teléfono de contacto", required: true },
                      {
                        name: "address",
                        label: "Dirección detallada o punto de encuentro",
                        type: "textarea",
                        required: true,
                      },
                    ]}
                    action={(v) =>
                      request(`/api/v1/integrations/orders/${o.id}/delivery`, "POST", v)
                    }
                  />
                )}

                {/* Ver datos de entrega (ambos) */}
                {["accepted", "dispatched", "delivered"].includes(o.status) && (
                  <RunButton
                    className="button secondary small"
                    run={async () => {
                      const res = await request(`/api/v1/integrations/orders/${o.id}/delivery`);
                      setDeliveryModal(res);
                    }}
                  >
                    <MapPin size={15} /> Ver datos de entrega
                  </RunButton>
                )}

                {/* Comprador: Reportar pago externo */}
                {isCustomer && ["accepted", "dispatched", "delivered"].includes(o.status) && (
                  <Action
                    title="Reportar pago"
                    className="button secondary small"
                    fields={[
                      {
                        name: "amount",
                        label: "Importe pagado",
                        type: "number",
                        min: 1,
                        value: o.total_minor / 100,
                        required: true,
                      },
                      {
                        name: "method",
                        label: "Medio de pago utilizado",
                        options: [
                          { value: "Pago Móvil", label: "Pago Móvil" },
                          { value: "Transferencia Bancaria", label: "Transferencia Bancaria" },
                          { value: "Divisas en Efectivo", label: "Divisas en Efectivo" },
                          { value: "Zelle", label: "Zelle" },
                          { value: "Punto de Venta / Tarjeta", label: "Punto de Venta / Tarjeta" },
                        ],
                        required: true,
                      },
                    ]}
                    action={(v) =>
                      api("report_external_payment", {
                        contract: null,
                        order: o.id,
                        amount: Math.round(v.amount * 100),
                        currency: o.currency,
                        method: v.method,
                      })
                    }
                    after={data.reload}
                  />
                )}

                {/* Cancelar pedido */}
                {["pending", "accepted"].includes(o.status) && (
                  <RunButton
                    className="button secondary small danger"
                    confirm="¿Deseas cancelar el pedido? Las unidades reservadas volverán al inventario."
                    run={() => api("advance_order", { order: o.id, action: "cancel" })}
                    after={data.reload}
                  >
                    Cancelar pedido
                  </RunButton>
                )}
              </div>

              {/* Historial de pagos reportados */}
              <ExternalPayments order={o.id} />
            </section>
          );
        })
      ) : (
        <Empty
          title="No tienes pedidos registrados"
          text="Cuando compres o vendas productos en el Marketplace, los verás organizados aquí."
        />
      )}

      {/* Modal de datos de entrega */}
      {deliveryModal && (
        <Modal
          title="Datos de Entrega del Pedido"
          onClose={() => setDeliveryModal(null)}
        >
          <div className="delivery-modal-body">
            <div className="delivery-info-row">
              <UserIcon size={18} />
              <div>
                <small className="muted">Destinatario:</small>
                <strong>{deliveryModal.recipient_name || "No especificado"}</strong>
              </div>
            </div>
            <div className="delivery-info-row">
              <Phone size={18} />
              <div>
                <small className="muted">Teléfono:</small>
                <strong>{deliveryModal.phone || "No especificado"}</strong>
              </div>
            </div>
            <div className="delivery-info-row">
              <MapPin size={18} />
              <div>
                <small className="muted">Dirección o instrucciones:</small>
                <p>{deliveryModal.address || "Pendiente por coordinar"}</p>
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

// --------------------------------------------------------------------------
// 4. Mi Tienda (Gestión de Tienda para Profesionales)
// --------------------------------------------------------------------------
function MyStore() {
  const pro = useData(() => api("my_professional"), []);
  const data = useData(() => api<Data[]>("my_products"), []);
  const files = useData(() => api<Data[]>("my_files"), []);
  const store = pro.data?.store;

  const totalStock = (data.data ?? []).reduce(
    (acc, curr) => acc + (curr.stock ?? 0),
    0
  );

  return (
    <div className="mystore-section">
      {!store ? (
        <div className="activate-store-hero">
          <div className="store-hero-icon">
            <Store size={44} />
          </div>
          <h2>Abre tu Tienda en SoyVinko</h2>
          <p>
            Vende herramientas, tecnología, insumos, repuestos o cualquier artículo directamente a clientes de toda la red.
            Recibe el 100% de tus ventas sin comisiones por transacción.
          </p>

          <div className="store-benefits-grid">
            <div className="benefit-card">
              <VinkoIconShieldSafe size={24} />
              <strong>0% Comisión</strong>
              <p>El dinero de tus ventas es íntegro para ti.</p>
            </div>
            <div className="benefit-card">
              <VinkoIconHandshake size={24} />
              <strong>Cualquier Producto</strong>
              <p>Comercializa productos de cualquier categoría de forma directa.</p>
            </div>
            <div className="benefit-card">
              <Package size={24} />
              <strong>Control de Stock</strong>
              <p>Gestiona precios, inventario y catálogo con facilidad.</p>
            </div>
          </div>

          <div className="store-activation-cta">
            <Action
              title="Activar mi tienda ahora"
              className="button primary"
              fields={[
                { name: "name", label: "Nombre comercial de la tienda", required: true },
                {
                  name: "slug",
                  label: "Identificador web único (slug)",
                  hint: "Ej: tecno-daniel (letras minúsculas y guiones)",
                  required: true,
                },
                {
                  name: "description",
                  label: "Descripción de tu tienda y productos",
                  type: "textarea",
                  required: true,
                },
              ]}
              action={(v) => api("activate_store", v)}
              after={pro.reload}
            />
            <Link className="button secondary" to="/wallet">
              Ver planes profesionales
            </Link>
          </div>
        </div>
      ) : (
        <>
          {/* Dashboard de la Tienda */}
          <div className="store-dashboard-card">
            <div className="store-info-col">
              <div className="store-badge-top">
                <span className="live-dot" /> Tienda Activa
              </div>
              <h2>{store.name}</h2>
              <p>{store.description}</p>
            </div>

            <div className="store-metrics-col">
              <div className="metric-box">
                <strong>{data.data?.length ?? 0}</strong>
                <span>Productos publicados</span>
              </div>
              <div className="metric-box">
                <strong>{totalStock}</strong>
                <span>Unidades en stock</span>
              </div>
              <div className="metric-box">
                <strong className="green-zero">0%</strong>
                <span>Comisión por venta</span>
              </div>
            </div>
          </div>

          {/* Gestión de Inventario */}
          <Section
            title="Catálogo e Inventario"
            action={
              <Action
                title="Añadir nuevo producto"
                className="button primary small"
                fields={[
                  { name: "sku", label: "Código SKU / Referencia", required: true },
                  { name: "name", label: "Nombre del producto o artículo", required: true },
                  {
                    name: "description",
                    label: "Descripción detallada",
                    type: "textarea",
                    required: true,
                  },
                  currencyField,
                  {
                    name: "amount",
                    label: "Precio unitario (USD o VES)",
                    type: "number",
                    min: 0,
                    required: true,
                  },
                  {
                    name: "stock",
                    label: "Unidades iniciales en stock",
                    type: "number",
                    min: 0,
                    required: true,
                  },
                ]}
                action={(v) =>
                  api("add_product", {
                    store: store.id,
                    sku: v.sku,
                    name: v.name,
                    description: v.description,
                    currency: v.currency,
                    price: Math.round(v.amount * 100),
                    stock: v.stock,
                  })
                }
                after={data.reload}
              />
            }
          >
            {data.loading ? (
              <Loading />
            ) : data.data?.length ? (
              <div className="store-products-list">
                {data.data.map((p) => (
                  <div className="store-product-item" key={p.id}>
                    <div className="product-summary-col">
                      <div className="prod-badge-row">
                        <Badge value={p.status} />
                        {p.sku && <span className="sku-tag">SKU: {p.sku}</span>}
                      </div>
                      <h3>{p.name}</h3>
                      <p className="muted small">{p.description}</p>
                    </div>

                    <div className="product-numbers-col">
                      <div className="price-tag">
                        {money(p.price_minor, p.currency)}
                      </div>
                      <div className="stock-counter">
                        Stock: <strong>{p.stock} unidades</strong>
                      </div>
                    </div>

                    <div className="product-edit-col">
                      <Action
                        title="Editar"
                        className="button secondary small"
                        fields={[
                          { name: "name", label: "Nombre", value: p.name, required: true },
                          {
                            name: "description",
                            label: "Descripción",
                            type: "textarea",
                            value: p.description,
                            required: true,
                          },
                          {
                            name: "amount",
                            label: "Precio",
                            type: "number",
                            value: p.price_minor / 100,
                            min: 0,
                            required: true,
                          },
                          {
                            name: "stock",
                            label: "Inventario disponible",
                            type: "number",
                            value: p.stock,
                            min: 0,
                            required: true,
                          },
                          {
                            name: "status",
                            label: "Estado de publicación",
                            value: p.status,
                            options: [
                              { value: "active", label: "Activo (Visible en Marketplace)" },
                              { value: "draft", label: "Borrador (Oculto)" },
                              { value: "archived", label: "Archivado" },
                            ],
                          },
                        ]}
                        action={(v) =>
                          api("update_product", {
                            product: p.id,
                            name: v.name,
                            description: v.description,
                            price: Math.round(v.amount * 100),
                            stock: v.stock,
                            status: v.status,
                          })
                        }
                        after={data.reload}
                      />

                      <Action
                        title="Subir foto"
                        className="button secondary small"
                        fields={[
                          {
                            name: "file",
                            label: "Seleccionar imagen",
                            options: (files.data ?? [])
                              .filter((f) => f.purpose === "product")
                              .map((f) => ({
                                value: f.id,
                                label: `Imagen · ${date(f.created_at)}`,
                              })),
                          },
                        ]}
                        action={(v) =>
                          api("add_product_image", { product: p.id, ...v })
                        }
                        after={data.reload}
                      />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <Empty
                title="Aún no has agregado productos a tu tienda"
                text="Comienza a publicar los productos y artículos que deseas comercializar."
              />
            )}
          </Section>
        </>
      )}
    </div>
  );
}
