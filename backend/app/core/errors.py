import logging
import re
from fastapi import Request, HTTPException
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from psycopg import Error as DatabaseError, OperationalError
from psycopg_pool import PoolTimeout

logger = logging.getLogger("chamba")
MESSAGES = {
    "professional_slug_taken": "Ese nombre de perfil ya está ocupado. Prueba otro nombre o añade una palabra o un número.",
    "invalid_professional_slug": "Escribe un nombre de perfil con letras o números, por ejemplo: develope-academy.",
    "offering_unavailable": "Este servicio ya no está disponible. Elige otro servicio del profesional.",
    "invalid_request_images": "Adjunta hasta 6 imágenes propias JPG, PNG o WEBP. Revisa las fotos seleccionadas.",
    "invalid_request_details": "Describe lo que necesitas con al menos 10 caracteres y revisa los datos de la solicitud.",
    "remote_location_not_allowed": "Los servicios remotos no necesitan dirección ni radio de búsqueda.",
    "professional_outside_coverage": "Este profesional no atiende la dirección seleccionada. Elige otra dirección o busca profesionales en esa zona.",
    "customer_quote_confirmation_required": "Envía una cotización. El cliente debe confirmar el precio para asignarte el servicio.",
    "positive_offer_required": "Escribe un precio mayor que cero en la moneda elegida por el cliente.",
    "request_unavailable": "Esta solicitud ya no recibe propuestas: fue asignada, cancelada o venció.",
    "address_not_owned": "Esa dirección no pertenece a tu cuenta.",
    "authentication_required": "Inicia sesión para continuar.",
    "branch_not_owned": "Esa sede no pertenece a tu perfil profesional.",
    "cancel_contract_instead": "Este servicio aún no empezó: cancélalo en lugar de abrir una disputa.",
    "chat_closed": "El chat de este servicio ya está cerrado.",
    "chat_unavailable": "El chat no está disponible para este servicio.",
    "contact_not_owned": "Ese contacto no pertenece a tu cuenta.",
    "contract_access_denied": "No participas en este servicio.",
    "contract_cancelled": "Este servicio fue cancelado.",
    "contract_identity_and_payment_mode_immutable": "Las partes y la forma de pago de un servicio no se pueden cambiar.",
    "contract_not_completed": "Podrás calificar cuando ambas partes confirmen que el servicio terminó.",
    "counterparty_confirmation_required": "Falta que la otra parte confirme.",
    "customer_refund_request_required": "Primero el cliente debe solicitar el reembolso.",
    "customer_release_required": "El cliente debe aprobar la entrega y liberar el pago.",
    "customer_unavailable": "El cliente no está disponible.",
    "delivery_not_allowed": "En este momento no se puede enviar una entrega.",
    "delivery_not_found": "No encontramos esa entrega.",
    "delivery_not_reviewable": "Esa entrega ya fue revisada.",
    "delivery_share_not_allowed": "No puedes compartir datos de entrega en este pedido.",
    "delivery_superseded": "Hay una entrega más reciente; revisa esa.",
    "dispute_ticket_and_reason_required": "Explica el motivo de la disputa.",
    "duplicate_product": "Ya tienes un producto con ese código (SKU).",
    "escrow_already_funded": "El pago de este servicio ya está reservado.",
    "escrow_refund_not_available": "El reembolso no está disponible en el estado actual del servicio.",
    "escrow_refund_required": "Primero hay que resolver el reembolso del pago reservado.",
    "exchange_rate_not_configured": "La tasa de Vinkos para esa moneda no está disponible.",
    "file_access_denied": "No tienes acceso a ese archivo.",
    "file_not_owned": "Ese archivo no es tuyo.",
    "file_unavailable": "El archivo ya no está disponible.",
    "funded_terms_immutable": "Con el pago ya reservado, el precio acordado no se puede cambiar.",
    "funding_quote_not_owned": "Esa cotización de pago no es tuya.",
    "funding_quote_stale": "La cotización cambió. Revisa el monto e inténtalo de nuevo.",
    "funds_not_releasable": "El pago todavía no se puede liberar.",
    "idempotency_conflict": "Ya enviaste esta operación con otros datos. Recarga e inténtalo de nuevo.",
    "idempotency_key_required": "Falta el identificador de la operación. Inténtalo de nuevo.",
    "invalid_account_type": "Tipo de cuenta no válido.",
    "invalid_business_name": "Indica una razón social válida.",
    "invalid_category": "Elige una categoría válida.",
    "invalid_challenge": "El enlace o código no es válido o ya venció.",
    "invalid_coordinates": "La ubicación no es válida. Elige un punto en Venezuela.",
    "invalid_currency": "Moneda no válida. Usa bolívares o dólares.",
    "invalid_delivery_files": "Adjunta archivos válidos para la entrega.",
    "invalid_dispute": "Explica el motivo de la disputa con más detalle.",
    "invalid_feed_filters": "Revisa los filtros de oportunidades.",
    "invalid_message": "Escribe un mensaje (máximo 4.000 caracteres).",
    "invalid_order_items": "Revisa los productos del pedido.",
    "invalid_pagination": "Página no válida.",
    "invalid_payment_report": "Revisa los datos del pago reportado.",
    "invalid_quantity": "Indica una cantidad válida.",
    "invalid_rate": "La calificación debe ser de 1 a 5 estrellas.",
    "invalid_remote_transition": "Esa acción no aplica a un servicio remoto en su estado actual.",
    "invalid_rif": "Indica un RIF válido (ejemplo: J-12345678-9).",
    "invalid_search_filters": "Revisa los filtros de búsqueda.",
    "invalid_session": "Tu sesión no es válida. Inicia sesión de nuevo.",
    "invalid_transition": "Esa acción no aplica al estado actual.",
    "invalid_vinkos_amount": "Indica una cantidad de Vinkos mayor que cero.",
    "location_not_shared": "El cliente todavía no comparte la ubicación del servicio.",
    "location_share_not_allowed": "No se puede compartir la ubicación en el estado actual del servicio.",
    "mixed_currencies": "Todos los productos del pedido deben estar en la misma moneda.",
    "not_target_professional": "Esta solicitud no está dirigida a ti.",
    "note_required": "Escribe una nota.",
    "offering_not_owned": "Ese servicio no pertenece a tu perfil.",
    "only_customer_can_fund": "Solo el cliente puede reservar el pago.",
    "only_customer_can_release": "Solo el cliente puede liberar el pago.",
    "only_customer_can_request_refund": "Solo el cliente puede pedir el reembolso.",
    "only_customer_can_review": "Solo el cliente puede revisar la entrega.",
    "only_customer_can_revoke": "Solo el cliente puede dejar de compartir la ubicación.",
    "only_customer_can_share": "Solo el cliente puede compartir la ubicación.",
    "only_professional_can_advance": "Solo el profesional puede avanzar este paso.",
    "only_professional_can_approve_refund": "Solo el profesional puede aprobar el reembolso.",
    "only_professional_can_deliver": "Solo el profesional puede enviar entregas.",
    "only_professional_can_start": "Solo el profesional puede iniciar el servicio.",
    "open_dispute_instead": "El servicio ya está en curso: abre una disputa si hay un problema.",
    "order_access_denied": "No participas en este pedido.",
    "payment_amount_mismatch": "El monto pagado no coincide con el acordado.",
    "payout_method_disabled": "Esa cuenta de retiro está desactivada.",
    "payout_method_not_owned": "Esa cuenta de retiro no es tuya.",
    "plan_unavailable": "Ese plan ya no está disponible.",
    "platform_payment_required": "Este servicio requiere pago protegido dentro de SoyVinko.",
    "position_not_allowed": "Solo puedes compartir tu posición mientras vas en camino o estás en el servicio.",
    "product_not_owned": "Ese producto no pertenece a tu tienda.",
    "professional_unavailable": "El profesional no está disponible.",
    "proposal_not_owned": "Esa propuesta no es tuya.",
    "proposal_not_pending": "Esa propuesta ya fue respondida.",
    "proposal_unavailable": "La propuesta ya no está disponible.",
    "purchase_not_found": "No encontramos esa compra.",
    "purchase_not_payable": "Esa compra ya no se puede pagar.",
    "reason_required": "Indica el motivo.",
    "refund_not_allowed": "El reembolso no está permitido en este estado.",
    "remote_service_has_no_location": "Los servicios remotos no usan ubicación.",
    "request_not_editable": "Esta solicitud ya no se puede modificar.",
    "request_not_publishable": "Completa la solicitud antes de publicarla.",
    "resource_not_owned": "No tienes acceso a este recurso.",
    "review_not_found": "No encontramos esa reseña.",
    "review_not_owned": "Esa reseña no es sobre tu perfil.",
    "store_access_denied": "No tienes acceso a esta tienda.",
    "store_suspended": "La tienda está suspendida.",
    "store_unavailable": "La tienda no está disponible.",
    "terms_not_allowed": "No se pueden proponer condiciones en este estado.",
    "terms_not_found": "No encontramos esa propuesta de condiciones.",
    "terms_superseded": "Hay una propuesta de condiciones más reciente.",
    "ticket_not_found": "No encontramos ese caso de soporte.",
    "ticket_not_owned": "Ese caso de soporte no es tuyo.",
    "topup_already_paid": "Esa recarga ya fue acreditada.",
    "topup_not_found": "No encontramos esa recarga.",
    "topup_requires_review": "La recarga quedó en revisión; te avisaremos.",
    "unsafe_attachment": "Ese archivo no está permitido.",
    "use_delivery_and_customer_release": "En servicios remotos se finaliza enviando la entrega y con la liberación del cliente.",
    "user_not_found": "No encontramos tu cuenta.",
    "verified_identity_required_for_withdrawal": "Para retirar necesitas tu identidad verificada (KYC aprobado).",
    "wallet_frozen": "Tu wallet está congelada. Escribe a soporte.",
    "wallet_frozen_requires_reconciliation": "Tu wallet está en revisión. Escribe a soporte.",
    "wallet_not_found": "No encontramos tu wallet.",
    "withdrawal_already_processing": "Ese retiro ya se está procesando.",
    "withdrawal_amount_too_small": "El monto es muy pequeño: la comisión sería igual o mayor que lo que recibirías.",
    "withdrawal_not_found": "No encontramos ese retiro.",
    "withdrawal_not_owned": "Ese retiro no es tuyo.",
    "withdrawal_quote_expired": "La cotización venció. Vuelve a calcular cuánto recibes.",
    "withdrawal_quote_not_owned": "Esa cotización no es tuya.",
    "offering_archived": "Ese servicio fue eliminado y ya no se puede modificar.",
    "invalid_order_transition": "El pedido ya está entregado o cancelado, o la acción no aplica a su estado actual.",
    "order_not_found": "No encontramos ese pedido.",
    "rif_locked_by_kyc": "Tu RIF está confirmado por tu verificación de identidad. Para cambiarlo, envía una nueva verificación.",
    "request_already_assigned": "Esta solicitud ya tiene un profesional asignado.",
    "proposal_already_exists": "Ya enviaste una propuesta para esta solicitud.",
    "invalid_proposal_expiry": "La propuesta debe vencer antes que la solicitud. Actualiza la página y vuelve a intentarlo.",
    "venezuela_only": "Actualmente atendemos únicamente direcciones en Venezuela.",
    "invalid_address_hierarchy": "Selecciona un estado, municipio y parroquia que correspondan entre sí.",
    "point_outside_parish": "El marcador está fuera de la parroquia seleccionada. Mueve el marcador o cambia la parroquia.",
    "invalid_address_details": "Escribe un nombre para la dirección y una dirección de al menos 3 caracteres.",
    "unsupported_currency": "Selecciona bolívares (Bs) o dólares (USD).",
    "insufficient_money": "Tu saldo disponible no alcanza para esta operación.",
    "funds_not_guaranteed": "El cliente debe reservar el importe acordado antes de iniciar.",
    "withdrawal_kyc_required": "Completa la verificación de identidad antes de retirar.",
    "verified_payout_method_required": "Selecciona un método de retiro verificado de la misma moneda.",
    "marketplace_entitlement_required": "Activa un plan que incluya marketplace para abrir tu tienda.",
    "already_exists": "Ya existe un registro con estos datos. Revisa la información o inicia sesión.",
    "agreed_positive_price_required": "Ambas partes deben acordar un precio positivo antes de reservar el pago.",
    "pending_contract_terms": "Confirma las condiciones pendientes antes de reservar el pago.",
    "unsafe_delivery_file": "Selecciona archivos propios aprobados para adjuntar a la entrega.",
    "insufficient_stock": "Ya no hay suficientes unidades disponibles para este pedido.",
    "access_reason_required": "Describe el motivo de acceso con al menos 10 caracteres.",
    "service_unavailable": "El servicio está temporalmente ocupado. Intenta nuevamente en unos momentos.",
    "insufficient_credits": "No tienes créditos suficientes para aceptar este servicio.",
    "insufficient_funds": "Tu saldo disponible no alcanza para esta operación.",
    "request_not_approved": "La solicitud necesita aprobación antes de publicarse.",
    "kyc_required": "Completa la verificación de identidad para retirar.",
    "withdrawal_fee_not_configured": "Todavía no hay una tarifa de retiro configurada para esta moneda.",
    "request_not_owned": "No tienes acceso a esta solicitud.",
    "professional_required": "Activa tu perfil profesional para continuar.",
    "guaranteed_funding_required": "El cliente debe reservar el importe antes de iniciar.",
    "payment_provider_unavailable": "El proveedor de pagos aún no está configurado.",
}


