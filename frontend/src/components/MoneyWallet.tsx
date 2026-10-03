import { useMemo, useState } from "react";
import {
  ArrowDownLeft,
  ArrowUpRight,
  ArrowRight,
  Banknote,
  CheckCircle2,
  Clock,
  Landmark,
  Minus,
  Plus,
  ShieldCheck,
  Wallet,
  AlertTriangle,
  LoaderCircle,
} from "lucide-react";
import { api, request, money, date, newKey, type Data } from "../api";
import { useToast } from "../context";
import { useData, Modal, Badge, Empty, Loading } from "../components";
import { AddPayoutMethodModal } from "../AddPayoutMethodModal";
import { VinkoIconCoin } from "../VinkoIcons";

type Currency = "VES" | "USD";
const CURRENCIES: { code: Currency; label: string; symbol: string }[] = [
  { code: "VES", label: "Bolívares", symbol: "Bs" },
  { code: "USD", label: "Dólares", symbol: "US$" },
];
const QUICK_VINKOS = [5, 10, 25, 50];
const toMinor = (major: string) => Math.round(Number(major.replace(/\./g, "").replace(",", ".")) * 100);
const BEFORE_ALL = 9_000_000_000_000_000;

// Texto claro para cada movimiento de dinero (solo cuenta disponible).
function movementLabel(e: Data) {
  const credit = e.amount_minor > 0;
  switch (e.kind) {
    case "topup":
      return credit ? "Recarga de saldo" : "Compra de Vinkos";
    case "vinko_purchase":
      return "Compra de Vinkos";
    case "contract_funded":
      return credit ? "Devolución de custodia" : "Pago reservado en custodia";
    case "contract_released":
      return credit ? "Pago recibido por un servicio" : "Pago liberado al profesional";
    case "contract_refunded":
    case "escrow_refund":
      return "Reembolso de servicio";
    default:
      return e.kind?.startsWith("withdrawal")
        ? credit
          ? "Retiro devuelto"
          : "Retiro solicitado"
        : "Movimiento";
  }
}

