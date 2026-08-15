package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.Medicamento;
import py.fpune.soracare.mvc.model.ViaAdministracion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MedicamentoDAO {

    private static final String COLUMNAS = """
            id_medicamento, nombre_comercial, principio_activo, presentacion,
            concentracion, via_administracion, requiere_receta, activo,
            fecha_creacion, fecha_actualizacion
            """;

    public Medicamento crear(Connection conexion, Medicamento medicamento)
            throws SQLException {
        String sql = """
                INSERT INTO soracare.medicamentos (
                    nombre_comercial, principio_activo, presentacion, concentracion,
                    via_administracion, requiere_receta, activo
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING
                """ + COLUMNAS;
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, medicamento.getNombreComercial());
            sentencia.setString(2, medicamento.getPrincipioActivo());
            sentencia.setString(3, medicamento.getPresentacion());
            sentencia.setString(4, medicamento.getConcentracion());
            sentencia.setString(5, medicamento.getViaAdministracion().name());
            sentencia.setBoolean(6, medicamento.isRequiereReceta());
            sentencia.setBoolean(7, medicamento.isActivo());
            try (ResultSet resultado = sentencia.executeQuery()) {
                if (!resultado.next()) {
                    throw new SQLException("No fue posible registrar el medicamento.");
                }
                return mapear(resultado);
            }
        }
    }

    public List<Medicamento> listarTodos() throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            return listarTodos(conexion);
        }
    }

    public List<Medicamento> listarTodos(Connection conexion) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM soracare.medicamentos "
                + "ORDER BY activo DESC, nombre_comercial, principio_activo";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            return mapearLista(resultado);
        }
    }

    public List<Medicamento> listarActivos() throws SQLException {
        try (Connection conexion = ConexionBD.obtenerConexion()) {
            String sql = "SELECT " + COLUMNAS + " FROM soracare.medicamentos "
                    + "WHERE activo = TRUE ORDER BY nombre_comercial, principio_activo";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql);
                 ResultSet resultado = sentencia.executeQuery()) {
                return mapearLista(resultado);
            }
        }
    }

    public Optional<Medicamento> buscarPorId(Connection conexion, long idMedicamento)
            throws SQLException {
        String sql = "SELECT " + COLUMNAS
                + " FROM soracare.medicamentos WHERE id_medicamento = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setLong(1, idMedicamento);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        }
    }

    public void cambiarEstado(Connection conexion, long idMedicamento, boolean activo)
            throws SQLException {
        String sql = "UPDATE soracare.medicamentos SET activo = ? WHERE id_medicamento = ?";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setBoolean(1, activo);
            sentencia.setLong(2, idMedicamento);
            if (sentencia.executeUpdate() != 1) {
                throw new IllegalArgumentException("El medicamento seleccionado no existe.");
            }
        }
    }

    private static List<Medicamento> mapearLista(ResultSet resultado) throws SQLException {
        List<Medicamento> medicamentos = new ArrayList<>();
        while (resultado.next()) medicamentos.add(mapear(resultado));
        return medicamentos;
    }

    private static Medicamento mapear(ResultSet resultado) throws SQLException {
        Medicamento medicamento = new Medicamento();
        medicamento.setIdMedicamento(resultado.getLong("id_medicamento"));
        medicamento.setNombreComercial(resultado.getString("nombre_comercial"));
        medicamento.setPrincipioActivo(resultado.getString("principio_activo"));
        medicamento.setPresentacion(resultado.getString("presentacion"));
        medicamento.setConcentracion(resultado.getString("concentracion"));
        medicamento.setViaAdministracion(ViaAdministracion.valueOf(
                resultado.getString("via_administracion")));
        medicamento.setRequiereReceta(resultado.getBoolean("requiere_receta"));
        medicamento.setActivo(resultado.getBoolean("activo"));
        medicamento.setFechaCreacion(resultado.getObject(
                "fecha_creacion", OffsetDateTime.class));
        medicamento.setFechaActualizacion(resultado.getObject(
                "fecha_actualizacion", OffsetDateTime.class));
        return medicamento;
    }
}
