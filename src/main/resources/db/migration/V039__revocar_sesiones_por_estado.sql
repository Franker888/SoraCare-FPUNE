ALTER TABLE soracare.usuarios
    ADD COLUMN version_sesion BIGINT NOT NULL DEFAULT 0;

-- El cambio de estado revoca todas las copias de sesión, también entre instancias.
-- Reactivar una cuenta nunca vuelve a validar una sesión anterior.
CREATE FUNCTION soracare.revocar_sesiones_por_estado() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado IS DISTINCT FROM OLD.estado THEN
        NEW.version_sesion := OLD.version_sesion + 1;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER usuarios_revocar_sesiones_por_estado
BEFORE UPDATE OF estado ON soracare.usuarios
FOR EACH ROW EXECUTE FUNCTION soracare.revocar_sesiones_por_estado();
