package py.fpune.soracare.dao;

import py.fpune.soracare.config.ConexionBD;
import py.fpune.soracare.mvc.model.EstadoCita;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EstadoCitaDAO {

    private static final String COLUMNAS =
            "id_estado_cita, codigo, nombre, descripcion, es_final, activo";

    public Optional<EstadoCita> buscarPorCodigo(Connection conexion, String codigo)
            throws SQLException {
        String sql = "SELECT " + COLUMNAS
                + " FROM soracare.estados_citas WHERE codigo = ? AND activo = TRUE";
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            sentencia.setString(1, codigo);
            try (ResultSet resultado = sentencia.executeQuery()) {
                return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
            }
        }
    }

    public List<EstadoCita> listarActivos() throws SQLException {
        String sql = "SELECT " + COLUMNAS
                + " FROM soracare.estados_citas WHERE activo = TRUE ORDER BY id_estado_cita";
        List<EstadoCita> estados = new ArrayList<>();
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {
            while (resultado.next()) estados.add(mapear(resultado));
        }
        return estados;
    }

    private static EstadoCita mapear(ResultSet resultado) throws SQLException {
        EstadoCita estado = new EstadoCita();
        estado.setIdEstadoCita(resultado.getInt("id_estado_cita"));
        estado.setCodigo(resultado.getString("codigo"));
        estado.setNombre(resultado.getString("nombre"));
        estado.setDescripcion(resultado.getString("descripcion"));
        estado.setFinalizado(resultado.getBoolean("es_final"));
        estado.setActivo(resultado.getBoolean("activo"));
        return estado;
    }
}