export function MoneyWallet({ onVinkosChanged, vinkoBalance }: { onVinkosChanged: () => void; vinkoBalance: number }) {
  const toast = useToast();
  const config = useData(() => request("/api/v1/config"), []);
  const sandbox = config.data?.payment_mode === "sandbox";
  const ves = useData(() => api("money_wallet", { currency: "VES" }), []);
  const usd = useData(() => api("money_wallet", { currency: "USD" }), []);
  const rates = useData(() => api<Record<string, Data>>("get_vinko_exchange_rates"), []);
  const topups = useData(() => api<Data[]>("my_topups"), []);
  const [historyCurrency, setHistoryCurrency] = useState<Currency>("VES");
  const history = useData(
    () => api<Data[]>("money_history", { currency: historyCurrency, limit: 30, before: BEFORE_ALL }),
    [historyCurrency],
  );

  const wallets: Record<Currency, Data | null> = { VES: ves.data, USD: usd.data };
  const reloadMoney = () => {
    ves.reload();
    usd.reload();
    topups.reload();
    history.reload();
  };

  // Conversor
  const [currency, setCurrency] = useState<Currency>("VES");
  const [vinkos, setVinkos] = useState(10);
  const [confirming, setConfirming] = useState<{ key: string } | null>(null);
  const [converting, setConverting] = useState(false);
  const [done, setDone] = useState<Data | null>(null);
  const [topupFor, setTopupFor] = useState<{ currency: Currency; amountMinor?: number } | null>(null);

  const rate: number | undefined = rates.data?.[currency]?.rate_minor;
  const available: number = wallets[currency]?.available_minor ?? 0;
  const cost = rate ? vinkos * rate : 0;
  const maxVinkos = rate ? Math.floor(available / rate) : 0;
  const missing = Math.max(cost - available, 0);
  const validAmount = Number.isInteger(vinkos) && vinkos >= 1 && vinkos <= 100000;

  const convert = async () => {
    if (!confirming) return;
    setConverting(true);
    try {
      const result = await api("convert_money_to_vinkos", { currency, vinkos, key: confirming.key });
      setDone({ ...result, vinkos });
      onVinkosChanged();
      reloadMoney();
    } catch (e) {
      toast((e as Error).message, true);
    } finally {
      setConverting(false);
    }
  };

  return (
    <>
      {/* Saldos en dinero */}
      <section className="mw-balances" aria-label="Saldo en dinero">
        {CURRENCIES.map((c) => {
          const w = wallets[c.code];
          return (
            <div className="mw-balance" key={c.code}>
              <div className="mw-balance-head">
                <span className="mw-balance-icon">
                  <Wallet size={18} />
                </span>
                <span>Saldo en {c.label.toLowerCase()}</span>
              </div>
              {w ? <strong>{money(w.available_minor ?? 0, c.code)}</strong> : <span className="dash-skeleton" />}
              <div className="mw-balance-meta">
                <span>
                  <ShieldCheck size={13} /> {money(w?.reserved_for_services_minor ?? 0, c.code)} en custodia
                </span>
                {(w?.withdrawal_pending_minor ?? 0) > 0 && (
                  <span>
                    <Clock size={13} /> {money(w?.withdrawal_pending_minor, c.code)} en retiro
                  </span>
                )}
              </div>
              <button className="button secondary small" onClick={() => setTopupFor({ currency: c.code })}>
                <Plus size={15} /> Recargar
              </button>
            </div>
          );
        })}
      </section>

      {/* Conversor de saldo a Vinkos */}
      <section id="comprar-vinkos" className="mw-convert" aria-label="Comprar Vinkos con tu saldo">
        <div className="mw-convert-intro">
          <span className="mw-convert-badge">
            <VinkoIconCoin size={22} strokeWidth={2.4} />
          </span>
          <div>
            <h2>Compra Vinkos con tu saldo</h2>
            <p>Conversión inmediata, sin comisiones. Los Vinkos se acreditan al instante.</p>
          </div>
        </div>

        <div className="mw-convert-grid">
          <div className="mw-convert-form">
            <div className="mw-field">
              <span className="mw-label">Pagar con</span>
              <div className="dash-tabs mw-currency" role="tablist" aria-label="Moneda de pago">
                {CURRENCIES.map((c) => (
                  <button
                    key={c.code}
                    role="tab"
                    aria-selected={currency === c.code}
                    className={currency === c.code ? "active" : ""}
                    disabled={!rates.data?.[c.code]}
                    onClick={() => setCurrency(c.code)}
                  >
                    {c.symbol} · {c.label}
                  </button>
                ))}
              </div>
              <span className="mw-hint">
                Disponible: <strong>{money(available, currency)}</strong>
              </span>
            </div>

            <div className="mw-field">
              <label className="mw-label" htmlFor="mw-vinkos">
                ¿Cuántos Vinkos quieres?
              </label>
              <div className="mw-stepper">
                <button type="button" aria-label="Restar un Vinko" onClick={() => setVinkos((v) => Math.max(1, v - 1))}>
                  <Minus size={18} />
                </button>
                <input
                  id="mw-vinkos"
                  inputMode="numeric"
                  value={Number.isFinite(vinkos) ? vinkos : ""}
                  onChange={(e) => setVinkos(Number(e.target.value.replace(/\D/g, "")) || 0)}
                />
                <button type="button" aria-label="Sumar un Vinko" onClick={() => setVinkos((v) => Math.min(100000, v + 1))}>
                  <Plus size={18} />
                </button>
              </div>
              <div className="mw-chips">
                {QUICK_VINKOS.map((n) => (
                  <button key={n} type="button" className={vinkos === n ? "active" : ""} onClick={() => setVinkos(n)}>
                    {n}
                  </button>
                ))}
                <button
                  type="button"
                  disabled={maxVinkos < 1}
                  className={vinkos === maxVinkos && maxVinkos > 0 ? "active" : ""}
                  onClick={() => setVinkos(maxVinkos)}
                >
                  Máximo ({maxVinkos})
                </button>
              </div>
            </div>
          </div>

          <div className="mw-summary">
            <div className="mw-rate">
              {rate ? (
                <>
                  1 Vinko = <strong>{money(rate, currency)}</strong>
                  {rates.data?.[currency]?.created_at && <span> · Tasa del {date(rates.data[currency].created_at)}</span>}
                </>
              ) : rates.loading ? (
                "Cargando tasa…"
              ) : (
                "Tasa no disponible para esta moneda"
              )}
            </div>
            <dl>
              <div>
                <dt>Recibes</dt>
                <dd>{validAmount ? vinkos : 0} Vinkos</dd>
              </div>
              <div>
                <dt>Comisión</dt>
                <dd className="mw-free">Gratis</dd>
              </div>
              <div className="mw-total">
                <dt>Total a pagar</dt>
                <dd>{money(cost, currency)}</dd>
              </div>
              <div>
                <dt>Saldo después</dt>
                <dd>{money(Math.max(available - cost, 0), currency)}</dd>
              </div>
              <div>
                <dt>Vinkos después</dt>
                <dd>{vinkoBalance + (validAmount ? vinkos : 0)} Vinkos</dd>
              </div>
            </dl>

            {missing > 0 && validAmount && rate ? (
              <div className="mw-warning">
                <AlertTriangle size={16} />
                <span>
                  Te faltan <strong>{money(missing, currency)}</strong> para esta compra.
                </span>
                <button className="link-button" onClick={() => setTopupFor({ currency, amountMinor: missing })}>
                  Recargar ahora
                </button>
              </div>
            ) : null}

            <button
              className="button primary mw-buy"
              disabled={!rate || !validAmount || missing > 0}
              onClick={() => {
                setDone(null);
                setConfirming({ key: newKey() });
              }}
            >
              Comprar {validAmount ? vinkos : 0} Vinkos <ArrowRight size={17} />
            </button>
          </div>
        </div>
      </section>

      {/* Recargas pendientes */}
      {(topups.data ?? []).some((t) => t.status !== "settled") && (
        <section className="dash-panel mw-pending">
          <div className="dash-panel-head">
            <h2>Recargas en proceso</h2>
          </div>
          <ul className="mw-list">
            {(topups.data ?? [])
              .filter((t) => t.status !== "settled")
              .map((t) => (
                <li key={t.id}>
                  <span className="mw-move-icon in">
                    <Clock size={16} />
                  </span>
                  <div>
                    <strong>Recarga de {money(t.amount_minor, t.currency)}</strong>
                    <span>{date(t.created_at)} · {sandbox ? "Pago de prueba sin confirmar" : "Esperando confirmación del pago"}</span>
                  </div>
                  {sandbox ? (
                    <SettleTopup id={t.id} after={reloadMoney} />
                  ) : (
                    <Badge value={t.status} />
                  )}
                </li>
              ))}
          </ul>
        </section>
      )}

      {/* Movimientos de dinero */}
      <section className="dash-panel">
        <div className="dash-panel-head">
          <h2>Movimientos de dinero</h2>
          <div className="dash-tabs" role="tablist" aria-label="Moneda del historial">
            {CURRENCIES.map((c) => (
              <button
                key={c.code}
                role="tab"
                aria-selected={historyCurrency === c.code}
                className={historyCurrency === c.code ? "active" : ""}
                onClick={() => setHistoryCurrency(c.code)}
              >
                {c.symbol}
              </button>
            ))}
          </div>
        </div>
        {history.loading && !history.data ? (
          <Loading />
        ) : (history.data ?? []).filter((e) => e.account_kind === "available").length ? (
          <ul className="mw-list">
            {(history.data ?? [])
              .filter((e) => e.account_kind === "available")
              .map((e) => (
                <li key={e.entry_id}>
                  <span className={`mw-move-icon ${e.amount_minor > 0 ? "in" : "out"}`}>
                    {e.amount_minor > 0 ? <ArrowDownLeft size={16} /> : <ArrowUpRight size={16} />}
                  </span>
                  <div>
                    <strong>{movementLabel(e)}</strong>
                    <span>{date(e.created_at)} · Saldo: {money(e.balance_after_minor, e.currency)}</span>
                  </div>
                  <span className={`mw-amount ${e.amount_minor > 0 ? "in" : "out"}`}>
                    {e.amount_minor > 0 ? "+" : "−"}
                    {money(Math.abs(e.amount_minor), e.currency)}
                  </span>
                </li>
              ))}
          </ul>
        ) : (
          <Empty title="Sin movimientos todavía" text="Tus recargas, pagos y compras de Vinkos aparecerán aquí." />
        )}
      </section>

      <Withdrawals wallets={wallets} onChanged={reloadMoney} />

      {confirming && (
        <Modal title={done ? "¡Compra realizada!" : "Confirma tu compra"} onClose={() => setConfirming(null)}>
          {done ? (
            <div className="mw-done">
              <CheckCircle2 size={44} />
              <h3>+{done.vinkos} Vinkos</h3>
              <p>
                Pagaste {money(done.debited_minor, currency)}. Ahora tienes <strong>{done.credits} Vinkos</strong> disponibles.
              </p>
              <button className="button primary" onClick={() => setConfirming(null)}>
                Listo
              </button>
            </div>
          ) : (
            <div className="mw-confirm">
              <div className="mw-confirm-amount">
                <VinkoIconCoin size={30} strokeWidth={2.4} />
                <strong>{vinkos} Vinkos</strong>
              </div>
              <dl>
                <div>
                  <dt>Se descontará de tu saldo</dt>
                  <dd>{money(cost, currency)}</dd>
                </div>
                <div>
                  <dt>Tasa aplicada</dt>
                  <dd>1 Vinko = {money(rate ?? 0, currency)}</dd>
                </div>
                <div>
                  <dt>Comisión</dt>
                  <dd className="mw-free">Gratis</dd>
                </div>
              </dl>
              <p className="mw-hint">Los Vinkos no son reembolsables a dinero. Se usan para contrataciones y beneficios en SoyVinko.</p>
              <div className="mw-actions">
                <button className="button secondary" onClick={() => setConfirming(null)} disabled={converting}>
                  Cancelar
                </button>
                <button className="button primary" onClick={convert} disabled={converting}>
                  {converting ? <LoaderCircle size={16} className="spin" /> : <>Confirmar compra</>}
                </button>
              </div>
            </div>
          )}
        </Modal>
      )}

      {topupFor && (
        <TopupModal
          currency={topupFor.currency}
          suggestedMinor={topupFor.amountMinor}
          sandbox={sandbox}
          onClose={() => setTopupFor(null)}
          onDone={reloadMoney}
        />
      )}
    </>
  );
}

