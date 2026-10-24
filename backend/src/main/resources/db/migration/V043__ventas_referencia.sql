-- V043 · Ventas: referencia opaca de la venta (R-V1 de docs/diseno/restaurantes.md, CT-07 a CT-10).
-- Quien cobra (por ejemplo la cuenta de un restaurante) manda un texto opaco al registrar la venta y lo recibe de vuelta en
-- venta.registrada para reconocerla. Es opcional, de 1 a 80 caracteres seguros, y una venta cobrada no se edita: la referencia
-- queda con las demás columnas que el disparador protege.

ALTER TABLE ventas.venta
    ADD COLUMN referencia TEXT CONSTRAINT ck_venta_referencia CHECK (referencia ~ '^[A-Za-z0-9._:-]{1,80}$');

CREATE OR REPLACE FUNCTION ventas.solo_se_anula() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    IF (NEW.id, NEW.negocio_id, NEW.serie, NEW.folio, NEW.turno_id, NEW.cliente_id, NEW.cupon_id, NEW.subtotal,
        NEW.descuento, NEW.total, NEW.ocurrido_en, NEW.registrada_por, NEW.referencia)
        IS DISTINCT FROM
       (OLD.id, OLD.negocio_id, OLD.serie, OLD.folio, OLD.turno_id, OLD.cliente_id, OLD.cupon_id, OLD.subtotal,
        OLD.descuento, OLD.total, OLD.ocurrido_en, OLD.registrada_por, OLD.referencia)
       OR (OLD.estado = 'ANULADA' AND NEW.estado <> 'ANULADA') THEN
        RAISE EXCEPTION 'Una venta cobrada solo se anula: no se edita'
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END
$$;
