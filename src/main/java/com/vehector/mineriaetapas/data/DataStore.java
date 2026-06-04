package com.vehector.mineriaetapas.data;

import com.vehector.mineriaetapas.MineriaEtapasPlugin;
import com.vehector.mineriaetapas.data.PlayerData;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class DataStore {
    private final MineriaEtapasPlugin plugin;
    private final File dbFile;
    private Connection connection;

    public DataStore(MineriaEtapasPlugin plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "players.db");
    }

    public void init() throws SQLException {
        if (!this.plugin.getDataFolder().exists() && !this.plugin.getDataFolder().mkdirs()) {
            this.plugin.getLogger().warning("No se pudo crear la carpeta de datos.");
        }
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e) {
            throw new SQLException("Driver SQLite no disponible.", e);
        }
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.dbFile.getAbsolutePath());
        this.connection.setAutoCommit(true);
        try (Statement st = this.connection.createStatement();){
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute("PRAGMA synchronous=NORMAL;");
            st.execute("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, current_stage INTEGER NOT NULL DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS stage_counts (uuid TEXT NOT NULL, stage_id TEXT NOT NULL, amount INTEGER NOT NULL, PRIMARY KEY (uuid, stage_id))");
        }
    }

    public synchronized PlayerData load(UUID uuid) {
        try {
            PlayerData data = new PlayerData(uuid, 0);
            try (PreparedStatement ps = this.connection.prepareStatement("SELECT current_stage FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        data.setCurrentStageIndex(rs.getInt(1));
                    }
                }
            }
            try (PreparedStatement ps = this.connection.prepareStatement("SELECT stage_id, amount FROM stage_counts WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        data.setCount(rs.getString(1), rs.getInt(2));
                    }
                }
            }
            data.clearDirty();
            return data;
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error cargando datos de " + String.valueOf(uuid), e);
            return new PlayerData(uuid, 0);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public synchronized void save(PlayerData data) {
        try {
            this.connection.setAutoCommit(false);
            try (PreparedStatement ps = this.connection.prepareStatement("INSERT INTO players(uuid, current_stage) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET current_stage = excluded.current_stage");){
                ps.setString(1, data.getUuid().toString());
                ps.setInt(2, data.getCurrentStageIndex());
                ps.executeUpdate();
            }
            Map<String, Integer> snapshot = data.snapshotCounts();
            try (PreparedStatement del = this.connection.prepareStatement("DELETE FROM stage_counts WHERE uuid = ?");){
                del.setString(1, data.getUuid().toString());
                del.executeUpdate();
            }
            if (!snapshot.isEmpty()) {
                try (PreparedStatement ins = this.connection.prepareStatement("INSERT INTO stage_counts(uuid, stage_id, amount) VALUES(?,?,?)");){
                    for (Map.Entry<String, Integer> e : snapshot.entrySet()) {
                        ins.setString(1, data.getUuid().toString());
                        ins.setString(2, e.getKey());
                        ins.setInt(3, e.getValue());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
            this.connection.commit();
            data.clearDirty();
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error guardando datos de " + String.valueOf(data.getUuid()), e);
            try {
                this.connection.rollback();
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
        }
        finally {
            try {
                this.connection.setAutoCommit(true);
            }
            catch (SQLException sQLException) {}
        }
    }

    public synchronized void close() {
        if (this.connection != null) {
            try {
                this.connection.close();
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.WARNING, "Error cerrando SQLite", e);
            }
        }
    }
}