function SettleTopup({ id, after }: { id: string; after: () => void }) {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  return (
    <button
      className="button secondary small"
      disabled={busy}
      onClick={async () => {
        setBusy(true);
        try {
          await request(`/api/v1/integrations/sandbox/topup/${id}/settle`, "POST");
          toast("Recarga acreditada (modo de prueba).");
          after();
        } catch (e) {
          toast((e as Error).message, true);
        } finally {
          setBusy(false);
        }
      }}
    >
      {busy ? <LoaderCircle size={14} className="spin" /> : "Simular pago"}
    </button>
  );
}

function TopupModal({
  currency,
  suggestedMinor,
  sandbox,
  onClose,
  onDone,
}: {
  currency: Currency;
  suggestedMinor?: number;
  sandbox: boolean;
  onClose: () => void;
  onDone: () => void;
}) {
  const toast = useToast();
  const symbol = currency === "VES" ? "Bs" : "US$";
  const quick = currency === "VES" ? [5000, 10000, 25000, 50000] : [10, 25, 50, 100];
  const [amount, setAmount] = useState(suggestedMinor ? String(Math.ceil(suggestedMinor / 100)) : "");
  const [key] = useState(newKey);
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState(false);
  const minor = toMinor(amount || "0");
  const valid = Number.isFinite(minor) && minor >= 100;

  const submit = async () => {
    setBusy(true);
    try {
      const id = await api<string>("create_topup", { currency, amount: minor, key });
      if (sandbox) {
        await request(`/api/v1/integrations/sandbox/topup/${id}/settle`, "POST");
        toast(`Recarga de ${money(minor, currency)} acreditada (modo de prueba).`);
        onDone();
        onClose();
      } else {
        setPending(true);
        onDone();
      }
    } catch (e) {
      toast((e as Error).message, true);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal title={`Recargar saldo en ${currency === "VES" ? "bolívares" : "dólares"}`} onClose={onClose}>
      {pending ? (
        <div className="mw-done">
          <Clock size={40} />
          <h3>Recarga registrada</h3>
          <p>
            Tu recarga de {money(minor, currency)} quedó pendiente. La acreditaremos en cuanto se confirme el pago.
          </p>
          <button className="button primary" onClick={onClose}>
            Entendido
          </button>
        </div>
      ) : (
        <div className="mw-confirm">
          <label className="mw-label" htmlFor="mw-topup">
            Monto a recargar
          </label>
          <div className="mw-money-input">
            <span>{symbol}</span>
            <input
              id="mw-topup"
              inputMode="decimal"
              placeholder="0,00"
              value={amount}
              onChange={(e) => setAmount(e.target.value.replace(/[^\d.,]/g, ""))}
            />
          </div>
          <div className="mw-chips">
            {quick.map((n) => (
              <button key={n} type="button" className={minor === n * 100 ? "active" : ""} onClick={() => setAmount(String(n))}>
                {symbol} {n.toLocaleString("es-VE")}
              </button>
            ))}
          </div>
          {sandbox && (
            <p className="mw-hint">
              <Banknote size={14} /> Portal de demostración: la recarga se simula y no mueve dinero real.
            </p>
          )}
          <div className="mw-actions">
            <button className="button secondary" onClick={onClose} disabled={busy}>
              Cancelar
            </button>
            <button className="button primary" onClick={submit} disabled={!valid || busy}>
              {busy ? <LoaderCircle size={16} className="spin" /> : <>Recargar {valid ? money(minor, currency) : ""}</>}
            </button>
          </div>
        </div>
      )}
    </Modal>
  );
}

function Withdrawals({ wallets, onChanged }: { wallets: Record<Currency, Data | null>; onChanged: () => void }) {
  const toast = useToast();
  const methods = useData(() => api<Data[]>("my_payout_methods"), []);
  const withdrawals = useData(() => api<Data[]>("my_withdrawals"), []);
  const [adding, setAdding] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);
  const active = (methods.data ?? []).filter((m) => m.status !== "disabled");
  const refresh = () => {
    methods.reload();
    withdrawals.reload();
    onChanged();
  };

  return (
    <section className="dash-panel">
      <div className="dash-panel-head">
        <h2>Retiros</h2>
        <div className="mw-head-actions">
          <button className="button secondary small" onClick={() => setAdding(true)}>
            <Landmark size={15} /> Añadir cuenta
          </button>
          <button
            className="button primary small"
            disabled={!active.some((m) => m.status === "verified")}
            onClick={() => setWithdrawing(true)}
          >
            Retirar saldo
          </button>
        </div>
      </div>
      <p className="mw-hint">La única comisión de SoyVinko se cobra al retirar. Verás el monto exacto antes de confirmar.</p>

      {methods.loading && !methods.data ? (
        <Loading />
      ) : active.length ? (
        <ul className="mw-methods">
          {active.map((m) => (
            <li key={m.id}>
              <Landmark size={18} />
              <div>
                <strong>{m.label}</strong>
                <span>
                  {m.masked_account} · {m.currency}
                </span>
              </div>
              <Badge value={m.status} />
            </li>
          ))}
        </ul>
      ) : (
        <p className="dash-muted">Añade una cuenta bancaria o de Pago Móvil a tu nombre para poder retirar.</p>
      )}

      {(withdrawals.data ?? []).length > 0 && (
        <ul className="mw-list mw-withdrawals">
          {(withdrawals.data ?? []).slice(0, 5).map((w) => (
            <li key={w.id}>
              <span className="mw-move-icon out">
                <ArrowUpRight size={16} />
              </span>
              <div>
                <strong>Retiro de {money(w.amount_minor, w.currency)}</strong>
                <span>
                  {date(w.created_at)} · Recibes {money(w.net_minor, w.currency)} · {w.destination_label}
                </span>
              </div>
              <Badge value={w.status} />
              {w.status === "pending" && (
                <button
                  className="link-button"
                  onClick={async () => {
                    try {
                      await api("cancel_withdrawal", { withdrawal: w.id });
                      toast("Retiro cancelado; el saldo volvió a tu cuenta.");
                      refresh();
                    } catch (e) {
                      toast((e as Error).message, true);
                    }
                  }}
                >
                  Cancelar
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      {adding && <AddPayoutMethodModal onClose={() => setAdding(false)} onSuccess={refresh} />}
      {withdrawing && (
        <WithdrawModal
          methods={active.filter((m) => m.status === "verified")}
          wallets={wallets}
          onClose={() => setWithdrawing(false)}
          onDone={refresh}
        />
      )}
    </section>
  );
}

function WithdrawModal({
  methods,
  wallets,
  onClose,
  onDone,
}: {
  methods: Data[];
  wallets: Record<Currency, Data | null>;
  onClose: () => void;
  onDone: () => void;
}) {
  const toast = useToast();
  const [method, setMethod] = useState<string>(methods[0]?.id ?? "");
  const selected = methods.find((m) => m.id === method);
  const currency = (selected?.currency ?? "VES") as Currency;
  const available = wallets[currency]?.available_minor ?? 0;
  const [amount, setAmount] = useState("");
  const [quote, setQuote] = useState<Data | null>(null);
  const [key] = useState(newKey);
  const [busy, setBusy] = useState(false);
  const minor = useMemo(() => toMinor(amount || "0"), [amount]);

  const run = async (fn: () => Promise<void>) => {
    setBusy(true);
    try {
      await fn();
    } catch (e) {
      toast((e as Error).message, true);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal title="Retirar saldo" onClose={onClose}>
      <div className="mw-confirm">
        <label className="mw-label" htmlFor="mw-method">
          Cuenta de destino
        </label>
        <select
          id="mw-method"
          value={method}
          onChange={(e) => {
            setMethod(e.target.value);
            setQuote(null);
          }}
        >
          {methods.map((m) => (
            <option key={m.id} value={m.id}>
              {m.label} · {m.masked_account}
            </option>
          ))}
        </select>
        <label className="mw-label" htmlFor="mw-amount">
          Monto · disponible {money(available, currency)}
        </label>
        <div className="mw-money-input">
          <span>{currency === "VES" ? "Bs" : "US$"}</span>
          <input
            id="mw-amount"
            inputMode="decimal"
            placeholder="0,00"
            value={amount}
            onChange={(e) => {
              setAmount(e.target.value.replace(/[^\d.,]/g, ""));
              setQuote(null);
            }}
          />
          <button type="button" className="link-button" onClick={() => setAmount(String(available / 100).replace(".", ","))}>
            Todo
          </button>
        </div>

        {quote && (
          <dl>
            <div>
              <dt>Monto</dt>
              <dd>{money(quote.amount_minor, currency)}</dd>
            </div>
            <div>
              <dt>Comisión de retiro ({(quote.basis_points / 100).toLocaleString("es-VE")}%)</dt>
              <dd>−{money(quote.fee_minor, currency)}</dd>
            </div>
            <div className="mw-total">
              <dt>Recibirás</dt>
              <dd>{money(quote.net_minor, currency)}</dd>
            </div>
          </dl>
        )}
        {quote && <p className="mw-hint">Cotización válida hasta las {new Date(quote.expires_at).toLocaleTimeString("es-VE", { hour: "2-digit", minute: "2-digit" })}.</p>}

        <div className="mw-actions">
          <button className="button secondary" onClick={onClose} disabled={busy}>
            Cancelar
          </button>
          {quote ? (
            <button
              className="button primary"
              disabled={busy}
              onClick={() =>
                run(async () => {
                  await api("request_withdrawal", { quote: quote.id, key });
                  toast("Retiro solicitado. Te avisaremos cuando se procese.");
                  onDone();
                  onClose();
                })
              }
            >
              {busy ? <LoaderCircle size={16} className="spin" /> : "Confirmar retiro"}
            </button>
          ) : (
            <button
              className="button primary"
              disabled={busy || !method || !(minor > 0) || minor > available}
              onClick={() => run(async () => setQuote(await api("quote_withdrawal", { currency, method, amount: minor })))}
            >
              {busy ? <LoaderCircle size={16} className="spin" /> : "Ver cuánto recibo"}
            </button>
          )}
        </div>
      </div>
    </Modal>
  );
}
