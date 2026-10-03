import { useState } from "react";
import { MessageCircle, ArrowRight, LifeBuoy } from "lucide-react";
import { api, date, type Data } from "../api";
import {
  useData,
  PageTitle,
  Section,
  Empty,
  Badge,
  Action,
  FieldsForm,
  ErrorBox,
} from "../components";
export default function Support() {
  const data = useData(() => api<Data[]>("my_tickets"), [], 10000),
    [selected, setSelected] = useState("");
  const thread = useData(
    () =>
      selected
        ? api("ticket_thread", { ticket: selected })
        : Promise.resolve(null),
    [selected],
    selected ? 10000 : 0,
  );
  return (
    <>
      <PageTitle
        eyebrow="ESTAMOS PARA AYUDARTE"
        title="Centro de soporte"
        description="Cuéntanos qué pasó. Cada caso tiene seguimiento y conversación propia."
      />
      <div className="two-col">
        <Section
          title="Mis casos"
          action={
            <Action
              title="Crear caso"
              fields={[
                {
                  name: "category",
                  label: "Tema",
                  options: [
                    ["service", "Un servicio"],
                    ["credits", "Créditos o pagos"],
                    ["kyc", "Verificación"],
                    ["marketplace", "Una compra"],
                    ["account", "Mi cuenta"],
                    ["abuse", "Reportar abuso"],
                    ["other", "Otro tema"],
                  ].map(([value, label]) => ({ value, label })),
                },
                { name: "subject", label: "Asunto" },
                {
                  name: "description",
                  label: "¿Cómo podemos ayudarte?",
                  type: "textarea",
                },
              ]}
              action={async (v) => {
                const id = await api("open_ticket", v);
                setSelected(id);
              }}
              after={data.reload}
            />
          }
        >
          {data.error && <ErrorBox message={data.error} />}{" "}
          {data.data?.length ? (
            data.data.map((t) => (
              <button
                className={`ticket-option ${selected === t.id ? "selected" : ""}`}
                key={t.id}
                onClick={() => setSelected(t.id)}
              >
                <div>
                  <h3>{t.subject}</h3>
                  <p>{date(t.created_at)}</p>
                </div>
                <Badge value={t.status} />
              </button>
            ))
          ) : (
            <Empty
              title="Estamos a un mensaje"
              text="Abre un caso cuando necesites ayuda con tu cuenta o tus servicios."
            />
          )}
        </Section>
        <Section title={thread.data?.subject ?? "Conversación del caso"}>
          {thread.error ? (
            <ErrorBox message={thread.error} />
          ) : thread.data ? (
            <>
              <p>{thread.data.description}</p>
              <div className="support-messages">
                {thread.data.messages?.map((m: Data) => (
                  <div key={m.id} className="support-message">
                    <p>{m.body}</p>
                    <small>{date(m.created_at)}</small>
                  </div>
                ))}
              </div>
              <FieldsForm
                fields={[
                  { name: "body", label: "Tu mensaje", type: "textarea" },
                ]}
                submit="Enviar respuesta"
                onSubmit={async (v) => {
                  await api("reply_ticket", { ticket: selected, ...v });
                  thread.reload();
                }}
              />
            </>
          ) : (
            <Empty
              title="Selecciona un caso"
              text="Aquí podrás leer y responder a las novedades de soporte."
            />
          )}
        </Section>
      </div>
    </>
  );
}