def validation_message(error):
    kind = error["type"]
    context = error.get("ctx", {})
    if error["loc"][-1:] == ("slug",) and kind in {"string_too_short", "string_pattern_mismatch"}:
        return MESSAGES["invalid_professional_slug"]
    if error["loc"][-1:] == ("currency",):
        return "Selecciona bolívares (Bs) o dólares (USD)."
    if kind == "string_too_short":
        return f"Escribe al menos {context['min_length']} caracteres."
    if kind == "string_too_long":
        return f"Usa como máximo {context['max_length']} caracteres."
    if kind in {"missing", "string_type"}:
        return "Completa este campo."
    if error["loc"][-1:] == ("email",) and kind == "value_error":
        return "Escribe un correo electrónico válido."
    return error["msg"]


def install_handlers(app):
    def response(request, status, code, message, details=None):
        return JSONResponse(
            status_code=status,
            content={
                "error": {
                    "code": code,
                    "message": message,
                    "request_id": getattr(request.state, "request_id", ""),
                    "details": details,
                }
            },
        )

    @app.exception_handler(HTTPException)
    async def http_error(request: Request, exc):
        message = exc.detail if isinstance(exc.detail, str) else "Solicitud rechazada."
        code = message if re.fullmatch("[a-z][a-z0-9_]{2,90}", message) else f"http_{exc.status_code}"
        return response(request, exc.status_code, code, MESSAGES.get(code, message))

    @app.exception_handler(RequestValidationError)
    async def validation_error(request: Request, exc):
        details = [
            {"field": ".".join(str(x) for x in e["loc"]), "message": validation_message(e)}
            for e in exc.errors()
        ]
        return response(request, 422, "validation_error", "Revisa los campos del formulario.", details)

    @app.exception_handler(DatabaseError)
    async def db_error(request: Request, exc):
        primary = exc.diag.message_primary or ""
        code = primary if re.fullmatch("[a-z][a-z0-9_]{2,90}", primary) else "operation_rejected"
        status = 403 if exc.sqlstate == "42501" else 409
        details = None
        if exc.sqlstate == "23505":
            code = "already_exists"
        if exc.diag.constraint_name in {"professionals_slug_key", "professionals_slug_check"}:
            code = "professional_slug_taken" if exc.sqlstate == "23505" else "invalid_professional_slug"
            details = [{"field": "body.slug", "message": MESSAGES[code]}]
        # PoolTimeout y caídas de conexión no traen sqlstate: no son rechazos de negocio.
        if isinstance(exc, PoolTimeout) or (isinstance(exc, OperationalError) and not exc.sqlstate):
            status, code = 503, "service_unavailable"
        elif exc.sqlstate and exc.sqlstate.startswith(("08", "53", "57")):
            status, code = 503, "service_unavailable"
        logger.warning(
            "db rejection code=%s state=%s request=%s", code, exc.sqlstate, request.state.request_id
        )
        return response(
            request,
            status,
            code,
            MESSAGES.get(code, "La operación no puede completarse en su estado actual."),
            details,
        )

    @app.exception_handler(Exception)
    async def unexpected(request: Request, exc):
        logger.exception("Unhandled request=%s", request.state.request_id)
        return response(
            request,
            500,
            "internal_error",
            "No pudimos completar la operación. Conserva el identificador de la solicitud.",
        )
