package py.fpune.soracare.config;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Permite que Hibernate reutilice el conjunto de conexiones seguro de SoraCare.
 */
public class ConexionHibernateProvider implements ConnectionProvider {

    @Override
    public Connection getConnection() throws SQLException {
        return ConexionBD.obtenerConexion();
    }

    @Override
    public void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> tipo) {
        return tipo.isAssignableFrom(getClass())
                || tipo.isAssignableFrom(ConnectionProvider.class);
    }

    @Override
    public <T> T unwrap(Class<T> tipo) {
        if (isUnwrappableAs(tipo)) {
            return tipo.cast(this);
        }
        throw new UnknownUnwrapTypeException(tipo);
    }
}
